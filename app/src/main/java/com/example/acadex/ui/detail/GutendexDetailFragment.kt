package com.example.acadex.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.acadex.R
import com.example.acadex.databinding.FragmentGutendexDetailBinding
import com.example.acadex.util.DownloadHelper
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class GutendexDetailFragment : Fragment() {

    private var _binding: FragmentGutendexDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GutendexDetailViewModel by viewModels()
    private val args: GutendexDetailFragmentArgs by navArgs()

    private var errorView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGutendexDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadBook(args.bookId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is GutendexDetailUiState.Loading -> {
                        binding.loadingProgress.visibility = View.VISIBLE
                        binding.detailContent.visibility = View.GONE
                        errorView?.visibility = View.GONE
                    }
                    is GutendexDetailUiState.Success -> {
                        binding.loadingProgress.visibility = View.GONE
                        binding.detailContent.visibility = View.VISIBLE
                        errorView?.visibility = View.GONE

                        val book = state.book
                        binding.bookTitle.text = book.title
                        binding.bookAuthors.text = book.authors.joinToString(", ") { it.name }
                        binding.metaLanguage.text = book.languages.firstOrNull()?.uppercase() ?: "EN"
                        binding.metaDownloads.text = if (book.downloadCount > 1000) {
                            "%.1fk".format(book.downloadCount / 1000f)
                        } else {
                            book.downloadCount.toString()
                        }

                        binding.subjectChips.removeAllViews()
                        book.subjects.take(4).forEach { subject ->
                            val chip = Chip(requireContext()).apply {
                                text = subject
                                isClickable = false
                                isCheckable = false
                            }
                            binding.subjectChips.addView(chip)
                        }

                        val txtUrl = book.getTxtUrl()
                        val htmlUrl = book.getHtmlUrl()
                        val pdfUrl = book.getPdfUrl()

                        // Prefer txt for native rendering, then html, then pdf
                        val (formatUrl, format) = when {
                            txtUrl != null && txtUrl.isNotBlank() -> txtUrl to "txt"
                            htmlUrl != null && htmlUrl.isNotBlank() -> htmlUrl to "html"
                            pdfUrl != null && pdfUrl.isNotBlank() -> pdfUrl to "pdf"
                            else -> null to null
                        }

                        if (formatUrl != null && format != null) {
                            binding.btnRead.isEnabled = true
                            binding.btnRead.text = "Read Book"
                            binding.btnRead.setOnClickListener {
                                // Double-check URL isn't blank before navigating (prevents Parcel NULL crash)
                                if (formatUrl.isBlank()) {
                                    Snackbar.make(binding.root, "No readable URL found for this book", Snackbar.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                findNavController().navigate(
                                    GutendexDetailFragmentDirections.actionGutendexDetailToBookReader(
                                        url = formatUrl,
                                        format = format
                                    )
                                )
                            }
                        } else {
                            binding.btnRead.isEnabled = false
                            binding.btnRead.text = "No preview available"
                            binding.btnRead.setOnClickListener {
                                Snackbar.make(binding.root, "This book has no readable format available", Snackbar.LENGTH_SHORT).show()
                            }
                        }

                        if (state.isSaved) {
                            binding.btnSave.setIconResource(R.drawable.ic_bookmark_filled)
                            binding.btnSave.text = "Saved"
                        } else {
                            binding.btnSave.setIconResource(R.drawable.ic_bookmark_outline)
                            binding.btnSave.text = "Save"
                        }
                        binding.btnSave.setOnClickListener {
                            viewModel.toggleSave()
                        }

                        binding.btnDownload.setOnClickListener {
                            val epubUrl = book.formats["application/epub+zip"]
                            val downloadUrl = pdfUrl ?: epubUrl ?: txtUrl ?: htmlUrl
                            
                            if (downloadUrl != null) {
                                val extension = when {
                                    downloadUrl == pdfUrl -> ".pdf"
                                    downloadUrl == epubUrl -> ".epub"
                                    downloadUrl == txtUrl -> ".txt"
                                    else -> ".html"
                                }
                                val safeTitle = book.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                                val fileName = "${safeTitle}$extension"
                                
                                val downloadId = DownloadHelper.enqueueDownload(requireContext(), downloadUrl, book.title, fileName)
                                if (downloadId != null) {
                                    Snackbar.make(binding.root, getString(R.string.downloading_file), Snackbar.LENGTH_SHORT).show()
                                } else {
                                    Snackbar.make(binding.root, "Failed to start download", Snackbar.LENGTH_SHORT).show()
                                }
                            } else {
                                Snackbar.make(binding.root, "No suitable format available for download", Snackbar.LENGTH_SHORT).show()
                            }
                        }

                        binding.btnShare.setOnClickListener {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, book.title)
                                putExtra(Intent.EXTRA_TEXT, "Read ${book.title} on Project Gutenberg: https://www.gutenberg.org/ebooks/${book.id}")
                            }
                            startActivity(Intent.createChooser(intent, "Share Book"))
                        }
                    }
                    is GutendexDetailUiState.Error -> {
                        binding.loadingProgress.visibility = View.GONE
                        binding.detailContent.visibility = View.GONE
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        if (errorView == null) {
            errorView = binding.errorStub.inflate()
            errorView?.findViewById<Button>(R.id.btnRetry)?.setOnClickListener {
                viewModel.loadBook(args.bookId)
            }
        }
        errorView?.visibility = View.VISIBLE
        errorView?.findViewById<TextView>(R.id.errorMessage)?.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        errorView = null
    }
}
