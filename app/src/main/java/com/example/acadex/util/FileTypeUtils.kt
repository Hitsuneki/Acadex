package com.example.acadex.util

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.example.acadex.R
import com.example.acadex.data.model.FileType

object FileTypeUtils {

    fun fromString(value: String): FileType = when (value.uppercase()) {
        "PDF" -> FileType.PDF
        "DOCX", "WORD" -> FileType.DOCX
        "PPTX", "POWERPOINT" -> FileType.PPTX
        "JPEG", "JPG" -> FileType.JPEG
        "PNG" -> FileType.PNG
        "TXT", "TEXT" -> FileType.TXT
        "DOC", "DOCUMENT" -> FileType.DOC
        "IMAGE" -> FileType.IMAGE
        "QUIZ" -> FileType.QUIZ
        else -> FileType.PDF
    }

    @ColorRes
    fun bgRes(type: FileType): Int = when (type) {
        FileType.PDF -> R.color.type_pdf_bg
        FileType.DOCX, FileType.DOC, FileType.PPTX, FileType.TXT -> R.color.type_doc_bg
        FileType.JPEG, FileType.PNG, FileType.IMAGE -> R.color.type_img_bg
        FileType.QUIZ -> R.color.type_quiz_bg
    }

    @ColorRes
    fun fgRes(type: FileType): Int = when (type) {
        FileType.PDF -> R.color.type_pdf_fg
        FileType.DOCX, FileType.DOC, FileType.PPTX, FileType.TXT -> R.color.type_doc_fg
        FileType.JPEG, FileType.PNG, FileType.IMAGE -> R.color.type_img_fg
        FileType.QUIZ -> R.color.type_quiz_fg
    }

    @DrawableRes
    fun iconRes(type: FileType): Int = when (type) {
        FileType.PDF -> R.drawable.ic_file_pdf
        FileType.DOCX, FileType.DOC, FileType.PPTX, FileType.TXT -> R.drawable.ic_file_doc
        FileType.JPEG, FileType.PNG, FileType.IMAGE -> R.drawable.ic_file_image
        FileType.QUIZ -> R.drawable.ic_file_quiz
    }
}
