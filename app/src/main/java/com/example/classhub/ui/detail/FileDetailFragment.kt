package com.example.classhub.ui.detail

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classhub.R
import com.example.classhub.adapters.CommentAdapter
import com.example.classhub.data.MockDataSource
import com.example.classhub.data.models.Comment
import com.example.classhub.databinding.BottomSheetPreviewBinding
import com.example.classhub.databinding.FragmentFileDetailBinding
import com.example.classhub.util.FileTypeUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        bindFile()
    }

    private fun bindFile() {
        val file = MockDataSource.getFileById(args.fileId) ?: run {
            findNavController().navigateUp()
            return
        }

        val context = requireContext()
        binding.toolbar.title = file.title
        binding.detailTitle.text = file.title
        binding.subjectBadge.text = file.subject
        binding.uploaderDate.text = "${file.uploaderName} · ${file.uploadDate}"
        binding.description.text = file.description.ifBlank { "No description provided." }
        binding.description.isVisible = file.description.isNotBlank()

        val typeLabel = FileTypeUtils.displayLabel(file.fileType)
        binding.fileTypeBadge.text = typeLabel
        binding.fileTypeBadge.setTextColor(
            ContextCompat.getColor(context, FileTypeUtils.textColorRes(file.fileType))
        )
        binding.fileTypeBadge.setBackgroundColor(
            ContextCompat.getColor(context, FileTypeUtils.bgColorRes(file.fileType))
        )

        val bgColor = ContextCompat.getColor(context, FileTypeUtils.bgColorRes(file.fileType))
        binding.largeIconContainer.backgroundTintList = ColorStateList.valueOf(bgColor)
        binding.largeFileIcon.setImageResource(FileTypeUtils.iconRes(file.fileType))

        val ratingDisplay = if (file.ratingCount > 0) file.rating else 0f
        binding.statsRow.text = "★ %.1f · %d downloads · %d comments".format(
            ratingDisplay, file.downloadCount, file.comments.size
        )
        binding.ratingSummary.text = getString(
            R.string.ratings_count_format,
            ratingDisplay,
            file.ratingCount
        )

        commentAdapter.submitList(file.comments.toList())
        updateSaveButton(file.isSaved)

        binding.btnDownload.setOnClickListener {
            Snackbar.make(binding.root, R.string.download_started, Snackbar.LENGTH_SHORT).show()
        }

        binding.btnPreview.setOnClickListener { showPreviewSheet() }

        binding.btnSave.setOnClickListener {
            file.isSaved = !file.isSaved
            updateSaveButton(file.isSaved)
        }

        binding.btnShare.setOnClickListener {
            val shareText = "${file.title}\n${file.description}\nShared via ClassHub"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        }

        binding.btnSubmitRating.setOnClickListener {
            val userRating = binding.ratingBar.rating
            if (userRating > 0f) {
                val newCount = file.ratingCount + 1
                file.rating = ((file.rating * file.ratingCount) + userRating) / newCount
                file.ratingCount = newCount
                binding.ratingSummary.text = getString(
                    R.string.ratings_count_format,
                    file.rating,
                    file.ratingCount
                )
                binding.statsRow.text = "★ %.1f · %d downloads · %d comments".format(
                    file.rating, file.downloadCount, file.comments.size
                )
                Snackbar.make(binding.root, R.string.rating_submitted, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnPostComment.setOnClickListener {
            val text = binding.commentInput.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val comment = Comment(
                commenterName = MockDataSource.profileName,
                text = text,
                date = dateFormat.format(Date())
            )
            file.comments.add(0, comment)
            commentAdapter.submitList(file.comments.toList())
            binding.commentInput.text?.clear()
            binding.statsRow.text = "★ %.1f · %d downloads · %d comments".format(
                file.rating, file.downloadCount, file.comments.size
            )
        }
    }

    private fun updateSaveButton(saved: Boolean) {
        if (saved) {
            binding.btnSave.text = getString(R.string.saved)
            binding.btnSave.setIconResource(android.R.drawable.btn_star_big_on)
        } else {
            binding.btnSave.text = getString(R.string.save_to_collection)
            binding.btnSave.setIconResource(android.R.drawable.btn_star_big_off)
        }
    }

    private fun showPreviewSheet() {
        val sheetBinding = BottomSheetPreviewBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
