package com.example.acadex.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DownloadHelper {

    private const val TAG = "DownloadHelper"

    fun enqueueDownload(context: Context, url: String, title: String, fileName: String): Long? {
        return runCatching {
            val safeName = MimeTypeUtils.sanitizeFileName(fileName)
            val uniqueName = "${System.currentTimeMillis()}_$safeName"
            val ext = safeName.substringAfterLast('.', "")
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title)
                setDescription(safeName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uniqueName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                mime?.let { setMimeType(it) }
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }.onFailure { Log.e(TAG, "DownloadManager enqueue failed", it) }
            .getOrNull()
    }

    suspend fun saveBytesToDownloads(context: Context, bytes: ByteArray, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                if (bytes.isEmpty()) return@runCatching false
                val safeName = MimeTypeUtils.sanitizeFileName(fileName)
                val ext = safeName.substringAfterLast('.', "")
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
                    ?: "application/octet-stream"
                writeToDownloads(context, bytes, safeName, mime)
            }.onFailure { Log.e(TAG, "saveBytesToDownloads failed", it) }
                .getOrDefault(false)
        }

    suspend fun saveToDownloads(context: Context, url: String, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeName = MimeTypeUtils.sanitizeFileName(fileName)
                val ext = safeName.substringAfterLast('.', "")
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
                    ?: "application/octet-stream"
                val bytes = NetworkFetch.downloadBytes(url)
                if (bytes.isEmpty()) return@runCatching false
                writeToDownloads(context, bytes, safeName, mime)
            }.onFailure { Log.e(TAG, "MediaStore download failed", it) }
                .getOrDefault(false)
        }

    private fun writeToDownloads(context: Context, bytes: ByteArray, safeName: String, mime: String): Boolean =
        runCatching {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching false
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                java.io.File(dir, safeName).writeBytes(bytes)
            }
            true
        }.getOrDefault(false)
}
