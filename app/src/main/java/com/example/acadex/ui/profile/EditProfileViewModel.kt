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

    private val _aboutMe = MutableStateFlow("")
    val aboutMe: StateFlow<String> = _aboutMe.asStateFlow()

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _status = MutableStateFlow("student")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    init {
        viewModelScope.launch {
            ProfileRepository.loadProfile()
            ProfileRepository.cachedProfile.value?.let { profile ->
                _displayName.value = profile.displayName
                _aboutMe.value = profile.aboutMe
                _gender.value = profile.gender
                _status.value = profile.status
            }
        }
    }

    fun setDisplayName(value: String) {
        _displayName.value = value
    }

    fun setAboutMe(value: String) {
        _aboutMe.value = value
    }

    fun setGender(value: String) {
        _gender.value = value
    }

    fun setStatus(value: String) {
        _status.value = value
    }

    fun save() {
        val name = _displayName.value.trim()
        if (name.isEmpty()) {
            _saveResult.value = SaveResult.ValidationError
            return
        }
        viewModelScope.launch {
            when (
                val result = ProfileRepository.updateProfile(
                    displayName = name,
                    aboutMe = _aboutMe.value.trim(),
                    gender = _gender.value.trim(),
                    status = _status.value
                )
            ) {
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
