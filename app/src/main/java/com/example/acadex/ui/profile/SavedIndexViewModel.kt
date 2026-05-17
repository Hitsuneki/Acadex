package com.example.acadex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedIndexViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ResourceFile>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ResourceFile>>> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = SavedRepository.fetchSavedMaterials()) {
                is RepoResult.Success -> _uiState.value = UiState.Success(result.data)
                is RepoResult.Error -> _uiState.value = UiState.Error(result.message)
            }
        }
    }

    fun unsave(file: ResourceFile) {
        viewModelScope.launch {
            when (SavedRepository.unsave(file.id)) {
                is RepoResult.Success -> {
                    val current = (_uiState.value as? UiState.Success)?.data.orEmpty()
                    _uiState.value = UiState.Success(current.filter { it.id != file.id })
                }
                is RepoResult.Error -> Unit
            }
        }
    }
}
