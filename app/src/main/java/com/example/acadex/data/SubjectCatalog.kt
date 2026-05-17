package com.example.acadex.data

import com.example.acadex.data.model.ResourceFile

object SubjectCatalog {

    private val defaults = listOf("All", "Math", "Science", "English", "Filipino", "History", "CS")

    fun forMaterials(files: List<ResourceFile>): List<String> {
        val fromFiles = files.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
        if (fromFiles.isEmpty()) return defaults
        return listOf("All") + fromFiles
    }
}
