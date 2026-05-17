package com.example.acadex.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.ResourceRepository
import com.example.acadex.data.model.ResourceFile
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _files = MutableLiveData<List<ResourceFile>>()
    val files: LiveData<List<ResourceFile>> = _files
    private var subject = "All"

    init { refresh() }

    fun setSubject(s: String) {
        subject = s
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            ResourceRepository.refreshFromSupabase()
            val all = ResourceRepository.getAllFiles()
            _files.postValue(
                if (subject == "All") all else all.filter { it.subject == subject }
            )
        }
    }
}
