package com.example.acadex.data.model

import com.google.gson.annotations.SerializedName

data class GutendexResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<GutendexBook>
)

data class GutendexBook(
    val id: Int,
    val title: String,
    val authors: List<GutendexAuthor>,
    val subjects: List<String>,
    val languages: List<String>,
    @SerializedName("download_count") val downloadCount: Int,
    val formats: Map<String, String>
) {
    fun getPdfUrl(): String? = formats["application/pdf"]
    fun getHtmlUrl(): String? = formats["text/html"]
    fun getTxtUrl(): String? = formats["text/plain"] ?: formats.entries.firstOrNull { it.key.startsWith("text/plain") }?.value
}

data class GutendexAuthor(
    val name: String
)

fun GutendexBook.toResourceFile(isSaved: Boolean): ResourceFile {
    val formatUrl = getPdfUrl() ?: getHtmlUrl() ?: getTxtUrl()
    return ResourceFile(
        id = "gutendex_$id",
        title = title,
        description = authors.joinToString(", ") { it.name },
        subject = subjects.firstOrNull() ?: "General",
        fileType = FileType.BOOK,
        uploaderName = "Project Gutenberg",
        uploadDate = "Downloads: $downloadCount",
        rating = 0f,
        ratingCount = 0,
        downloadCount = downloadCount,
        isSaved = isSaved,
        remoteId = id.toString(),
        storagePath = null,
        downloadUrl = formatUrl
    )
}
