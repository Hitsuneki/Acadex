package com.example.acadex.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.auth.AuthRepository
import com.example.acadex.data.auth.AuthResult
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _authSuccess = MutableLiveData<Boolean>()
    val authSuccess: LiveData<Boolean> = _authSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            when (val result = repository.signIn(email, password)) {
                is AuthResult.Success -> _authSuccess.value = true
                is AuthResult.Error -> _errorMessage.value = result.message
            }
            _loading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun validate(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _errorMessage.value = "Email is required."
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _errorMessage.value = "Enter a valid email address."
            return false
        }
        if (password.isBlank()) {
            _errorMessage.value = "Password is required."
            return false
        }
        return true
    }
}
