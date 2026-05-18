package com.example.acadex.ui.browse

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.acadex.adapter.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("acadex_prefs", Context.MODE_PRIVATE)
    
    private val _viewMode = MutableStateFlow(getViewModePref())
    val viewMode: StateFlow<ViewMode> = _viewMode

    private fun getViewModePref(): ViewMode {
        val name = prefs.getString("acadex_pref_view_mode", ViewMode.ROW.name) ?: ViewMode.ROW.name
        return try {
            ViewMode.valueOf(name)
        } catch (e: Exception) {
            ViewMode.ROW
        }
    }

    fun toggleViewMode() {
        val nextMode = when (_viewMode.value) {
            ViewMode.ROW -> ViewMode.TILE
            ViewMode.TILE -> ViewMode.COMPACT
            ViewMode.COMPACT -> ViewMode.ROW
        }
        prefs.edit().putString("acadex_pref_view_mode", nextMode.name).apply()
        _viewMode.value = nextMode
    }
}
