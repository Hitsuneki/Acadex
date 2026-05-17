package com.example.acadex.data.model

data class ResourceFile(
    val id: String,
    val title: String,
    val description: String = "",
    val subject: String,
    val fileType: FileType,
    val uploaderName: String,
    val uploadDate: String,
    var rating: Float,
    var ratingCount: Int = 0,
    var downloadCount: Int,
    val comments: MutableList<Comment> = mutableListOf(),
    var isSaved: Boolean = false,
    /** Supabase row id; null = mock-only or session-local entry */
    val remoteId: String? = null,
    val storagePath: String? = null,
    val downloadUrl: String? = null
) {
    val isRemote: Boolean get() = remoteId != null
}
