package com.example.acadex.ui.upload

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
import com.example.acadex.R
import com.example.acadex.data.MockDataSource
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.databinding.FragmentUploadBinding
import com.example.acadex.util.FileTypeUtils
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

    private val pickFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            selectedFileName = r.data?.data?.lastPathSegment ?: "file"
            binding.selectedFileName.text = selectedFileName
            binding.fileError.isVisible = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.submit_to_archive)
        val subjects = resources.getStringArray(R.array.subjects_spinner)
        val types = resources.getStringArray(R.array.file_types)
        binding.subjectDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subjects))
        binding.subjectDropdown.setText(subjects[0], false)
        binding.fileTypeDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types))
        binding.fileTypeDropdown.setText(types[0], false)
        binding.uploaderEditText.setText(MockDataSource.profileName)
        binding.uploadZone.setOnClickListener {
            pickFile.launch(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE)
            }, getString(R.string.tap_to_select_file)))
        }
        binding.btnUpload.setOnClickListener { submit() }
    }

    private fun submit() {
        val title = binding.titleEditText.text?.toString()?.trim().orEmpty()
        val uploader = binding.uploaderEditText.text?.toString()?.trim().orEmpty()
        var ok = true
        if (title.isEmpty()) { binding.titleInputLayout.error = getString(R.string.field_required); ok = false }
        else binding.titleInputLayout.error = null
        if (uploader.isEmpty()) { binding.uploaderInputLayout.error = getString(R.string.field_required); ok = false }
        else binding.uploaderInputLayout.error = null
        if (selectedFileName == null) { binding.fileError.isVisible = true; ok = false }
        if (!ok) return

        binding.btnUpload.isEnabled = false
        binding.uploadProgress.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            val file = ResourceFile(
                id = MockDataSource.createFileId(),
                title = title,
                description = binding.descriptionEditText.text?.toString()?.trim().orEmpty(),
                subject = binding.subjectDropdown.text.toString(),
                fileType = FileTypeUtils.fromString(binding.fileTypeDropdown.text.toString()),
                uploaderName = uploader,
                uploadDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date()),
                rating = 0f,
                downloadCount = 0
            )
            MockDataSource.files.add(0, file)
            MockDataSource.profileName = uploader
            binding.uploadProgress.isVisible = false
            binding.btnUpload.isEnabled = true
            clearForm()
            // TODO: integrate Firebase Storage
            Snackbar.make(binding.root, R.string.upload_success, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        binding.titleEditText.text?.clear()
        binding.descriptionEditText.text?.clear()
        selectedFileName = null
        binding.selectedFileName.setText(R.string.no_file_selected)
        binding.fileError.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
