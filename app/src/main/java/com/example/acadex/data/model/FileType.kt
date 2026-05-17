package com.example.acadex.data.model

enum class FileType {
    PDF, DOC, IMAGE, QUIZ;

    fun displayName(): String = when (this) {
        PDF -> "PDF"
        DOC -> "DOC"
        IMAGE -> "Image"
        QUIZ -> "Quiz"
    }
}
