package com.example.acadex.data

import com.example.acadex.data.model.Comment
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.repository.MaterialRepository
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.util.UserIdentity

/** Merges local mock seed data with Supabase materials for browse/home. */
object ResourceRepository {

    private val remoteFiles = mutableListOf<ResourceFile>()

    fun getAllFiles(): List<ResourceFile> {
        val mock = MockDataSource.files.toList()
        val remote = remoteFiles.toList()
        return (mock + remote).distinctBy { fileKey(it) }
    }

    fun getFileById(id: String): ResourceFile? =
        MockDataSource.getFileById(id) ?: remoteFiles.find { it.id == id }

    fun getSavedFiles(): List<ResourceFile> = getAllFiles().filter { it.isSaved }

    fun filterFiles(
        subject: String,
        query: String,
        sort: MockDataSource.SortOption
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
            MockDataSource.SortOption.NEWEST -> list.sortedByDescending { it.uploadDate }
            MockDataSource.SortOption.MOST_DOWNLOADED -> list.sortedByDescending { it.downloadCount }
            MockDataSource.SortOption.TOP_RATED -> list.sortedByDescending { it.rating }
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

    suspend fun submitRating(file: ResourceFile, stars: Float): Result<Unit> {
        if (file.isRemote) {
            return when (val r = MaterialRepository.submitRating(file.id, stars, UserIdentity.displayName())) {
                is RepoResult.Success -> Result.success(Unit)
                is RepoResult.Error -> Result.failure(Exception(r.message))
            }
        }
        applyLocalRating(file, stars)
        return Result.success(Unit)
    }

    suspend fun postComment(file: ResourceFile, text: String): Result<Comment> {
        if (file.isRemote) {
            return when (val r = MaterialRepository.postComment(file.id, text, UserIdentity.displayName())) {
                is RepoResult.Success -> {
                    file.comments.add(0, r.data)
                    Result.success(r.data)
                }
                is RepoResult.Error -> Result.failure(Exception(r.message))
            }
        }
        val comment = Comment(UserIdentity.displayName(), text, java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(java.util.Date()))
        file.comments.add(0, comment)
        return Result.success(comment)
    }

    suspend fun toggleSaved(file: ResourceFile): Result<Boolean> {
        if (!file.isRemote) {
            file.isSaved = !file.isSaved
            return Result.success(file.isSaved)
        }
        return if (file.isSaved) {
            when (val r = SavedRepository.unsave(file.id)) {
                is RepoResult.Success -> {
                    file.isSaved = false
                    Result.success(false)
                }
                is RepoResult.Error -> Result.failure(Exception(r.message))
            }
        } else {
            when (val r = SavedRepository.save(file.id)) {
                is RepoResult.Success -> {
                    file.isSaved = true
                    Result.success(true)
                }
                is RepoResult.Error -> Result.failure(Exception(r.message))
            }
        }
    }

    suspend fun recordDownload(file: ResourceFile): Result<String?> {
        if (!file.isRemote) return Result.success(file.downloadUrl)
        return when (val r = MaterialRepository.recordDownload(file)) {
            is RepoResult.Success -> {
                file.downloadCount += 1
                Result.success(r.data)
            }
            is RepoResult.Error -> Result.failure(Exception(r.message))
        }
    }

    suspend fun loadRemoteDetail(materialId: String): ResourceFile? {
        return when (val result = MaterialRepository.fetchById(materialId)) {
            is RepoResult.Success -> {
                val comments = when (val c = MaterialRepository.fetchComments(materialId)) {
                    is RepoResult.Success -> c.data.toMutableList()
                    else -> mutableListOf()
                }
                val uid = UserIdentity.uidOrNull()
                val saved = if (uid != null) {
                    when (val s = MaterialRepository.isSaved(materialId, uid)) {
                        is RepoResult.Success -> s.data
                        else -> false
                    }
                } else false
                val file = result.data.copy(comments = comments, isSaved = saved)
                remoteFiles.removeAll { it.id == materialId }
                remoteFiles.add(file)
                file
            }
            is RepoResult.Error -> getFileById(materialId)
        }
    }

    private fun applyLocalRating(file: ResourceFile, stars: Float) {
        val newCount = file.ratingCount + 1
        file.rating = ((file.rating * file.ratingCount) + stars) / newCount
        file.ratingCount = newCount
    }

    private fun fileKey(file: ResourceFile): String =
        if (file.isRemote) "remote_${file.id}" else "mock_${file.id}"
}
