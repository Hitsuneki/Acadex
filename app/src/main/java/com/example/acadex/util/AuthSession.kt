package com.example.acadex.util

import com.google.firebase.auth.FirebaseAuth

object AuthSession {

    fun syncProfileFromFirebase() {
        // Profile display name is synced via ProfileRepository.ensureProfileExists()
    }

    fun isLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null
}
