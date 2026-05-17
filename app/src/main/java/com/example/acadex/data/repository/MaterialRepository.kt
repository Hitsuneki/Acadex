package com.example.acadex.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.acadex.data.model.Comment
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.result.RepoResult
import com.example.acadex.data.result.userMessage
import com.example.acadex.data.supabase.CommentInsert
import com.example.acadex.data.supabase.CommentRow
import com.example.acadex.data.supabase.DownloadCountUpdate
import com.example.acadex.data.supabase.MaterialInsert
import com.example.acadex.data.supabase.MaterialRow
import com.example.acadex.data.supabase.MaterialStatsUpdate
import com.example.acadex.data.supabase.RatingInsert
import com.example.acadex.data.supabase.SavedMaterialRow
import com.example.acadex.data.supabase.SupabaseClient
import com.example.acadex.util.FileTypeUtils
import com.example.acadex.util.MimeTypeUtils
import com.example.acadex.util.UserIdentity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MaterialRepository {

    private const val TAG = "MaterialRepository"
    private const val NETWORK = "No connection. Please check your internet."
    private const val SERVER = "Something went wrong. Please try again."

    suspend fun fetchAll(): RepoResult<List<ResourceFile>> = withContext(Dispatchers.IO) {
        runRepo {
            val rows = client().postgrest.from("materials").select {
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<MaterialRow>()
            rows.map { it.toResourceFile() }
        }
    }

    suspend fun fetchById(materialId: String): RepoResult<ResourceFile> = withContext(Dispatchers.IO) {
        runRepo {
            client().postgrest.from("materials").select {
                filter { eq("id", materialId) }
            }.decodeSingle<MaterialRow>().toResourceFile()
        }
    }

    suspend fun fetchByUploader(uid: String): RepoResult<List<ResourceFile>> = withContext(Dispatchers.IO) {
        runRepo {
            client().postgrest.from("materials").select {
                filter { eq("uploader_id", uid) }
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<MaterialRow>().map { it.toResourceFile() }
        }
    }

    suspend fun fetchUserStats(uid: String): RepoResult<UserMaterialStats> = withContext(Dispatchers.IO) {
        runRepo {
            val rows = client().postgrest.from("materials").select {
                filter { eq("uploader_id", uid) }
            }.decodeList<MaterialRow>()
            UserMaterialStats(
                uploads = rows.size,
                downloads = rows.sumOf { it.downloadCount },
                avgRating = if (rows.isEmpty()) 0f else rows.map { it.ratingAvg }.average().toFloat()
            )
        }
    }

    suspend fun upload(
        context: Context,
        uri: Uri,
        fileName: String,
        fileType: com.example.acadex.data.model.FileType,
        title: String,
        description: String,
        subject: String,
        tags: List<String>
    ): RepoResult<ResourceFile> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            return@withContext RepoResult.Error(SERVER)
        }
        val uid = UserIdentity.requireUid()
        val displayName = UserIdentity.displayName()
        val safeName = MimeTypeUtils.sanitizeFileName(fileName)
        val storagePath = "$uid/${System.currentTimeMillis()}_$safeName"

        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext RepoResult.Error(SERVER)

            client().storage.from(SupabaseClient.MATERIALS_BUCKET).upload(storagePath, bytes) {
                upsert = false
            }

            val inserted = try {
                client().postgrest.from("materials").insert(
                    MaterialInsert(
                        title = title,
                        description = description,
                        subject = subject,
                        fileType = MimeTypeUtils.storageFileType(fileType),
                        uploaderId = uid,
                        uploaderName = displayName,
                        storagePath = storagePath,
                        tags = tags
                    )
                ) {
                    select()
                }.decodeSingle<MaterialRow>()
            } catch (dbError: Exception) {
                Log.e(TAG, "DB insert failed after storage upload", dbError)
                runCatching {
                    client().storage.from(SupabaseClient.MATERIALS_BUCKET).delete(storagePath)
                }
                return@withContext RepoResult.Error(SERVER, dbError)
            }

            RepoResult.Success(inserted.toResourceFile())
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            RepoResult.Error(
                e.userMessage(NETWORK, SERVER),
                e
            )
        }
    }

    suspend fun deleteMaterial(material: ResourceFile): RepoResult<Unit> = withContext(Dispatchers.IO) {
        runRepo {
            val path = material.storagePath
            client().postgrest.from("materials").delete {
                filter { eq("id", material.id) }
            }
            if (!path.isNullOrBlank()) {
                runCatching {
                    client().storage.from(SupabaseClient.MATERIALS_BUCKET).delete(path)
                }
            }
            Unit
        }
    }

    suspend fun submitRating(materialId: String, stars: Float, userName: String): RepoResult<Unit> =
        withContext(Dispatchers.IO) {
            runRepo {
                val userId = UserIdentity.requireUid()
                client().postgrest.from("ratings").upsert(
                    RatingInsert(
                        materialId = materialId,
                        userId = userId,
                        userName = userName,
                        rating = stars
                    )
                )
                val ratings = client().postgrest.from("ratings").select {
                    filter { eq("material_id", materialId) }
                }.decodeList<com.example.acadex.data.supabase.RatingRow>()
                val count = ratings.size
                val avg = if (count == 0) 0f else ratings.sumOf { it.rating.toDouble() }.toFloat() / count
                client().postgrest.from("materials").update(
                    MaterialStatsUpdate(ratingAvg = avg, ratingCount = count)
                ) {
                    filter { eq("id", materialId) }
                }
                Unit
            }
        }

    suspend fun postComment(materialId: String, text: String, commenterName: String): RepoResult<Comment> =
        withContext(Dispatchers.IO) {
            runRepo {
                val row = client().postgrest.from("comments").insert(
                    CommentInsert(
                        materialId = materialId,
                        userId = UserIdentity.uidOrNull(),
                        commenterName = commenterName,
                        body = text
                    )
                ) {
                    select()
                }.decodeSingle<CommentRow>()
                row.toComment()
            }
        }

    suspend fun fetchComments(materialId: String): RepoResult<List<Comment>> = withContext(Dispatchers.IO) {
        runRepo {
            client().postgrest.from("comments").select {
                filter { eq("material_id", materialId) }
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<CommentRow>().map { it.toComment() }
        }
    }

    suspend fun recordDownload(material: ResourceFile): RepoResult<String?> = withContext(Dispatchers.IO) {
        runRepo {
            val newCount = material.downloadCount + 1
            client().postgrest.from("materials").update(DownloadCountUpdate(downloadCount = newCount)) {
                filter { eq("id", material.id) }
            }
            material.downloadUrl
        }
    }

    suspend fun isSaved(materialId: String, userId: String): RepoResult<Boolean> = withContext(Dispatchers.IO) {
        runRepo {
            client().postgrest.from("saved_materials").select {
                filter {
                    eq("user_id", userId)
                    eq("material_id", materialId)
                }
            }.decodeList<SavedMaterialRow>().isNotEmpty()
        }
    }

    private inline fun <T> runRepo(block: () -> T): RepoResult<T> {
        if (!SupabaseClient.isConfigured) return RepoResult.Error(SERVER)
        return try {
            RepoResult.Success(block())
        } catch (e: Exception) {
            Log.e(TAG, "Supabase error", e)
            RepoResult.Error(e.userMessage(NETWORK, SERVER), e)
        }
    }

    private fun client() = SupabaseClient.instance

    private fun MaterialRow.toResourceFile(
        comments: MutableList<Comment> = mutableListOf(),
        isSaved: Boolean = false
    ): ResourceFile {
        val url = storagePath?.let { path ->
            runCatching {
                client().storage.from(SupabaseClient.MATERIALS_BUCKET).publicUrl(path)
            }.getOrNull()
        }
        return ResourceFile(
            id = id,
            title = title,
            description = description,
            subject = subject,
            fileType = FileTypeUtils.fromString(fileType),
            uploaderName = uploaderName,
            uploadDate = formatDate(createdAt),
            rating = ratingAvg,
            ratingCount = ratingCount,
            downloadCount = downloadCount,
            comments = comments,
            isSaved = isSaved,
            remoteId = id,
            storagePath = storagePath,
            downloadUrl = url
        )
    }

    private fun CommentRow.toComment() = Comment(
        commenterName = commenterName,
        text = body,
        date = formatDate(createdAt),
        remoteId = id
    )

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val out = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val parsed = parser.parse(iso.take(19)) ?: return iso
            out.format(parsed)
        }.getOrDefault(iso)
    }

    data class UserMaterialStats(
        val uploads: Int,
        val downloads: Int,
        val avgRating: Float
    )
}
