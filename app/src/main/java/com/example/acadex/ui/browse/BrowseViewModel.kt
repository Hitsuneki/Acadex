package com.example.acadex.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.MockDataSource
import com.example.acadex.data.ResourceRepository
import com.example.acadex.data.model.ResourceFile
import kotlinx.coroutines.launch

class BrowseViewModel : ViewModel() {

    private val _files = MutableLiveData<List<ResourceFile>>()
    val files: LiveData<List<ResourceFile>> = _files

    private var subject = "All"
    private var query = ""
    private var sortBy = MockDataSource.SortOption.NEWEST

    init {
        refresh()
    }

    fun setSubject(s: String) {
        subject = s
        refresh()
    }

    fun setQuery(q: String) {
        query = q
        refresh()
    }

    fun setSort(sort: MockDataSource.SortOption) {
        sortBy = sort
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            ResourceRepository.refreshFromSupabase()
            _files.postValue(ResourceRepository.filterFiles(subject, query, sortBy))
        }
    }
}
