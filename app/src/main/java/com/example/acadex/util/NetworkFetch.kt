package com.example.acadex.util

import java.net.HttpURLConnection
import java.net.URL

object NetworkFetch {

    fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 120_000
        connection.setRequestProperty("User-Agent", "Acadex-Android")
        connection.setRequestProperty("Accept", "*/*")
        return connection
    }

    fun downloadBytes(url: String): ByteArray {
        val connection = openConnection(url)
        connection.requestMethod = "GET"
        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code for $url")
        }
        return connection.inputStream.use { it.readBytes() }
    }
}
