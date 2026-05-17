package com.example.acadex.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser get() = auth.currentUser

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e))
        }
    }

    suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun mapAuthError(e: Exception): String = when (e) {
        is FirebaseAuthInvalidUserException -> "No account found with this email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        else -> e.localizedMessage ?: "Authentication failed. Please try again."
    }
}

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}
