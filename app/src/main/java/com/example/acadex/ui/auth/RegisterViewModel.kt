package com.example.acadex.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.auth.AuthRepository
import com.example.acadex.data.auth.AuthResult
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _authSuccess = MutableLiveData<Boolean>()
    val authSuccess: LiveData<Boolean> = _authSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun signUp(email: String, password: String, confirmPassword: String) {
        if (!validate(email, password, confirmPassword)) return
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            when (val result = repository.signUp(email, password)) {
                is AuthResult.Success -> _authSuccess.value = true
                is AuthResult.Error -> _errorMessage.value = result.message
            }
            _loading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun validate(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isBlank()) {
            _errorMessage.value = "Email is required."
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _errorMessage.value = "Enter a valid email address."
            return false
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return false
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Passwords do not match."
            return false
        }
        return true
    }
}
