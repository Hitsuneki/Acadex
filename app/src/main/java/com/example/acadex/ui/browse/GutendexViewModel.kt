package com.example.acadex.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.model.GutendexBook
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.model.toResourceFile
import com.example.acadex.data.repository.GutendexRepository
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.result.RepoResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GutendexUiState {
    object Loading : GutendexUiState()
    data class Success(
        val books: List<ResourceFile>,
        val hasMore: Boolean,
        val isPaginating: Boolean = false
    ) : GutendexUiState()
    data class Error(val message: String) : GutendexUiState()
}

class GutendexViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GutendexUiState>(GutendexUiState.Loading)
    val uiState: StateFlow<GutendexUiState> = _uiState

    private var currentSearch: String? = null
    private var currentTopic: String? = null
    private var currentPage = 1
    private val loadedBooks = mutableListOf<GutendexBook>()
    private var hasNextPage = true

    init {
        loadBooks(reset = true)
    }

    fun setSearch(query: String) {
        currentSearch = query.ifBlank { null }
        loadBooks(reset = true)
    }

    fun setTopic(topic: String) {
        currentTopic = if (topic == "All" || topic.isBlank()) null else topic
        loadBooks(reset = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state is GutendexUiState.Success && !state.isPaginating && hasNextPage) {
            _uiState.value = state.copy(isPaginating = true)
            currentPage++
            loadBooks(reset = false)
        }
    }

    fun loadBooks(reset: Boolean) {
        if (reset) {
            currentPage = 1
            loadedBooks.clear()
            hasNextPage = true
            _uiState.value = GutendexUiState.Loading
        }

        viewModelScope.launch {
            val result = GutendexRepository.fetchBooks(
                search = currentSearch,
                topic = currentTopic,
                page = currentPage
            )

            when (result) {
                is RepoResult.Success -> {
                    val response = result.data
                    hasNextPage = response.next != null
                    loadedBooks.addAll(response.results)

                    val savedIds = fetchSavedIds()
                    val resourceFiles = loadedBooks.map { book ->
                        book.toResourceFile(isSaved = savedIds.contains(book.id.toString()))
                    }

                    _uiState.value = GutendexUiState.Success(
                        books = resourceFiles,
                        hasMore = hasNextPage,
                        isPaginating = false
                    )
                }
                is RepoResult.Error -> {
                    if (reset) {
                        _uiState.value = GutendexUiState.Error(result.message)
                    } else {
                        val savedIds = fetchSavedIds()
                        val resourceFiles = loadedBooks.map { book ->
                            book.toResourceFile(isSaved = savedIds.contains(book.id.toString()))
                        }
                        _uiState.value = GutendexUiState.Success(
                            books = resourceFiles,
                            hasMore = hasNextPage,
                            isPaginating = false
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchSavedIds(): Set<String> {
        return when (val saved = SavedRepository.fetchSavedMaterials()) {
            is RepoResult.Success -> saved.data.mapNotNull { it.remoteId }.toSet()
            else -> emptySet()
        }
    }
}
