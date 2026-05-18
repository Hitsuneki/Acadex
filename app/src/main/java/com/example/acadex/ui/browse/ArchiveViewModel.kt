package com.example.acadex.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.ResourceRepository
import com.example.acadex.data.SortOption
import com.example.acadex.data.model.ResourceFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ArchiveUiState {
    object Loading : ArchiveUiState()
    data class Success(val files: List<ResourceFile>) : ArchiveUiState()
    data class Error(val message: String) : ArchiveUiState()
}

class ArchiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ArchiveUiState>(ArchiveUiState.Loading)
    val uiState: StateFlow<ArchiveUiState> = _uiState

    private var subject = "All"
    private var query = ""
    private var sortBy = SortOption.NEWEST

    init {
        refresh()
    }

    fun setSubject(s: String) {
        subject = s
        refresh()
    }

    fun setQuery(q: String) {
        query = q
        refresh()
    }

    fun setSort(sort: SortOption) {
        sortBy = sort
        refresh()
    }

    fun refresh() {
        _uiState.value = ArchiveUiState.Loading
        viewModelScope.launch {
            try {
                ResourceRepository.refreshFromSupabase()
                val files = ResourceRepository.filterFiles(subject, query, sortBy)
                _uiState.value = ArchiveUiState.Success(files)
            } catch (e: Exception) {
                _uiState.value = ArchiveUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}
