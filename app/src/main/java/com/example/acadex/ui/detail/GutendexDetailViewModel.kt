package com.example.acadex.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.model.GutendexBook
import com.example.acadex.data.repository.GutendexRepository
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.result.RepoResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GutendexDetailUiState {
    object Loading : GutendexDetailUiState()
    data class Success(val book: GutendexBook, val isSaved: Boolean) : GutendexDetailUiState()
    data class Error(val message: String) : GutendexDetailUiState()
}

class GutendexDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GutendexDetailUiState>(GutendexDetailUiState.Loading)
    val uiState: StateFlow<GutendexDetailUiState> = _uiState

    private var bookId: Int = 0

    fun loadBook(id: Int) {
        bookId = id
        _uiState.value = GutendexDetailUiState.Loading
        viewModelScope.launch {
            val bookResult = GutendexRepository.fetchBookDetails(id)
            if (bookResult is RepoResult.Success) {
                val isSaved = SavedRepository.isBookSaved(id)
                _uiState.value = GutendexDetailUiState.Success(bookResult.data, isSaved)
            } else {
                val msg = (bookResult as? RepoResult.Error)?.message ?: "Failed to load book details"
                _uiState.value = GutendexDetailUiState.Error(msg)
            }
        }
    }

    fun toggleSave() {
        val state = _uiState.value
        if (state is GutendexDetailUiState.Success) {
            viewModelScope.launch {
                val nextSaved = !state.isSaved
                val result = if (nextSaved) {
                    SavedRepository.saveBook(bookId)
                } else {
                    SavedRepository.unsaveBook(bookId)
                }
                if (result is RepoResult.Success) {
                    _uiState.value = state.copy(isSaved = nextSaved)
                }
            }
        }
    }
}
