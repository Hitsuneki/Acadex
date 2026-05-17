package com.example.acadex.ui.detail

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.acadex.R
import com.example.acadex.adapters.CommentAdapter
import com.example.acadex.adapters.PdfPageAdapter
import com.example.acadex.data.model.FileType
import com.example.acadex.databinding.FragmentFileDetailBinding
import com.example.acadex.databinding.IncludeErrorStateBinding
import com.example.acadex.ui.common.UiState
import com.example.acadex.ui.saved.SavedIndexSharedViewModel
import com.example.acadex.util.FileTypeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FileDetailFragment : Fragment() {

    private var _binding: FragmentFileDetailBinding? = null
    private val binding get() = _binding!!

    private val args: FileDetailFragmentArgs by navArgs()
    private val savedShared: SavedIndexSharedViewModel by navGraphViewModels(R.id.nav_graph)
    private val viewModel: FileDetailViewModel by viewModels {
        FileDetailViewModelFactory(requireActivity().application, savedShared)
    }

    private lateinit var commentAdapter: CommentAdapter
    private var pdfAdapter: PdfPageAdapter? = null
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
            viewModel.onDownloadComplete(id)
        }
    }

    private val starViews by lazy {
        listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFileDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val errorBinding = IncludeErrorStateBinding.bind(binding.fullScreenError.root)
        errorBinding.btnRetry.setOnClickListener { viewModel.retryLoad() }
        binding.btnViewerRetry.setOnClickListener { viewModel.retryViewer() }
        binding.btnCommentsRetry.setOnClickListener { viewModel.retryComments() }

        commentAdapter = CommentAdapter(
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid,
            onDelete = { comment ->
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.confirm_delete_comment)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteComment(comment) }
                    .show()
            }
        )
        binding.commentsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecycler.adapter = commentAdapter

        setupStars()
        setupWebView()
        setupActions()
        observeViewModel()

        viewModel.load(args.materialId)
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            requireContext(),
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        requireContext().unregisterReceiver(downloadReceiver)
        super.onPause()
    }

    private fun setupStars() {
        starViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                viewModel.selectStars(index + 1)
            }
        }
    }

    private fun setupWebView() {
        binding.viewerWebView.settings.javaScriptEnabled = true
        binding.viewerWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.webViewProgress.isVisible = false
            }
        }
        binding.viewerWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.webViewProgress.isVisible = newProgress in 0..99
                binding.webViewProgress.progress = newProgress
            }
        }
    }

    private fun setupActions() {
        binding.aiStrip.setOnClickListener {
            // TODO integrate AI Summarizer API
            Snackbar.make(binding.root, R.string.ai_indexing_soon, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnDownload.setOnClickListener { viewModel.startDownload() }
        binding.btnPreview.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, binding.viewerSection.top)
        }
        binding.btnSave.setOnClickListener { viewModel.toggleSave() }
        binding.btnShare.setOnClickListener {
            val text = viewModel.shareMessage() ?: return@setOnClickListener
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    getString(R.string.share)
                )
            )
        }
        binding.btnSubmitRating.setOnClickListener { viewModel.submitRating() }
        binding.btnOpenGoogleDocs.setOnClickListener { viewModel.openGoogleDocsViewer() }

        binding.commentInput.doAfterTextChanged {
            binding.btnPostComment.isEnabled = !it?.toString().orEmpty().trim().isNullOrEmpty()
        }
        binding.btnPostComment.setOnClickListener {
            val text = binding.commentInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) viewModel.postComment(text)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.screenState.collect { state ->
                        binding.fullScreenLoading.isVisible = state is UiState.Loading
                        binding.scrollView.isVisible = state is UiState.Success
                        binding.fullScreenError.root.isVisible = state is UiState.Error
                        if (state is UiState.Error) {
                            IncludeErrorStateBinding.bind(binding.fullScreenError.root)
                                .errorMessage.text = state.message
                        }
                    }
                }
                launch {
                    viewModel.material.collect { material ->
                        material ?: return@collect
                        bindHeader(material)
                    }
                }
                launch {
                    viewModel.ratingAvg.collect { avg ->
                        binding.statRating.text = "★ %.1f".format(avg)
                    }
                }
                launch {
                    viewModel.downloadCount.collect { count ->
                        binding.statDownloads.text = count.toString()
                    }
                }
                launch {
                    viewModel.commentCount.collect { count ->
                        binding.statComments.text = count.toString()
                    }
                }
                launch {
                    viewModel.isSaved.collect { saved -> updateSaveButton(saved) }
                }
                launch {
                    viewModel.comments.collect { commentAdapter.submitList(it) }
                }
                launch {
                    viewModel.commentsState.collect { state ->
                        binding.commentsLoading.isVisible = state is UiState.Loading
                        binding.commentsError.isVisible = state is UiState.Error
                        if (state is UiState.Error) {
                            binding.commentsErrorText.text = state.message
                        }
                    }
                }
                launch {
                    viewModel.selectedStars.collect { count -> renderStars(count) }
                }
                launch {
                    viewModel.userRating.collect {
                        binding.btnSubmitRating.text = getString(
                            if (viewModel.isUpdateRating()) R.string.update_rating else R.string.submit_rating
                        )
                    }
                }
                launch {
                    viewModel.ratingError.collect { err ->
                        binding.ratingError.isVisible = !err.isNullOrBlank()
                        binding.ratingError.text = err
                    }
                }
                launch {
                    viewModel.viewerState.collect { renderViewer(it) }
                }
                launch {
                    viewModel.postingComment.collect { posting ->
                        binding.commentInput.isEnabled = !posting
                        if (!posting) {
                            val hasText = !binding.commentInput.text?.toString().orEmpty().trim().isNullOrEmpty()
                            binding.btnPostComment.isEnabled = hasText
                        } else {
                            binding.btnPostComment.isEnabled = false
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is FileDetailEvent.Snackbar -> {
                                Snackbar.make(binding.root, event.messageRes, Snackbar.LENGTH_SHORT).show()
                                viewModel.consumeEvent()
                            }
                            is FileDetailEvent.CommentPosted -> {
                                binding.commentInput.text?.clear()
                                binding.btnPostComment.isEnabled = false
                                binding.commentsRecycler.smoothScrollToPosition(
                                    (commentAdapter.itemCount - 1).coerceAtLeast(0)
                                )
                                viewModel.consumeEvent()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun bindHeader(material: com.example.acadex.data.model.ResourceFile) {
        binding.toolbar.title = material.title
        binding.detailTitle.text = material.title
        binding.uploaderDate.text = "${material.uploaderName} · ${material.uploadDate}"
        binding.subjectBadge.text = material.subject
        val ctx = requireContext()
        val bg = ContextCompat.getColor(ctx, FileTypeUtils.bgRes(material.fileType))
        val fg = ContextCompat.getColor(ctx, FileTypeUtils.fgRes(material.fileType))
        binding.fileTypeBadge.text = material.fileType.displayName()
        binding.fileTypeBadge.setTextColor(fg)
        binding.fileTypeBadge.setBackgroundColor(bg)
        binding.largeIconContainer.backgroundTintList = ColorStateList.valueOf(bg)
        binding.largeFileIcon.setImageResource(FileTypeUtils.iconRes(material.fileType))
        binding.largeFileIcon.imageTintList = ColorStateList.valueOf(fg)
    }

    private fun renderStars(count: Int) {
        starViews.forEachIndexed { index, view ->
            val filled = index < count
            view.setImageResource(if (filled) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        }
    }

    private fun updateSaveButton(saved: Boolean) {
        if (saved) {
            binding.btnSave.text = getString(R.string.saved_to_index)
            binding.btnSave.setIconResource(R.drawable.ic_bookmark_filled)
            binding.btnSave.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.star_amber)
            )
        } else {
            binding.btnSave.text = getString(R.string.save_to_index)
            binding.btnSave.setIconResource(R.drawable.ic_bookmark_outline)
            binding.btnSave.iconTint = null
        }
    }

    private fun renderViewer(state: ViewerUiState) {
        binding.viewerLoading.isVisible = state is ViewerUiState.Loading
        binding.viewerError.isVisible = state is ViewerUiState.Error
        binding.viewerPdf.isVisible = state is ViewerUiState.Pdf
        binding.viewerImage.isVisible = state is ViewerUiState.Image || state is ViewerUiState.ImageFile
        binding.viewerTextCard.isVisible = state is ViewerUiState.Text
        binding.viewerOffice.isVisible = state is ViewerUiState.OfficePlaceholder
        binding.viewerWebViewContainer.isVisible = state is ViewerUiState.GoogleDocs

        when (state) {
            is ViewerUiState.Pdf -> {
                pdfAdapter?.close()
                pdfAdapter = PdfPageAdapter(state.cacheFile).also { adapter ->
                    binding.pdfPagesRecycler.layoutManager = LinearLayoutManager(requireContext())
                    binding.pdfPagesRecycler.adapter = adapter
                    binding.pdfPagesRecycler.clearOnScrollListeners()
                    binding.pdfPagesRecycler.addOnScrollListener(object :
                        androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                            val first = lm.findFirstVisibleItemPosition().coerceAtLeast(0) + 1
                            binding.pdfPageCounter.text = getString(
                                R.string.pdf_page_counter,
                                first.coerceAtMost(state.pageCount),
                                state.pageCount
                            )
                        }
                    })
                }
                binding.pdfPageCounter.text = getString(R.string.pdf_page_counter, 1, state.pageCount)
            }
            is ViewerUiState.Image -> {
                binding.viewerImage.setImageDrawable(null)
                Glide.with(this)
                    .load(state.url)
                    .fitCenter()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_icon_placeholder)
                    .error(R.drawable.bg_icon_placeholder)
                    .into(binding.viewerImage)
            }
            is ViewerUiState.ImageFile -> {
                binding.viewerImage.setImageDrawable(null)
                Glide.with(this)
                    .load(state.cacheFile)
                    .fitCenter()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_icon_placeholder)
                    .error(R.drawable.bg_icon_placeholder)
                    .into(binding.viewerImage)
            }
            is ViewerUiState.Text -> {
                binding.viewerText.text = state.content
                binding.txtTruncatedNotice.isVisible = state.truncated
            }
            is ViewerUiState.OfficePlaceholder -> {
                binding.officeFileName.text = state.fileName
                val meta = buildString {
                    append(state.fileType.displayName())
                    if (state.fileSizeLabel.isNotBlank()) {
                        append(" · ")
                        append(state.fileSizeLabel)
                    }
                }
                binding.officeFileMeta.text = meta
                binding.officeIcon.setImageResource(FileTypeUtils.iconRes(state.fileType))
            }
            is ViewerUiState.GoogleDocs -> {
                binding.webViewProgress.isVisible = true
                binding.viewerWebView.loadUrl(state.viewerUrl)
            }
            else -> Unit
        }
    }

    override fun onDestroyView() {
        pdfAdapter?.close()
        pdfAdapter = null
        binding.viewerWebView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
