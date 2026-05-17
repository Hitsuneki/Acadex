package com.example.acadex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.repository.ProfileRepository
import com.example.acadex.data.result.RepoResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _section = MutableStateFlow("")
    val section: StateFlow<String> = _section.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    init {
        ProfileRepository.cachedProfile.value?.let {
            _displayName.value = it.displayName
            _section.value = it.section
        }
    }

    fun setDisplayName(value: String) {
        _displayName.value = value
    }

    fun setSection(value: String) {
        _section.value = value
    }

    fun save() {
        val name = _displayName.value.trim()
        if (name.isEmpty()) {
            _saveResult.value = SaveResult.ValidationError
            return
        }
        viewModelScope.launch {
            when (val result = ProfileRepository.updateProfile(name, _section.value.trim())) {
                is RepoResult.Success -> _saveResult.value = SaveResult.Success
                is RepoResult.Error -> _saveResult.value = SaveResult.Failed
            }
        }
    }

    fun onSaveHandled() {
        _saveResult.value = null
    }
}

sealed class SaveResult {
    data object Success : SaveResult()
    data object Failed : SaveResult()
    data object ValidationError : SaveResult()
}
