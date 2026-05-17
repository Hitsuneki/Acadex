package com.example.acadex.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IncrementDownloadParams(
    @SerialName("material_id") val materialId: String
)
