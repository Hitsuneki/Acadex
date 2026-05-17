package com.example.classhub.ui.upload

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.classhub.R
import com.example.classhub.data.MockDataSource
import com.example.classhub.data.models.Comment
import com.example.classhub.data.models.ResourceFile
import com.example.classhub.databinding.FragmentUploadBinding
import com.example.classhub.util.FileTypeUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private var selectedFileName: String? = null

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            selectedFileName = uri?.lastPathSegment ?: "selected_file"
            binding.selectedFileName.text = selectedFileName
            binding.fileError.isVisible = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subjects = resources.getStringArray(R.array.subjects)
        val fileTypes = resources.getStringArray(R.array.file_types)

        binding.subjectDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subjects)
        )
        binding.subjectDropdown.setText(subjects[0], false)

        binding.fileTypeDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fileTypes)
        )
        binding.fileTypeDropdown.setText(fileTypes[0], false)

        val defaultName = MockDataSource.profileName
        if (binding.uploaderEditText.text.isNullOrBlank()) {
            binding.uploaderEditText.setText(defaultName)
        }

        binding.uploadZone.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickFileLauncher.launch(Intent.createChooser(intent, getString(R.string.tap_to_select_file)))
        }

        binding.btnUpload.setOnClickListener { attemptUpload() }
    }

    private fun attemptUpload() {
        var valid = true
        val title = binding.titleEditText.text?.toString()?.trim().orEmpty()
        val uploader = binding.uploaderEditText.text?.toString()?.trim().orEmpty()

        if (title.isEmpty()) {
            binding.titleInputLayout.error = getString(R.string.field_required)
            valid = false
        } else {
            binding.titleInputLayout.error = null
        }

        if (uploader.isEmpty()) {
            binding.uploaderInputLayout.error = getString(R.string.field_required)
            valid = false
        } else {
            binding.uploaderInputLayout.error = null
        }

        if (selectedFileName == null) {
            binding.fileError.isVisible = true
            valid = false
        }

        if (!valid) return

        binding.btnUpload.isEnabled = false
        binding.uploadProgress.isVisible = true

        val description = binding.descriptionEditText.text?.toString()?.trim().orEmpty()
        val subject = binding.subjectDropdown.text.toString()
        val fileType = FileTypeUtils.normalizeType(binding.fileTypeDropdown.text.toString())
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)

            val newFile = ResourceFile(
                id = MockDataSource.createFileId(),
                title = title,
                description = description,
                subject = subject,
                fileType = fileType,
                uploaderName = uploader,
                uploadDate = dateFormat.format(Date()),
                rating = 0f,
                ratingCount = 0,
                downloadCount = 0,
                comments = mutableListOf<Comment>(),
                localFileName = selectedFileName
            )
            MockDataSource.addFile(newFile)
            MockDataSource.profileName = uploader

            binding.uploadProgress.isVisible = false
            binding.btnUpload.isEnabled = true
            clearForm()
            Snackbar.make(binding.root, R.string.upload_success, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        binding.titleEditText.text?.clear()
        binding.descriptionEditText.text?.clear()
        selectedFileName = null
        binding.selectedFileName.setText(R.string.no_file_selected)
        binding.fileError.isVisible = false
        binding.titleInputLayout.error = null
        binding.uploaderInputLayout.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
