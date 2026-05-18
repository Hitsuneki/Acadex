package com.example.acadex.ui.reader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

        when (args.format) {
            "html" -> setupHtmlReader(args.url)
            "pdf" -> setupPdfReader(args.url)
            "txt" -> setupTxtReader(args.url)
            else -> {
                binding.readerLoading.visibility = View.GONE
                Toast.makeText(requireContext(), "Unsupported book format", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupHtmlReader(url: String) {
        binding.webView.visibility = View.VISIBLE
        binding.readerLoading.visibility = View.VISIBLE
        
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.loadWithOverviewMode = true

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (isAdded) {
                    binding.readerLoading.visibility = View.GONE
                }
            }
        }
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.loadUrl(url)
    }

    private fun setupPdfReader(url: String) {
        binding.pdfRecyclerView.visibility = View.VISIBLE
        binding.pdfPageIndicator.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = downloadFile(url, ".pdf")
                downloadedFile = file
                
                binding.readerLoading.visibility = View.GONE

                pdfAdapter = PdfPageAdapter(file).also { adapter ->
                    binding.pdfRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.pdfRecyclerView.adapter = adapter
                    binding.pdfRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                            val first = lm.findFirstVisibleItemPosition().coerceAtLeast(0) + 1
                            val total = adapter.itemCount
                            binding.pdfPageCounter.text = "Page $first / $total"
                        }
                    })
                    binding.pdfPageCounter.text = "Page 1 / ${adapter.itemCount}"
                }
            } catch (e: Exception) {
                if (isAdded) {
                    binding.readerLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to download PDF book", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupTxtReader(url: String) {
        binding.textScrollView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = downloadFile(url, ".txt")
                downloadedFile = file
                
                val textContent = withContext(Dispatchers.IO) {
                    file.readText(Charsets.UTF_8)
                }

                binding.readerLoading.visibility = View.GONE
                binding.textView.text = textContent
            } catch (e: Exception) {
                if (isAdded) {
                    binding.readerLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to download book text", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun downloadFile(urlString: String, suffix: String): File = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val tempFile = File.createTempFile("gutenberg_book", suffix, requireContext().cacheDir)
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } else {
            throw IOException("Server returned code ${connection.responseCode}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfAdapter?.close()
        pdfAdapter = null
        try {
            downloadedFile?.delete()
        } catch (e: Exception) {
            // Ignore
        }
        _binding = null
    }
}
