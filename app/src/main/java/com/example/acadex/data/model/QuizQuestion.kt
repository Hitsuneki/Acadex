package com.example.acadex.data.model

data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    val correctIndex: Int
)
