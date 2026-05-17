package com.example.classhub.data.models

data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    val correctIndex: Int
)
