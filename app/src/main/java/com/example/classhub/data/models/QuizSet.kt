package com.example.classhub.data.models

data class QuizSet(
    val id: Int,
    val title: String,
    val subject: String,
    val difficulty: String,
    val questions: List<QuizQuestion>
)
