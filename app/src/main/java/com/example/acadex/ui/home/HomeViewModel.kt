package com.example.acadex.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.ResourceRepository
import com.example.acadex.data.SubjectCatalog
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.model.toResourceFile
import com.example.acadex.data.repository.QuizRepository
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.repository.GutendexRepository
import com.example.acadex.data.result.RepoResult
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _files = MutableLiveData<List<ResourceFile>>()
    val files: LiveData<List<ResourceFile>> = _files

    private val _subjects = MutableLiveData<List<String>>(listOf("All"))
    val subjects: LiveData<List<String>> = _subjects

    private val _quizCount = MutableLiveData(0)
    val quizCount: LiveData<Int> = _quizCount

    private val _savedCount = MutableLiveData(0)
    val savedCount: LiveData<Int> = _savedCount

    private val _searchResults = MutableLiveData<List<ResourceFile>>()
    val searchResults: LiveData<List<ResourceFile>> = _searchResults

    private var subject = "All"

    init { refresh() }

    fun setSubject(s: String) {
        subject = s
        publishFiles()
    }

    fun refresh() {
        viewModelScope.launch {
            ResourceRepository.refreshFromSupabase()
            val all = ResourceRepository.getAllFiles()
            _subjects.postValue(SubjectCatalog.forMaterials(all))
            
            when (val quizzes = QuizRepository.fetchQuizSets()) {
                is RepoResult.Success -> _quizCount.postValue(quizzes.data.size)
                is RepoResult.Error -> _quizCount.postValue(0)
            }
            
            when (val saved = SavedRepository.fetchSavedMaterials()) {
                is RepoResult.Success -> _savedCount.postValue(saved.data.size)
                is RepoResult.Error -> _savedCount.postValue(0)
            }
            
            publishFiles()
        }
    }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchResults.postValue(emptyList())
            return
        }
        viewModelScope.launch {
            val localMatches = ResourceRepository.getAllFiles().filter {
                it.title.contains(query, ignoreCase = true) || it.subject.contains(query, ignoreCase = true)
            }
            
            val remoteResult = GutendexRepository.fetchBooks(query, null, 1)
            val remoteMatches = if (remoteResult is RepoResult.Success) {
                remoteResult.data.results.map { it.toResourceFile(isSaved = false) }
            } else {
                emptyList()
            }
            
            _searchResults.postValue(localMatches + remoteMatches)
        }
    }

    private fun publishFiles() {
        val all = ResourceRepository.getAllFiles()
        _files.postValue(if (subject == "All") all else all.filter { it.subject == subject })
    }

    fun materialCount(): Int = ResourceRepository.getAllFiles().size

    fun subjectCount(): Int = ResourceRepository.getAllFiles().map { it.subject }.distinct().size
}
