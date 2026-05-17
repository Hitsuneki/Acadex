package com.example.acadex.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaterialRow(
    val id: String,
    val title: String,
    val description: String = "",
    val subject: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("uploader_id") val uploaderId: String? = null,
    @SerialName("uploader_name") val uploaderName: String,
    @SerialName("storage_path") val storagePath: String? = null,
    @SerialName("rating_avg") val ratingAvg: Float = 0f,
    @SerialName("rating_count") val ratingCount: Int = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    val tags: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MaterialInsert(
    val title: String,
    val description: String = "",
    val subject: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("uploader_id") val uploaderId: String? = null,
    @SerialName("uploader_name") val uploaderName: String,
    @SerialName("storage_path") val storagePath: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val section: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ProfileUpsert(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val section: String = ""
)

@Serializable
data class ProfileUpdate(
    @SerialName("display_name") val displayName: String,
    val section: String
)

@Serializable
data class QuizHistoryRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("quiz_set_id") val quizSetId: String,
    val score: Int,
    val total: Int,
    @SerialName("taken_at") val takenAt: String? = null
)

@Serializable
data class QuizHistoryInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("quiz_set_id") val quizSetId: String,
    val score: Int,
    val total: Int
)

@Serializable
data class QuizSetRow(
    val id: String,
    val title: String,
    val subject: String,
    val difficulty: String = "EASY"
)

@Serializable
data class CommentRow(
    val id: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("commenter_name") val commenterName: String,
    val body: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CommentInsert(
    @SerialName("material_id") val materialId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("commenter_name") val commenterName: String,
    val body: String
)

@Serializable
data class RatingRow(
    val id: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    val rating: Float
)

@Serializable
data class RatingInsert(
    @SerialName("material_id") val materialId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    val rating: Float
)

@Serializable
data class SavedMaterialRow(
    @SerialName("user_id") val userId: String,
    @SerialName("material_id") val materialId: String
)

@Serializable
data class MaterialStatsUpdate(
    @SerialName("rating_avg") val ratingAvg: Float,
    @SerialName("rating_count") val ratingCount: Int
)

@Serializable
data class DownloadCountUpdate(
    @SerialName("download_count") val downloadCount: Int
)
