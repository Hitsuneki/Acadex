package com.example.acadex.data.model

data class Comment(
    val id: String,
    val userId: String?,
    val commenterName: String,
    val text: String,
    val createdAtIso: String,
    val displayDate: String,
    val relativeTime: String
)
