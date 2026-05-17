package com.example.classhub.util

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.example.classhub.R

object FileTypeUtils {

    fun normalizeType(fileType: String): String = when (fileType.lowercase()) {
        "document", "doc" -> "DOC"
        "pdf" -> "PDF"
        "image" -> "Image"
        "quiz" -> "Quiz"
        else -> fileType
    }

    @ColorRes
    fun textColorRes(fileType: String): Int = when (normalizeType(fileType)) {
        "PDF" -> R.color.pdf_text
        "DOC" -> R.color.doc_text
        "Image" -> R.color.image_text
        "Quiz" -> R.color.quiz_text
        else -> R.color.text_primary
    }

    @ColorRes
    fun bgColorRes(fileType: String): Int = when (normalizeType(fileType)) {
        "PDF" -> R.color.pdf_bg
        "DOC" -> R.color.doc_bg
        "Image" -> R.color.image_bg
        "Quiz" -> R.color.quiz_bg
        else -> R.color.background_light
    }

    fun displayLabel(fileType: String): String = normalizeType(fileType)

    @DrawableRes
    fun iconRes(fileType: String): Int = when (normalizeType(fileType)) {
        "PDF" -> R.drawable.ic_file_pdf
        "DOC" -> R.drawable.ic_file_doc
        "Image" -> R.drawable.ic_file_image
        "Quiz" -> R.drawable.ic_file_quiz
        else -> R.drawable.ic_file_pdf
    }
}
