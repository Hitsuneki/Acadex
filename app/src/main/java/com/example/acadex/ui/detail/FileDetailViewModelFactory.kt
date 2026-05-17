package com.example.acadex.ui.detail

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.acadex.ui.saved.SavedIndexSharedViewModel

class FileDetailViewModelFactory(
    private val application: Application,
    private val savedShared: SavedIndexSharedViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileDetailViewModel::class.java)) {
            return FileDetailViewModel(application, savedShared) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
