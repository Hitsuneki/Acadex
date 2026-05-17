package com.example.acadex.data.model

data class QuizHistoryEntry(
    val id: String,
    val quizTitle: String,
    val subject: String,
    val difficulty: String,
    val score: Int,
    val total: Int,
    val takenAt: String
) {
    val percentage: Int
        get() = if (total == 0) 0 else ((score.toFloat() / total) * 100).toInt()
}
