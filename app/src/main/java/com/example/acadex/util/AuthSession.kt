package com.example.acadex.util

import com.example.acadex.data.MockDataSource
import com.google.firebase.auth.FirebaseAuth

object AuthSession {

    fun syncProfileFromFirebase() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Student"
        MockDataSource.profileName = displayName
    }

    fun isLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null
}
