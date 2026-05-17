package com.example.acadex.data.model

enum class Difficulty {
    EASY, MEDIUM, HARD;

    fun label(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}
