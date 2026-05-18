package com.example.acadex.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.example.acadex.data.model.FileType

object MimeTypeUtils {

    val allowedMimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "image/jpeg",
        "image/png",
        "text/plain"
    )

    fun mimeType(context: Context, uri: Uri): String? {
        context.contentResolver.getType(uri)?.let { return it }
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (!ext.isNullOrBlank()) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        }
        return null
    }

    fun fileTypeFromMime(mime: String?): FileType? = when (mime) {
        "application/pdf" -> FileType.PDF
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> FileType.DOCX
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> FileType.PPTX
        "image/jpeg" -> FileType.JPEG
        "image/png" -> FileType.PNG
        "text/plain" -> FileType.TXT
        else -> null
    }

    fun extensionLabel(fileType: FileType): String = when (fileType) {
        FileType.PDF -> "PDF"
        FileType.DOCX -> "DOCX"
        FileType.PPTX -> "PPTX"
        FileType.JPEG -> "JPG"
        FileType.PNG -> "PNG"
        FileType.TXT -> "TXT"
        FileType.DOC -> "DOC"
        FileType.IMAGE -> "IMG"
        FileType.QUIZ -> "QUIZ"
        FileType.BOOK -> "BOOK"
    }

    fun storageFileType(fileType: FileType): String = when (fileType) {
        FileType.PDF -> "PDF"
        FileType.DOCX -> "DOCX"
        FileType.PPTX -> "PPTX"
        FileType.JPEG -> "JPEG"
        FileType.PNG -> "PNG"
        FileType.TXT -> "TXT"
        else -> fileType.name
    }

    fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "upload" }
}
