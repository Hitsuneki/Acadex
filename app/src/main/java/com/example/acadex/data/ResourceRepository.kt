package com.example.acadex.data

import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.repository.MaterialRepository
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.util.UserIdentity

/** Supabase-backed materials for home and browse. */
object ResourceRepository {

    private val remoteFiles = mutableListOf<ResourceFile>()

    fun getAllFiles(): List<ResourceFile> = remoteFiles.toList()

    fun getFileById(id: String): ResourceFile? = remoteFiles.find { it.id == id }

    fun filterFiles(
        subject: String,
        query: String,
        sort: SortOption
    ): List<ResourceFile> {
        var list = getAllFiles()
        if (subject != "All") list = list.filter { it.subject == subject }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                    it.uploaderName.lowercase().contains(q) ||
                    it.subject.lowercase().contains(q)
            }
        }
        return when (sort) {
            SortOption.NEWEST -> list.sortedByDescending { it.uploadDate }
            SortOption.MOST_DOWNLOADED -> list.sortedByDescending { it.downloadCount }
            SortOption.TOP_RATED -> list.sortedByDescending { it.rating }
        }
    }

    suspend fun refreshFromSupabase() {
        when (val result = MaterialRepository.fetchAll()) {
            is RepoResult.Success -> {
                val uid = UserIdentity.uidOrNull()
                val savedIds = if (uid != null) {
                    when (val saved = SavedRepository.fetchSavedMaterials()) {
                        is RepoResult.Success -> saved.data.map { it.id }.toSet()
                        else -> emptySet()
                    }
                } else emptySet()
                remoteFiles.clear()
                remoteFiles.addAll(
                    result.data.map { file ->
                        file.copy(isSaved = savedIds.contains(file.id))
                    }
                )
            }
            is RepoResult.Error -> Unit
        }
    }
}
