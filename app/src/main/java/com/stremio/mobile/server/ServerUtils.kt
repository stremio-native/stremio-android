package com.stremio.mobile.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

fun formatServerErrorMessage(message: String): String {
    return if (message.contains("failed to bind", ignoreCase = true) || message.contains("11470")) {
        "Port 11470 is already in use. Please close any other Stremio or streaming apps running in the background and try again."
    } else {
        message
    }
}

suspend fun isStreamingServerReachable(baseUrl: String, timeoutMs: Int): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL("$baseUrl/settings").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.setRequestProperty("Accept", "application/json")
        try {
            connection.responseCode in 200..499
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)
}

/** Returns [input] as an http(s) base URL without a trailing slash, or null if it isn't one. */
fun parseStreamingServerUrl(input: String): String? {
    val trimmed = input.trim().trimEnd('/')
    val url = runCatching { URL(trimmed) }.getOrNull() ?: return null
    return trimmed.takeIf { url.protocol in setOf("http", "https") && !url.host.isNullOrEmpty() }
}
