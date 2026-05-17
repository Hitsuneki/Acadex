package com.example.classhub.data.models

data class ResourceFile(
    val id: Int,
    val title: String,
    val description: String,
    val subject: String,
    val fileType: String,
    val uploaderName: String,
    val uploadDate: String,
    var rating: Float,
    var ratingCount: Int = 0,
    var downloadCount: Int,
    val comments: MutableList<Comment>,
    var isSaved: Boolean = false,
    var localFileName: String? = null
)
