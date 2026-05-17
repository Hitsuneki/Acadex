package com.example.acadex.ui.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.model.FileType
import com.example.acadex.data.repository.MaterialRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.util.MimeTypeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadViewModel : ViewModel() {

    private val _formState = MutableStateFlow(UploadFormState())
    val formState: StateFlow<UploadFormState> = _formState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    fun onFileSelected(context: Context, uri: Uri, displayName: String) {
        val mime = MimeTypeUtils.mimeType(context, uri)
        val type = MimeTypeUtils.fileTypeFromMime(mime)
        if (type == null) {
            _uploadState.value = UploadUiState.Snackbar(UploadMessage.UNSUPPORTED_TYPE)
            return
        }
        _formState.update {
            it.copy(
                fileUri = uri,
                fileName = displayName,
                fileType = type,
                fileError = false
            )
        }
    }

    fun clearFile() {
        _formState.value = UploadFormState()
    }

    fun setTitle(value: String) = _formState.update { it.copy(title = value, titleError = false) }
    fun setDescription(value: String) = _formState.update { it.copy(description = value) }
    fun setSubject(value: String) = _formState.update { it.copy(subject = value, subjectError = false) }
    fun setTags(tags: List<String>) = _formState.update { it.copy(tags = tags) }

    fun submit(context: Context) {
        val form = _formState.value
        var valid = true
        if (form.fileUri == null) valid = false
        if (form.title.isBlank()) valid = false
        if (form.subject.isBlank()) valid = false
        if (!valid) {
            _formState.update {
                it.copy(
                    fileError = form.fileUri == null,
                    titleError = form.title.isBlank(),
                    subjectError = form.subject.isBlank()
                )
            }
            return
        }

        val uri = form.fileUri!!
        val fileType = form.fileType!!
        _uploadState.value = UploadUiState.Uploading

        viewModelScope.launch {
            when (
                val result = MaterialRepository.upload(
                    context = context,
                    uri = uri,
                    fileName = form.fileName,
                    fileType = fileType,
                    title = form.title.trim(),
                    description = form.description.trim(),
                    subject = form.subject.trim(),
                    tags = form.tags
                )
            ) {
                is RepoResult.Success -> {
                    _formState.value = UploadFormState()
                    _uploadState.value = UploadUiState.Success
                }
                is RepoResult.Error -> {
                    _uploadState.value = UploadUiState.Snackbar(
                        if (result.message.contains("connection", ignoreCase = true)) {
                            UploadMessage.UPLOAD_NETWORK
                        } else {
                            UploadMessage.UPLOAD_FAILED
                        }
                    )
                }
            }
        }
    }

    fun onUploadMessageShown() {
        if (_uploadState.value !is UploadUiState.Uploading) {
            _uploadState.value = UploadUiState.Idle
        }
    }
}

data class UploadFormState(
    val fileUri: Uri? = null,
    val fileName: String = "",
    val fileType: FileType? = null,
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val tags: List<String> = emptyList(),
    val fileError: Boolean = false,
    val titleError: Boolean = false,
    val subjectError: Boolean = false
)

sealed class UploadUiState {
    data object Idle : UploadUiState()
    data object Uploading : UploadUiState()
    data object Success : UploadUiState()
    data class Snackbar(val message: UploadMessage) : UploadUiState()
}

enum class UploadMessage {
    UNSUPPORTED_TYPE,
    UPLOAD_FAILED,
    UPLOAD_NETWORK,
    METADATA_FAILED
}
