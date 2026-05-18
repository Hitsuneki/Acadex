package com.example.acadex.data.model

enum class FileType {
    PDF, DOCX, PPTX, JPEG, PNG, TXT,
    /** Legacy mock types */
    DOC, IMAGE, QUIZ, BOOK;

    fun displayName(): String = when (this) {
        PDF -> "PDF"
        DOCX -> "Word"
        PPTX -> "PowerPoint"
        JPEG -> "JPEG"
        PNG -> "PNG"
        TXT -> "Text"
        DOC -> "DOC"
        IMAGE -> "Image"
        QUIZ -> "Quiz"
        BOOK -> "Book"
    }
}
