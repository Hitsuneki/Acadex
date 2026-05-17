package com.example.acadex.util

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.example.acadex.R
import com.example.acadex.data.model.FileType

object FileTypeUtils {

    fun fromString(value: String): FileType = when (value.uppercase()) {
        "PDF" -> FileType.PDF
        "DOC", "DOCUMENT" -> FileType.DOC
        "IMAGE" -> FileType.IMAGE
        "QUIZ" -> FileType.QUIZ
        else -> FileType.PDF
    }

    @ColorRes fun bgRes(type: FileType): Int = when (type) {
        FileType.PDF -> R.color.type_pdf_bg
        FileType.DOC -> R.color.type_doc_bg
        FileType.IMAGE -> R.color.type_img_bg
        FileType.QUIZ -> R.color.type_quiz_bg
    }

    @ColorRes fun fgRes(type: FileType): Int = when (type) {
        FileType.PDF -> R.color.type_pdf_fg
        FileType.DOC -> R.color.type_doc_fg
        FileType.IMAGE -> R.color.type_img_fg
        FileType.QUIZ -> R.color.type_quiz_fg
    }

    @DrawableRes fun iconRes(type: FileType): Int = when (type) {
        FileType.PDF -> R.drawable.ic_file_pdf
        FileType.DOC -> R.drawable.ic_file_doc
        FileType.IMAGE -> R.drawable.ic_file_image
        FileType.QUIZ -> R.drawable.ic_file_quiz
    }
}
