package com.example.acadex.ui.saved

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Nav-graph scoped save state shared between File Detail and Saved Index. */
class SavedIndexSharedViewModel : ViewModel() {

    private val _savedOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val savedOverrides: StateFlow<Map<String, Boolean>> = _savedOverrides.asStateFlow()

    fun setSaved(materialId: String, saved: Boolean) {
        _savedOverrides.update { it + (materialId to saved) }
    }

    fun resolveSaved(materialId: String, fromServer: Boolean): Boolean =
        _savedOverrides.value[materialId] ?: fromServer
}
