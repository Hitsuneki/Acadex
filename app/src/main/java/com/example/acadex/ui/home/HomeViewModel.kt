package com.example.acadex.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acadex.data.MockDataSource
import com.example.acadex.data.model.ResourceFile

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
        _files.value = if (subject == "All") MockDataSource.files
        else MockDataSource.files.filter { it.subject == subject }
    }
}
