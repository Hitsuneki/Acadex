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
import com.example.acadex.data.supabase.RatingPatch
import com.example.acadex.data.supabase.SavedMaterialRow
import com.example.acadex.data.supabase.SupabaseClient
import com.example.acadex.util.FileTypeUtils
import com.example.acadex.util.MimeTypeUtils
import com.example.acadex.util.UserIdentity
import com.example.acadex.data.supabase.IncrementDownloadParams
import com.example.acadex.util.RelativeTimeUtils
import com.example.acadex.util.StorageUrlHelper
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import com.example.acadex.util.NetworkFetch

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

    suspend fun fetchUserRating(materialId: String, userId: String): RepoResult<Float?> =
        withContext(Dispatchers.IO) {
            runRepo {
                val rows = client().postgrest.from("ratings").select {
                    filter {
                        eq("material_id", materialId)
                        eq("user_id", userId)
                    }
                }.decodeList<com.example.acadex.data.supabase.RatingRow>()
                rows.firstOrNull()?.rating
            }
        }

    suspend fun submitRating(materialId: String, stars: Float, userName: String): RepoResult<ResourceFile> =
        withContext(Dispatchers.IO) {
            runRepo {
                val userId = UserIdentity.requireUid()
                val existing = client().postgrest.from("ratings").select {
                    filter {
                        eq("material_id", materialId)
                        eq("user_id", userId)
                    }
                }.decodeList<com.example.acadex.data.supabase.RatingRow>()

                if (existing.isNotEmpty()) {
                    client().postgrest.from("ratings").update(
                        RatingPatch(rating = stars, userName = userName)
                    ) {
                        filter { eq("id", existing.first().id) }
                    }
                } else {
                    client().postgrest.from("ratings").insert(
                        RatingInsert(
                            materialId = materialId,
                            userId = userId,
                            userName = userName,
                            rating = stars
                        )
                    )
                }
                recalculateMaterialRating(materialId)
                client().postgrest.from("materials").select {
                    filter { eq("id", materialId) }
                }.decodeSingle<MaterialRow>().toResourceFile()
            }
        }

    private suspend fun recalculateMaterialRating(materialId: String) {
        val ratings = client().postgrest.from("ratings").select {
            filter { eq("material_id", materialId) }
        }.decodeList<com.example.acadex.data.supabase.RatingRow>()
        val avg = if (ratings.isEmpty()) 0f else ratings.map { it.rating }.average().toFloat()
        val count = ratings.size
        client().postgrest.from("materials").update(
            MaterialStatsUpdate(ratingAvg = avg, ratingCount = count)
        ) {
            filter { eq("id", materialId) }
        }
    }

    suspend fun incrementDownloadCount(materialId: String): RepoResult<Unit> =
        withContext(Dispatchers.IO) {
            if (!SupabaseClient.isConfigured) return@withContext RepoResult.Error(SERVER)
            try {
                client().postgrest.rpc(
                    "increment_download_count",
                    IncrementDownloadParams(materialId = materialId)
                )
                RepoResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "increment_download_count RPC failed, falling back", e)
                try {
                    val row = client().postgrest.from("materials").select {
                        filter { eq("id", materialId) }
                    }.decodeSingle<MaterialRow>()
                    client().postgrest.from("materials").update(
                        DownloadCountUpdate(downloadCount = row.downloadCount + 1)
                    ) {
                        filter { eq("id", materialId) }
                    }
                    RepoResult.Success(Unit)
                } catch (fallback: Exception) {
                    Log.e(TAG, "increment download fallback failed", fallback)
                    RepoResult.Error(fallback.userMessage(NETWORK, SERVER), fallback)
                }
            }
        }

    /** Downloads via Storage API first (reliable), then public HTTP URL as fallback. */
    suspend fun downloadFile(storagePath: String?, publicUrl: String?): RepoResult<ByteArray> =
        withContext(Dispatchers.IO) {
            if (!SupabaseClient.isConfigured) return@withContext RepoResult.Error(SERVER)
            if (!storagePath.isNullOrBlank()) {
                try {
                    val bytes = client().storage
                        .from(SupabaseClient.MATERIALS_BUCKET)
                        .downloadAuthenticated(storagePath)
                    if (bytes.isNotEmpty()) return@withContext RepoResult.Success(bytes)
                } catch (e: Exception) {
                    Log.w(TAG, "Storage download failed: $storagePath", e)
                }
            }
            val url = publicUrl ?: storagePath?.let { StorageUrlHelper.publicUrl(it) }
            if (!url.isNullOrBlank()) {
                return@withContext downloadBytes(url)
            }
            RepoResult.Error(SERVER)
        }

    suspend fun downloadBytes(publicUrl: String): RepoResult<ByteArray> = withContext(Dispatchers.IO) {
        runRepo {
            NetworkFetch.downloadBytes(publicUrl)
        }
    }

    suspend fun fetchContentLength(publicUrl: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = NetworkFetch.openConnection(publicUrl).apply {
                requestMethod = "HEAD"
            }
            connection.connect()
            connection.contentLengthLong.takeIf { it > 0 }
        }.getOrNull()
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
                order(column = "created_at", order = Order.ASCENDING)
            }.decodeList<CommentRow>().map { it.toComment() }
        }
    }

    suspend fun deleteComment(commentId: String): RepoResult<Unit> = withContext(Dispatchers.IO) {
        runRepo {
            client().postgrest.from("comments").delete {
                filter { eq("id", commentId) }
            }
            Unit
        }
    }

    data class FileDetailBundle(
        val material: ResourceFile,
        val comments: List<Comment>,
        val commentsError: String?,
        val userRating: Float?,
        val isSaved: Boolean
    )

    suspend fun loadFileDetail(materialId: String, userId: String?): RepoResult<FileDetailBundle> =
        withContext(Dispatchers.IO) {
            if (!SupabaseClient.isConfigured) return@withContext RepoResult.Error(SERVER)
            try {
                coroutineScope {
                    val materialDeferred = async {
                        client().postgrest.from("materials").select {
                            filter { eq("id", materialId) }
                        }.decodeSingle<MaterialRow>()
                    }
                    val commentsDeferred = async {
                        client().postgrest.from("comments").select {
                            filter { eq("material_id", materialId) }
                            order(column = "created_at", order = Order.ASCENDING)
                        }.decodeList<CommentRow>()
                    }
                    val ratingDeferred = async {
                        if (userId == null) null
                        else {
                            client().postgrest.from("ratings").select {
                                filter {
                                    eq("material_id", materialId)
                                    eq("user_id", userId)
                                }
                            }.decodeList<com.example.acadex.data.supabase.RatingRow>().firstOrNull()?.rating
                        }
                    }
                    val savedDeferred = async {
                        if (userId == null) false
                        else {
                            client().postgrest.from("saved_materials").select {
                                filter {
                                    eq("user_id", userId)
                                    eq("material_id", materialId)
                                }
                            }.decodeList<SavedMaterialRow>().isNotEmpty()
                        }
                    }

                    val row = materialDeferred.await()
                    val isSaved = savedDeferred.await()
                    val commentsResult = runCatching {
                        commentsDeferred.await().map { it.toComment() }
                    }
                    val comments = commentsResult.getOrDefault(emptyList())
                    val commentsError = commentsResult.exceptionOrNull()?.userMessage(NETWORK, SERVER)
                    val material = row.toResourceFile(
                        comments = comments.toMutableList(),
                        isSaved = isSaved
                    )
                    RepoResult.Success(
                        FileDetailBundle(
                            material = material,
                            comments = comments,
                            commentsError = commentsError,
                            userRating = ratingDeferred.await(),
                            isSaved = isSaved
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadFileDetail failed", e)
                RepoResult.Error(e.userMessage(NETWORK, SERVER), e)
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
        val url = StorageUrlHelper.publicUrl(storagePath)
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
        id = id,
        userId = userId,
        commenterName = commenterName,
        text = body,
        createdAtIso = createdAt.orEmpty(),
        displayDate = RelativeTimeUtils.formatDisplayDate(createdAt),
        relativeTime = RelativeTimeUtils.formatRelative(createdAt)
    )

    private fun formatDate(iso: String?): String = RelativeTimeUtils.formatDisplayDate(iso)

    data class UserMaterialStats(
        val uploads: Int,
        val downloads: Int,
        val avgRating: Float
    )
}
