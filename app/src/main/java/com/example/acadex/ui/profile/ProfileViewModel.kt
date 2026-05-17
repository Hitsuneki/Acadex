package com.example.acadex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.repository.MaterialRepository
import com.example.acadex.data.repository.ProfileRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ProfileUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileUiData>> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val profileResult = ProfileRepository.loadProfile(force = true)) {
                is RepoResult.Error -> {
                    _uiState.value = UiState.Error(profileResult.message)
                    return@launch
                }
                is RepoResult.Success -> {
                    val profile = profileResult.data
                    when (val statsResult = MaterialRepository.fetchUserStats(profile.id)) {
                        is RepoResult.Error -> _uiState.value = UiState.Error(statsResult.message)
                        is RepoResult.Success -> {
                            val stats = statsResult.data
                            _uiState.value = UiState.Success(
                                ProfileUiData(
                                    displayName = profile.displayName,
                                    aboutMe = profile.aboutMe,
                                    gender = profile.gender,
                                    status = profile.status,
                                    uploads = stats.uploads,
                                    downloads = stats.downloads,
                                    avgRating = stats.avgRating
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun initials(name: String): String =
        name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
            .ifEmpty { "?" }

    fun formatStatus(status: String): String = when (status.lowercase()) {
        "teacher" -> "Teacher"
        "other" -> "Other"
        else -> "Student"
    }
}

data class ProfileUiData(
    val displayName: String,
    val aboutMe: String,
    val gender: String,
    val status: String,
    val uploads: Int,
    val downloads: Int,
    val avgRating: Float
)
