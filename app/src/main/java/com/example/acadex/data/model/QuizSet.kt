package com.example.acadex.data.model

data class QuizSet(
    val id: Int,
    val title: String,
    val subject: String,
    val difficulty: Difficulty,
    val questions: List<QuizQuestion>
)
