package com.example.classhub.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.classhub.data.MockDataSource
import com.example.classhub.data.models.ResourceFile

class HomeViewModel : ViewModel() {

    private val _files = MutableLiveData<List<ResourceFile>>()
    val files: LiveData<List<ResourceFile>> = _files

    private var selectedSubject = "All"

    init {
        refresh()
    }

    fun setSubject(subject: String) {
        selectedSubject = subject
        refresh()
    }

    fun refresh() {
        val filtered = if (selectedSubject == "All") {
            MockDataSource.files.toList()
        } else {
            MockDataSource.files.filter { it.subject == selectedSubject }
        }
        _files.value = filtered
    }
}
