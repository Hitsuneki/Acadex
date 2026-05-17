package com.example.acadex.ui.detail

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadex.R
import com.example.acadex.adapters.CommentAdapter
import com.example.acadex.data.ResourceRepository
import com.example.acadex.databinding.FragmentFileDetailBinding
import com.example.acadex.util.FileTypeUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class FileDetailFragment : Fragment() {

    private var _binding: FragmentFileDetailBinding? = null
    private val binding get() = _binding!!

    private val args: FileDetailFragmentArgs by navArgs()
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        commentAdapter = CommentAdapter()
        binding.commentsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecycler.adapter = commentAdapter

        bindFile()
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            ResourceRepository.refreshFromSupabase()
            bindFile()
        }
    }

    private fun bindFile() {
        val materialId = args.materialId
        val cached = ResourceRepository.getFileById(materialId)
        if (cached != null) {
            bindFileContent(cached)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = ResourceRepository.loadRemoteDetail(materialId)
            if (loaded != null) {
                bindFileContent(loaded)
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun bindFileContent(file: com.example.acadex.data.model.ResourceFile) {

        val context = requireContext()
        binding.toolbar.title = file.title
        binding.detailTitle.text = file.title
        binding.subjectBadge.text = file.subject
        binding.uploaderDate.text = "${file.uploaderName} · ${file.uploadDate}"
        binding.description.text = file.description.ifBlank { getString(R.string.no_description) }
        binding.description.isVisible = file.description.isNotBlank()

        val bgColor = ContextCompat.getColor(context, FileTypeUtils.bgRes(file.fileType))
        val fgColor = ContextCompat.getColor(context, FileTypeUtils.fgRes(file.fileType))
        binding.fileTypeBadge.text = file.fileType.displayName()
        binding.fileTypeBadge.setTextColor(fgColor)
        binding.fileTypeBadge.setBackgroundColor(bgColor)
        binding.largeIconContainer.backgroundTintList = ColorStateList.valueOf(bgColor)
        binding.largeFileIcon.setImageResource(FileTypeUtils.iconRes(file.fileType))
        binding.largeFileIcon.imageTintList = ColorStateList.valueOf(fgColor)

        val ratingDisplay = if (file.ratingCount > 0) file.rating else 0f
        binding.statsRow.text = getString(
            R.string.file_stats_format,
            ratingDisplay,
            file.downloadCount,
            file.comments.size
        )
        binding.ratingSummary.text = getString(
            R.string.ratings_count_format,
            ratingDisplay,
            file.ratingCount
        )

        commentAdapter.submitList(file.comments.toList())
        updateSaveButton(file.isSaved)

        binding.btnDownload.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val urlResult = ResourceRepository.recordDownload(file)
                val url = urlResult.getOrNull()
                if (!url.isNullOrBlank()) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    Snackbar.make(binding.root, R.string.download_started, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, R.string.download_started, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnPreview.setOnClickListener { showPreview(file.downloadUrl) }
        binding.aiStrip.setOnClickListener {
            Snackbar.make(binding.root, R.string.ai_indexing_soon, Snackbar.LENGTH_SHORT).show()
        }

        binding.btnSave.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = ResourceRepository.toggleSaved(file)
                if (result.isSuccess) {
                    updateSaveButton(file.isSaved)
                } else {
                    Snackbar.make(binding.root, R.string.save_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnShare.setOnClickListener {
            val shareText = buildString {
                append(file.title)
                if (file.description.isNotBlank()) append("\n").append(file.description)
                file.downloadUrl?.let { append("\n").append(it) }
                append("\n").append(getString(R.string.shared_via_acadex))
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        }

        binding.btnSubmitRating.setOnClickListener {
            val userRating = binding.ratingBar.rating
            if (userRating <= 0f) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val result = ResourceRepository.submitRating(file, userRating)
                if (result.isSuccess) {
                    binding.ratingSummary.text = getString(
                        R.string.ratings_count_format,
                        file.rating,
                        file.ratingCount
                    )
                    binding.statsRow.text = getString(
                        R.string.file_stats_format,
                        file.rating,
                        file.downloadCount,
                        file.comments.size
                    )
                    Snackbar.make(binding.root, R.string.rating_submitted, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, R.string.rating_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnPostComment.setOnClickListener {
            val text = binding.commentInput.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val result = ResourceRepository.postComment(file, text)
                if (result.isSuccess) {
                    commentAdapter.submitList(file.comments.toList())
                    binding.commentInput.text?.clear()
                    binding.statsRow.text = getString(
                        R.string.file_stats_format,
                        file.rating,
                        file.downloadCount,
                        file.comments.size
                    )
                } else {
                    Snackbar.make(binding.root, R.string.comment_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateSaveButton(saved: Boolean) {
        if (saved) {
            binding.btnSave.text = getString(R.string.saved_to_index)
            binding.btnSave.setIconResource(android.R.drawable.btn_star_big_on)
        } else {
            binding.btnSave.text = getString(R.string.save_to_index)
            binding.btnSave.setIconResource(android.R.drawable.btn_star_big_off)
        }
    }

    private fun showPreview(url: String?) {
        if (!url.isNullOrBlank()) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } else {
            Snackbar.make(binding.root, R.string.preview_coming_soon, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
