package com.example.acadex.data.repository

import android.util.Log
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.result.RepoResult
import com.example.acadex.data.result.userMessage
import com.example.acadex.data.supabase.MaterialRow
import com.example.acadex.data.supabase.SavedMaterialRow
import com.example.acadex.data.supabase.SupabaseClient
import com.example.acadex.util.FileTypeUtils
import com.example.acadex.util.UserIdentity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SavedRepository {

    private const val TAG = "SavedRepository"
    private const val NETWORK = "No connection. Please check your internet."
    private const val SERVER = "Something went wrong. Please try again."

    suspend fun fetchSavedMaterials(): RepoResult<List<ResourceFile>> = withContext(Dispatchers.IO) {
        val uid = UserIdentity.requireUid()
        runRepo {
            val saved = client().postgrest.from("saved_materials").select {
                filter { eq("user_id", uid) }
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<SavedMaterialRow>()

            if (saved.isEmpty()) return@runRepo emptyList()

            val ids = saved.map { it.materialId }
            val materials = client().postgrest.from("materials").select {
                filter { isIn("id", ids) }
            }.decodeList<MaterialRow>()

            val byId = materials.associateBy { it.id }
            saved.mapNotNull { s -> byId[s.materialId]?.toResourceFile(isSaved = true) }
        }
    }

    suspend fun save(materialId: String): RepoResult<Unit> = withContext(Dispatchers.IO) {
        val uid = UserIdentity.requireUid()
        runRepo {
            client().postgrest.from("saved_materials").insert(
                SavedMaterialRow(userId = uid, materialId = materialId)
            )
            Unit
        }
    }

    suspend fun unsave(materialId: String): RepoResult<Unit> = withContext(Dispatchers.IO) {
        val uid = UserIdentity.requireUid()
        runRepo {
            client().postgrest.from("saved_materials").delete {
                filter {
                    eq("user_id", uid)
                    eq("material_id", materialId)
                }
            }
            Unit
        }
    }

    suspend fun clearAll(): RepoResult<Unit> = withContext(Dispatchers.IO) {
        val uid = UserIdentity.requireUid()
        runRepo {
            client().postgrest.from("saved_materials").delete {
                filter { eq("user_id", uid) }
            }
            Unit
        }
    }

    private inline fun <T> runRepo(block: () -> T): RepoResult<T> {
        if (!SupabaseClient.isConfigured) return RepoResult.Error(SERVER)
        return try {
            RepoResult.Success(block())
        } catch (e: Exception) {
            Log.e(TAG, "Saved error", e)
            RepoResult.Error(e.userMessage(NETWORK, SERVER), e)
        }
    }

    private fun client() = SupabaseClient.instance

    private fun MaterialRow.toResourceFile(isSaved: Boolean): ResourceFile {
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
            isSaved = isSaved,
            remoteId = id,
            storagePath = storagePath,
            downloadUrl = url
        )
    }

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
}
