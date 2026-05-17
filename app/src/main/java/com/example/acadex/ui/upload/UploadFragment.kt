package com.example.acadex.ui.upload

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.acadex.R
import com.example.acadex.databinding.FragmentUploadBinding
import com.example.acadex.databinding.LayoutFileIndicatorBinding
import com.example.acadex.util.FileTypeUtils
import com.example.acadex.util.MimeTypeUtils
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import android.provider.OpenableColumns

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private var indicatorBinding: LayoutFileIndicatorBinding? = null
    private val viewModel: UploadViewModel by viewModels()

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val name = queryDisplayName(uri) ?: "file"
        viewModel.onFileSelected(requireContext(), uri, name)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        indicatorBinding = LayoutFileIndicatorBinding.bind(binding.fileIndicator.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val subjects = resources.getStringArray(R.array.subject_suggestions)
        binding.subjectInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subjects)
        )

        binding.uploadZone.setOnClickListener {
            pickFile.launch("*/*")
        }
        indicatorBinding?.btnClearFile?.setOnClickListener { viewModel.clearFile() }
        binding.btnUpload.setOnClickListener { viewModel.submit(requireContext()) }

        binding.titleEditText.doAfterTextChanged { viewModel.setTitle(it?.toString().orEmpty()) }
        binding.descriptionEditText.doAfterTextChanged { viewModel.setDescription(it?.toString().orEmpty()) }
        binding.subjectInput.doAfterTextChanged { viewModel.setSubject(it?.toString().orEmpty()) }
        binding.tagsEditText.doAfterTextChanged { syncTagsFromInput() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.formState.collect { renderForm(it) }
                }
                launch {
                    viewModel.uploadState.collect { renderUploadState(it) }
                }
            }
        }
    }

    private fun syncTagsFromInput() {
        val raw = binding.tagsEditText.text?.toString().orEmpty()
        val tags = raw.split(',', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        viewModel.setTags(tags)
        binding.tagsChipGroup.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    val remaining = viewModel.formState.value.tags.filter { it != tag }
                    binding.tagsEditText.setText(remaining.joinToString(", "))
                    viewModel.setTags(remaining)
                }
            }
            binding.tagsChipGroup.addView(chip)
        }
    }

    private fun renderForm(state: UploadFormState) {
        val hasFile = state.fileUri != null && state.fileType != null
        binding.uploadZone.isVisible = !hasFile
        binding.fileIndicator.root.isVisible = hasFile
        binding.fileError.isVisible = state.fileError

        if (hasFile) {
            val ctx = requireContext()
            val type = state.fileType!!
            val ind = indicatorBinding ?: return
            ind.selectedFileName.text = state.fileName
            ind.selectedFileTypePill.text = MimeTypeUtils.extensionLabel(type)
            val bg = ContextCompat.getColor(ctx, FileTypeUtils.bgRes(type))
            val fg = ContextCompat.getColor(ctx, FileTypeUtils.fgRes(type))
            ind.selectedFileIconContainer.backgroundTintList = ColorStateList.valueOf(bg)
            ind.selectedFileIcon.setImageResource(FileTypeUtils.iconRes(type))
            ind.selectedFileIcon.imageTintList = ColorStateList.valueOf(fg)
            ind.selectedFileTypePill.setTextColor(fg)
            ind.selectedFileTypePill.setBackgroundColor(bg)
        }

        binding.titleInputLayout.error = if (state.titleError) getString(R.string.title_required) else null
        binding.subjectInputLayout.error = if (state.subjectError) getString(R.string.subject_required) else null
    }

    private fun renderUploadState(state: UploadUiState) {
        val uploading = state is UploadUiState.Uploading
        binding.btnUpload.isEnabled = !uploading
        binding.uploadProgress.isVisible = uploading

        when (state) {
            is UploadUiState.Success -> {
                Snackbar.make(binding.root, R.string.upload_submitted, Snackbar.LENGTH_SHORT).show()
                viewModel.onUploadMessageShown()
            }
            is UploadUiState.Snackbar -> {
                val msg = when (state.message) {
                    UploadMessage.UNSUPPORTED_TYPE -> R.string.file_type_not_supported
                    UploadMessage.UPLOAD_NETWORK -> R.string.upload_failed_network
                    UploadMessage.UPLOAD_FAILED -> R.string.upload_failed
                    UploadMessage.METADATA_FAILED -> R.string.upload_metadata_failed
                }
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                viewModel.onUploadMessageShown()
            }
            else -> Unit
        }
    }

    private fun queryDisplayName(uri: android.net.Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return uri.lastPathSegment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        indicatorBinding = null
        _binding = null
    }
}
