package com.stremio.mobile.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

fun formatServerErrorMessage(message: String): String {
    return if (message.contains("failed to bind", ignoreCase = true) || message.contains("11470")) {
        "Port 11470 is already in use. Please close any other Stremio or streaming apps running in the background and try again."
    } else {
        message
    }
}

suspend fun isStreamingServerReachable(
    baseUrl: String,
    connectTimeoutMs: Int,
    readTimeoutMs: Int = connectTimeoutMs,
    acceptedCodes: IntRange = 200..499,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL("$baseUrl/settings").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Accept", "application/json")
        try {
            connection.responseCode in acceptedCodes
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)
}

private val leadingZeroOctet = Regex("""(^|\.)0\d""")

/**
 * Returns [input] as a canonical http(s) base URL, or null if it isn't one: lowercase scheme and host, no default port,
 * no trailing slash, no query/fragment/user info. Canonical, because stremio-core re-parses the URL and stores it normalized.
 */
fun parseStreamingServerUrl(input: String): String? {
    val uri = runCatching { URI(input.trim()).normalize() }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    val host = uri.host?.lowercase()
    if (scheme != "http" && scheme != "https" || host.isNullOrEmpty()) return null
    if (uri.rawQuery != null || uri.rawFragment != null || uri.rawUserInfo != null) return null
    // Forms that stremio-core's URL parser reads differently: octal IPv4 octets, IPv6 zone ids, a leading "..".
    val isIpv4 = host.all { it.isDigit() || it == '.' }
    if (isIpv4 && leadingZeroOctet.containsMatchIn(host) || '%' in host || uri.rawPath.orEmpty().startsWith("/..")) return null

    val port = uri.port
    if (port == 0 || port > 65535) return null
    val defaultPort = if (scheme == "http") 80 else 443
    val portPart = if (port == -1 || port == defaultPort) "" else ":$port"
    return "$scheme://$host$portPart${uri.rawPath.orEmpty().trimEnd('/')}"
}
