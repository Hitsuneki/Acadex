package com.example.acadex.util

import com.example.acadex.BuildConfig
import com.example.acadex.data.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage

object StorageUrlHelper {

    /**
     * Public URL for sharing / WebView. Uses the Supabase SDK so path encoding matches storage.
     */
    fun publicUrl(storagePath: String?): String? {
        if (storagePath.isNullOrBlank() || !SupabaseClient.isConfigured) return null
        return runCatching {
            SupabaseClient.instance.storage
                .from(SupabaseClient.MATERIALS_BUCKET)
                .publicUrl(storagePath)
        }.getOrNull() ?: buildPublicUrl(storagePath)
    }

    private fun buildPublicUrl(storagePath: String): String {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val path = storagePath.split('/').joinToString("/") { encodePathSegment(it) }
        return "$base/storage/v1/object/public/${SupabaseClient.MATERIALS_BUCKET}/$path"
    }

    /** RFC 3986 path segment encoding — keeps . _ - unencoded (URLEncoder breaks Supabase paths). */
    private fun encodePathSegment(segment: String): String {
        val allowed = "-._~"
        return buildString {
            for (ch in segment) {
                when {
                    ch.isLetterOrDigit() || ch in allowed -> append(ch)
                    else -> append("%${"%02X".format(ch.code)}")
                }
            }
        }
    }

    fun googleDocsViewerUrl(publicFileUrl: String): String {
        val encoded = java.net.URLEncoder.encode(publicFileUrl, Charsets.UTF_8.name())
        return "https://docs.google.com/viewer?url=$encoded"
    }

    fun fileNameFromPath(storagePath: String?): String {
        if (storagePath.isNullOrBlank()) return "file"
        return storagePath.substringAfterLast('/')
    }

    fun formatFileSize(bytes: Long?): String {
        if (bytes == null || bytes < 0) return ""
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.1f GB".format(mb / 1024.0)
    }
}
