package com.example.acadex.util

import com.google.firebase.auth.FirebaseAuth

object UserIdentity {

    fun requireUid(): String =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: error("User must be signed in")

    fun uidOrNull(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun displayName(): String {
        val user = FirebaseAuth.getInstance().currentUser ?: return "Student"
        return user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Student"
    }
}
