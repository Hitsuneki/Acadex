package com.example.acadex.data.result

sealed class RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : RepoResult<Nothing>()
}

fun <T> RepoResult<T>.getOrNull(): T? = (this as? RepoResult.Success)?.data

fun Throwable.userMessage(networkMessage: String, serverMessage: String): String {
    val msg = message?.lowercase().orEmpty()
    return when {
        msg.contains("unable to resolve host") ||
            msg.contains("failed to connect") ||
            msg.contains("timeout") ||
            msg.contains("network") -> networkMessage
        else -> serverMessage
    }
}
