package com.example.acadex.ui.reader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.adapters.PdfPageAdapter
import com.example.acadex.databinding.FragmentBookReaderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class BookReaderFragment : Fragment() {

    private var _binding: FragmentBookReaderBinding? = null
    private val binding get() = _binding!!

    private val args: BookReaderFragmentArgs by navArgs()

    private var pdfAdapter: PdfPageAdapter? = null
    private var downloadedFile: File? = null
    private var currentFontSize = 17 // sp
    private val minFont = 12
    private val maxFont = 28

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.readerToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnRetry.setOnClickListener { startReader() }

        setupFontControls()
        startReader()
    }

    private fun startReader() {
        showLoading()
        when (args.format) {
            "html" -> setupHtmlReader(args.url)
            "pdf" -> setupPdfReader(args.url)
            "txt" -> setupTxtReader(args.url)
            else -> showError("Unsupported book format: ${args.format}")
        }
    }

    private fun setupFontControls() {
        binding.fontSizeLabel.text = currentFontSize.toString()
        binding.btnFontDecrease.setOnClickListener {
            if (currentFontSize > minFont) {
                currentFontSize -= 1
                applyFontSize()
            }
        }
        binding.btnFontIncrease.setOnClickListener {
            if (currentFontSize < maxFont) {
                currentFontSize += 1
                applyFontSize()
            }
        }
    }

    private fun applyFontSize() {
        binding.fontSizeLabel.text = currentFontSize.toString()
        binding.textView.textSize = currentFontSize.toFloat()
        binding.btnFontDecrease.alpha = if (currentFontSize <= minFont) 0.3f else 1f
        binding.btnFontIncrease.alpha = if (currentFontSize >= maxFont) 0.3f else 1f
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupHtmlReader(url: String) {
        binding.readerToolbar.title = "Reading"
        binding.readerToolbar.subtitle = "HTML"

        binding.webView.visibility = View.VISIBLE
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.builtInZoomControls = true
        binding.webView.settings.displayZoomControls = false
        binding.webView.settings.defaultFontSize = currentFontSize

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (isAdded) {
                    showContent()
                    // Inject reader-friendly CSS
                    view?.evaluateJavascript("""
                        (function() {
                            var style = document.createElement('style');
                            style.innerHTML = `
                                body {
                                    font-family: Georgia, serif !important;
                                    line-height: 1.8 !important;
                                    padding: 16px 20px !important;
                                    max-width: 100% !important;
                                    color: #0F172A !important;
                                    background: #FFFFFF !important;
                                    word-wrap: break-word !important;
                                    overflow-wrap: break-word !important;
                                }
                                img { max-width: 100% !important; height: auto !important; }
                                pre, code { white-space: pre-wrap !important; word-wrap: break-word !important; }
                            `;
                            document.head.appendChild(style);
                        })();
                    """.trimIndent(), null)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                if (isAdded) {
                    showError("Failed to load page: $description")
                }
            }
        }
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.loadUrl(url)
    }

    private fun setupPdfReader(url: String) {
        binding.readerToolbar.title = "Reading"
        binding.readerToolbar.subtitle = "PDF"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.loadingText.text = "Downloading PDF…"
                val file = downloadFile(url, ".pdf")
                downloadedFile = file

                showContent()
                binding.pdfRecyclerView.visibility = View.VISIBLE
                binding.pdfPageIndicator.visibility = View.VISIBLE
                binding.readingProgress.visibility = View.VISIBLE

                pdfAdapter = PdfPageAdapter(file).also { adapter ->
                    val layoutManager = LinearLayoutManager(requireContext())
                    binding.pdfRecyclerView.layoutManager = layoutManager
                    binding.pdfRecyclerView.adapter = adapter

                    binding.readingProgress.max = adapter.itemCount.coerceAtLeast(1)
                    binding.readingProgress.progress = 1

                    binding.pdfRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                            val first = lm.findFirstVisibleItemPosition().coerceAtLeast(0) + 1
                            val total = adapter.itemCount
                            binding.pdfPageCounter.text = "Page $first / $total"
                            binding.readingProgress.progress = first
                        }
                    })
                    binding.pdfPageCounter.text = "Page 1 / ${adapter.itemCount}"
                }
            } catch (e: Exception) {
                if (isAdded) {
                    showError("Failed to download PDF: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun setupTxtReader(url: String) {
        binding.readerToolbar.title = "Reading"
        binding.readerToolbar.subtitle = "Plain Text"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.loadingText.text = "Downloading text…"
                val file = downloadFile(url, ".txt")
                downloadedFile = file

                val textContent = withContext(Dispatchers.IO) {
                    file.readText(Charsets.UTF_8)
                }

                showContent()
                binding.textScrollView.visibility = View.VISIBLE
                binding.textPositionIndicator.visibility = View.VISIBLE
                binding.fontControls.visibility = View.VISIBLE
                binding.readingProgress.visibility = View.VISIBLE
                binding.readingProgress.max = 100
                binding.textView.text = textContent
                binding.textView.textSize = currentFontSize.toFloat()
                applyFontSize()

                // Track scroll position for progress
                binding.textScrollView.viewTreeObserver.addOnScrollChangedListener {
                    if (!isAdded || _binding == null) return@addOnScrollChangedListener
                    val scrollView = binding.textScrollView
                    val child = scrollView.getChildAt(0) ?: return@addOnScrollChangedListener
                    val scrollRange = child.height - scrollView.height
                    if (scrollRange > 0) {
                        val percent = ((scrollView.scrollY.toFloat() / scrollRange) * 100).toInt().coerceIn(0, 100)
                        binding.textPositionLabel.text = "$percent% read"
                        binding.readingProgress.progress = percent
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    showError("Failed to download text: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showLoading() {
        binding.readerLoading.visibility = View.VISIBLE
        binding.readerError.visibility = View.GONE
        binding.webView.visibility = View.GONE
        binding.pdfRecyclerView.visibility = View.GONE
        binding.textScrollView.visibility = View.GONE
        binding.pdfPageIndicator.visibility = View.GONE
        binding.textPositionIndicator.visibility = View.GONE
        binding.fontControls.visibility = View.GONE
        binding.readingProgress.visibility = View.GONE
    }

    private fun showContent() {
        binding.readerLoading.visibility = View.GONE
        binding.readerError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.readerLoading.visibility = View.GONE
        binding.readerError.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.webView.visibility = View.GONE
        binding.pdfRecyclerView.visibility = View.GONE
        binding.textScrollView.visibility = View.GONE
        binding.pdfPageIndicator.visibility = View.GONE
        binding.textPositionIndicator.visibility = View.GONE
        binding.fontControls.visibility = View.GONE
        binding.readingProgress.visibility = View.GONE
    }

    private suspend fun downloadFile(urlString: String, suffix: String): File = withContext(Dispatchers.IO) {
        // Follow redirects manually to handle Gutenberg's URL scheme
        var currentUrl = urlString
        var redirectCount = 0
        val maxRedirects = 5

        while (redirectCount < maxRedirects) {
            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            connection.setRequestProperty("User-Agent", "Acadex-Android")
            connection.setRequestProperty("Accept", "*/*")
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IOException("Redirect with no Location header")
                currentUrl = if (location.startsWith("http")) location else {
                    val base = URL(currentUrl)
                    URL(base, location).toString()
                }
                connection.disconnect()
                redirectCount++
                continue
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IOException("Server returned code $responseCode")
            }

            val tempFile = File.createTempFile("gutenberg_book", suffix, requireContext().cacheDir)
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
            connection.disconnect()

            if (tempFile.length() == 0L) {
                tempFile.delete()
                throw IOException("Downloaded file is empty")
            }
            return@withContext tempFile
        }
        throw IOException("Too many redirects")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfAdapter?.close()
        pdfAdapter = null
        try {
            downloadedFile?.delete()
        } catch (_: Exception) { }
        _binding = null
    }
}
