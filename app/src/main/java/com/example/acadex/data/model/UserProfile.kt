package com.example.acadex.data.model

data class UserProfile(
    val id: String,
    val displayName: String,
    val aboutMe: String = "",
    val gender: String = "",
    val status: String = "student"
)
