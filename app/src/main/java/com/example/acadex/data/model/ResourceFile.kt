package com.example.acadex.data.model

data class ResourceFile(
    val id: Int,
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
    var isSaved: Boolean = false
)
