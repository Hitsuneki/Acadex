package com.example.acadex.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object RelativeTimeUtils {

    fun formatRelative(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val millis = parseIsoMillis(iso) ?: return iso
        val now = System.currentTimeMillis()
        val diff = now - millis
        if (diff < TimeUnit.MINUTES.toMillis(1)) return "Just now"
        if (diff < TimeUnit.HOURS.toMillis(1)) {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff).coerceAtLeast(1)
            return if (mins == 1L) "1 minute ago" else "$mins minutes ago"
        }
        if (diff < TimeUnit.DAYS.toMillis(1)) {
            val hours = TimeUnit.MILLISECONDS.toHours(diff).coerceAtLeast(1)
            return if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        if (diff < TimeUnit.DAYS.toMillis(2)) return "Yesterday"
        return formatDisplayDate(iso)
    }

    fun formatDisplayDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val out = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val parsed = parser.parse(iso.take(19)) ?: return iso
            out.format(parsed)
        }.getOrDefault(iso)
    }

    private fun parseIsoMillis(iso: String): Long? = runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        parser.parse(iso.take(19))?.time
    }.getOrNull()
}
