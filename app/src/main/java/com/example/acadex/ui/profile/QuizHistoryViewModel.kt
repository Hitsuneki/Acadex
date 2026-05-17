package com.example.acadex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.model.QuizHistoryEntry
import com.example.acadex.data.repository.QuizRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<QuizHistoryEntry>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<QuizHistoryEntry>>> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = QuizRepository.fetchHistory()) {
                is RepoResult.Success -> _uiState.value = UiState.Success(result.data)
                is RepoResult.Error -> _uiState.value = UiState.Error(result.message)
            }
        }
    }
}
