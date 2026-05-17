package com.example.acadex.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.model.Comment
import com.example.acadex.data.model.FileType
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.repository.MaterialRepository
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.ui.common.UiState
import com.example.acadex.ui.saved.SavedIndexSharedViewModel
import com.example.acadex.util.DownloadHelper
import com.example.acadex.util.FileTypeUtils
import com.example.acadex.util.StorageUrlHelper
import com.example.acadex.util.UserIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileDetailViewModel(
    application: Application,
    private val savedShared: SavedIndexSharedViewModel
) : AndroidViewModel(application) {

    private val _screenState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val screenState: StateFlow<UiState<Unit>> = _screenState.asStateFlow()

    private val _material = MutableStateFlow<ResourceFile?>(null)
    val material: StateFlow<ResourceFile?> = _material.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentsState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val commentsState: StateFlow<UiState<Unit>> = _commentsState.asStateFlow()

    private val _userRating = MutableStateFlow<Float?>(null)
    val userRating: StateFlow<Float?> = _userRating.asStateFlow()

    private val _selectedStars = MutableStateFlow(0)
    val selectedStars: StateFlow<Int> = _selectedStars.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()

    private val _downloadCount = MutableStateFlow(0)
    val downloadCount: StateFlow<Int> = _downloadCount.asStateFlow()

    private val _ratingAvg = MutableStateFlow(0f)
    val ratingAvg: StateFlow<Float> = _ratingAvg.asStateFlow()

    private val _ratingError = MutableStateFlow<String?>(null)
    val ratingError: StateFlow<String?> = _ratingError.asStateFlow()

    private val _postingComment = MutableStateFlow(false)
    val postingComment: StateFlow<Boolean> = _postingComment.asStateFlow()

    private val _viewerState = MutableStateFlow<ViewerUiState>(ViewerUiState.Idle)
    val viewerState: StateFlow<ViewerUiState> = _viewerState.asStateFlow()

    private val _showGoogleDocsWebView = MutableStateFlow(false)
    val showGoogleDocsWebView: StateFlow<Boolean> = _showGoogleDocsWebView.asStateFlow()

    private var materialId: String = ""
    private var pdfCacheFile: File? = null

    fun load(materialId: String) {
        this.materialId = materialId
        _screenState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = MaterialRepository.loadFileDetail(materialId, UserIdentity.uidOrNull())) {
                is RepoResult.Success -> {
                    val bundle = result.data
                    val material = bundle.material.copy(
                        downloadUrl = bundle.material.downloadUrl
                            ?: StorageUrlHelper.publicUrl(bundle.material.storagePath)
                    )
                    _material.value = material
                    _comments.value = bundle.comments
                    _commentsState.value = if (bundle.commentsError != null) {
                        UiState.Error(bundle.commentsError)
                    } else {
                        UiState.Success(Unit)
                    }
                    _userRating.value = bundle.userRating
                    _selectedStars.value = bundle.userRating?.toInt() ?: 0
                    val saved = savedShared.resolveSaved(materialId, bundle.isSaved)
                    _isSaved.value = saved
                    _commentCount.value = bundle.comments.size
                    _downloadCount.value = bundle.material.downloadCount
                    _ratingAvg.value = bundle.material.rating
                    _screenState.value = UiState.Success(Unit)
                    prepareViewer(material)
                }
                is RepoResult.Error -> {
                    _screenState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun retryLoad() = load(materialId)

    fun retryComments() {
        _commentsState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = MaterialRepository.fetchComments(materialId)) {
                is RepoResult.Success -> {
                    _comments.value = result.data
                    _commentCount.value = result.data.size
                    _commentsState.value = UiState.Success(Unit)
                }
                is RepoResult.Error -> {
                    _commentsState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun prepareViewer(material: ResourceFile) {
        if (material.storagePath.isNullOrBlank()) {
            _viewerState.value = ViewerUiState.Error(
                getApplication<Application>().getString(com.example.acadex.R.string.viewer_load_failed)
            )
            return
        }
        val fileType = FileTypeUtils.inferFromPath(material.storagePath, material.fileType)
        when (fileType) {
            FileType.PDF -> loadPdf(material)
            FileType.JPEG, FileType.PNG, FileType.IMAGE -> loadImage(material)
            FileType.TXT -> loadText(material)
            FileType.DOCX, FileType.PPTX, FileType.DOC -> loadOfficePlaceholder(material)
            else -> loadOfficePlaceholder(material)
        }
    }

    private fun loadOfficePlaceholder(material: ResourceFile) {
        val fileName = StorageUrlHelper.fileNameFromPath(material.storagePath)
        val url = material.downloadUrl ?: StorageUrlHelper.publicUrl(material.storagePath).orEmpty()
        _viewerState.value = ViewerUiState.OfficePlaceholder(
            fileName = fileName,
            fileType = material.fileType,
            fileSizeLabel = "",
            publicUrl = url
        )
        viewModelScope.launch {
            when (val result = MaterialRepository.downloadFile(material.storagePath, material.downloadUrl)) {
                is RepoResult.Success -> {
                    val sizeLabel = StorageUrlHelper.formatFileSize(result.data.size.toLong())
                    if (_viewerState.value is ViewerUiState.OfficePlaceholder) {
                        _viewerState.value = ViewerUiState.OfficePlaceholder(
                            fileName = fileName,
                            fileType = material.fileType,
                            fileSizeLabel = sizeLabel,
                            publicUrl = url
                        )
                    }
                }
                is RepoResult.Error -> Unit
            }
        }
    }

    fun retryViewer() {
        _material.value?.let { prepareViewer(it) }
    }

    fun openGoogleDocsViewer() {
        val material = _material.value ?: return
        val url = material.downloadUrl ?: return
        _showGoogleDocsWebView.value = true
        _viewerState.value = ViewerUiState.GoogleDocs(StorageUrlHelper.googleDocsViewerUrl(url))
    }

    fun closeGoogleDocsViewer() {
        _showGoogleDocsWebView.value = false
        _material.value?.let { m ->
            if (m.fileType == FileType.DOCX || m.fileType == FileType.PPTX || m.fileType == FileType.DOC) {
                prepareViewer(m)
            }
        }
    }

    private fun loadImage(material: ResourceFile) {
        _viewerState.value = ViewerUiState.Loading
        viewModelScope.launch {
            when (val bytesResult = MaterialRepository.downloadFile(material.storagePath, material.downloadUrl)) {
                is RepoResult.Error -> {
                    _viewerState.value = ViewerUiState.Error(
                        getApplication<Application>().getString(com.example.acadex.R.string.viewer_load_failed)
                    )
                }
                is RepoResult.Success -> {
                    val ext = material.storagePath?.substringAfterLast('.', "jpg") ?: "jpg"
                    val file = withContext(Dispatchers.IO) {
                        File.createTempFile("acadex_img_", ".$ext", getApplication<Application>().cacheDir).apply {
                            writeBytes(bytesResult.data)
                        }
                    }
                    _viewerState.value = ViewerUiState.ImageFile(file)
                }
            }
        }
    }

    private fun loadPdf(material: ResourceFile) {
        _viewerState.value = ViewerUiState.Loading
        viewModelScope.launch {
            when (val bytesResult = MaterialRepository.downloadFile(material.storagePath, material.downloadUrl)) {
                is RepoResult.Error -> {
                    _viewerState.value = ViewerUiState.Error(
                        getApplication<Application>().getString(com.example.acadex.R.string.viewer_load_failed)
                    )
                }
                is RepoResult.Success -> {
                    val file = withContext(Dispatchers.IO) {
                        File.createTempFile("acadex_pdf_", ".pdf", getApplication<Application>().cacheDir).apply {
                            writeBytes(bytesResult.data)
                        }
                    }
                    pdfCacheFile = file
                    val pageCount = withContext(Dispatchers.IO) {
                        runCatching {
                            android.graphics.pdf.PdfRenderer(
                                android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                            ).use { it.pageCount }
                        }.getOrDefault(0)
                    }
                    if (pageCount <= 0) {
                        _viewerState.value = ViewerUiState.Error(
                            getApplication<Application>().getString(com.example.acadex.R.string.viewer_load_failed)
                        )
                    } else {
                        _viewerState.value = ViewerUiState.Pdf(file, pageCount)
                    }
                }
            }
        }
    }

    private fun loadText(material: ResourceFile) {
        _viewerState.value = ViewerUiState.Loading
        viewModelScope.launch {
            when (val bytesResult = MaterialRepository.downloadFile(material.storagePath, material.downloadUrl)) {
                is RepoResult.Error -> {
                    _viewerState.value = ViewerUiState.Error(
                        getApplication<Application>().getString(com.example.acadex.R.string.viewer_load_failed)
                    )
                }
                is RepoResult.Success -> {
                    val max = 50 * 1024
                    val bytes = bytesResult.data
                    val truncated = bytes.size > max
                    val text = String(if (truncated) bytes.copyOf(max) else bytes, Charsets.UTF_8)
                    _viewerState.value = ViewerUiState.Text(text, truncated)
                }
            }
        }
    }

    fun pdfCacheFile(): File? = pdfCacheFile

    fun selectStars(count: Int) {
        if (count < 1) return
        if (_userRating.value != null && count == _selectedStars.value) return
        _selectedStars.value = count
        _ratingError.value = null
    }

    fun submitRating() {
        val stars = _selectedStars.value
        if (stars <= 0) {
            _ratingError.value = getApplication<Application>().getString(
                com.example.acadex.R.string.rating_required
            )
            return
        }
        _ratingError.value = null
        viewModelScope.launch {
            val hadRating = _userRating.value != null
            when (val result = MaterialRepository.submitRating(materialId, stars.toFloat(), UserIdentity.displayName())) {
                is RepoResult.Success -> {
                    _material.value = result.data
                    _ratingAvg.value = result.data.rating
                    _userRating.value = stars.toFloat()
                    _events.value = FileDetailEvent.Snackbar(
                        if (hadRating) com.example.acadex.R.string.rating_updated
                        else com.example.acadex.R.string.rating_submitted
                    )
                }
                is RepoResult.Error -> {
                    _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.rating_failed)
                }
            }
        }
    }

    fun postComment(text: String) {
        val body = text.trim()
        if (body.isEmpty() || _postingComment.value) return
        _postingComment.value = true
        viewModelScope.launch {
            when (val result = MaterialRepository.postComment(materialId, body, UserIdentity.displayName())) {
                is RepoResult.Success -> {
                    _comments.update { it + result.data }
                    _commentCount.update { it + 1 }
                    _events.value = FileDetailEvent.CommentPosted
                }
                is RepoResult.Error -> {
                    _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.comment_failed)
                }
            }
            _postingComment.value = false
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            when (MaterialRepository.deleteComment(comment.id)) {
                is RepoResult.Success -> {
                    _comments.update { list -> list.filter { it.id != comment.id } }
                    _commentCount.update { (it - 1).coerceAtLeast(0) }
                }
                is RepoResult.Error -> {
                    _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.comment_delete_failed)
                }
            }
        }
    }

    fun toggleSave() {
        val currentlySaved = _isSaved.value
        val optimistic = !currentlySaved
        _isSaved.value = optimistic
        savedShared.setSaved(materialId, optimistic)
        viewModelScope.launch {
            val result = if (optimistic) SavedRepository.save(materialId) else SavedRepository.unsave(materialId)
            if (result is RepoResult.Error) {
                _isSaved.value = currentlySaved
                savedShared.setSaved(materialId, currentlySaved)
                _events.value = FileDetailEvent.Snackbar(
                    if (optimistic) com.example.acadex.R.string.save_failed
                    else com.example.acadex.R.string.unsave_failed
                )
            }
        }
    }

    fun startDownload() {
        val material = _material.value ?: return
        val fileName = StorageUrlHelper.fileNameFromPath(material.storagePath)
        val app = getApplication<Application>()
        _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.downloading_to_folder)
        viewModelScope.launch {
            when (val result = MaterialRepository.downloadFile(material.storagePath, material.downloadUrl)) {
                is RepoResult.Success -> {
                    val saved = DownloadHelper.saveBytesToDownloads(app, result.data, fileName)
                    if (saved) {
                        _downloadCount.update { it + 1 }
                        MaterialRepository.incrementDownloadCount(materialId)
                        _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.download_complete)
                    } else {
                        _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.download_failed)
                    }
                }
                is RepoResult.Error -> {
                    val url = material.downloadUrl ?: StorageUrlHelper.publicUrl(material.storagePath)
                    val downloadId = url?.let { DownloadHelper.enqueueDownload(app, it, material.title, fileName) }
                    if (downloadId != null) {
                        _pendingDownloadId.value = downloadId
                        _downloadCount.update { it + 1 }
                        MaterialRepository.incrementDownloadCount(materialId)
                    } else {
                        _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.download_failed)
                    }
                }
            }
        }
    }

    private val _pendingDownloadId = MutableStateFlow<Long?>(null)
    val pendingDownloadId: StateFlow<Long?> = _pendingDownloadId.asStateFlow()

    fun onDownloadComplete(downloadId: Long) {
        if (_pendingDownloadId.value == downloadId) {
            _pendingDownloadId.value = null
            _events.value = FileDetailEvent.Snackbar(com.example.acadex.R.string.download_complete)
        }
    }

    fun shareMessage(): String? {
        val material = _material.value ?: return null
        val url = material.downloadUrl ?: StorageUrlHelper.publicUrl(material.storagePath) ?: return null
        return "Check out this material on Acadex: ${material.title} — $url"
    }

    fun isUpdateRating(): Boolean = _userRating.value != null

    private val _events = MutableStateFlow<FileDetailEvent?>(null)
    val events: StateFlow<FileDetailEvent?> = _events.asStateFlow()

    fun consumeEvent() {
        _events.value = null
    }

    override fun onCleared() {
        pdfCacheFile?.delete()
        super.onCleared()
    }
}

sealed class ViewerUiState {
    data object Idle : ViewerUiState()
    data object Loading : ViewerUiState()
    data class Pdf(val cacheFile: File, val pageCount: Int) : ViewerUiState()
    data class Image(val url: String) : ViewerUiState()
    data class ImageFile(val cacheFile: File) : ViewerUiState()
    data class Text(val content: String, val truncated: Boolean) : ViewerUiState()
    data class OfficePlaceholder(
        val fileName: String,
        val fileType: FileType,
        val fileSizeLabel: String,
        val publicUrl: String
    ) : ViewerUiState()
    data class GoogleDocs(val viewerUrl: String) : ViewerUiState()
    data class Error(val message: String) : ViewerUiState()
}

sealed class FileDetailEvent {
    data class Snackbar(val messageRes: Int) : FileDetailEvent()
    data object CommentPosted : FileDetailEvent()
}
