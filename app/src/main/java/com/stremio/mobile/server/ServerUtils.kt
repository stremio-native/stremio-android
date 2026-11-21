package com.stremio.mobile.server

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import timber.log.Timber

/** Needed on Android 17+ to reach LAN addresses; not for loopback or VPN (Tailscale). */
const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

fun hasLocalNetworkPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 37 || context.checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

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
    }.onFailure { Timber.d(it, "Streaming server probe failed") }.getOrDefault(false)
}

private val leadingZeroOctet = Regex("""(^|\.)0\d""")

/** [input] as an http(s) base URL in the form stremio-core stores it, or null if invalid. */
fun parseStreamingServerUrl(input: String): String? {
    val trimmed = input.trim()
    val withScheme = if ("://" in trimmed || trimmed.isEmpty()) trimmed else "http://$trimmed"
    val uri = runCatching { URI(withScheme).normalize() }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    val host = uri.host?.lowercase()
    if (scheme != "http" && scheme != "https" || host.isNullOrEmpty()) return null
    if (uri.rawQuery != null || uri.rawFragment != null || uri.rawUserInfo != null) return null
    // Forms stremio-core parses differently.
    val path = uri.rawPath.orEmpty()
    val isOctalIpv4 = host.all { it.isDigit() || it == '.' } && leadingZeroOctet.containsMatchIn(host)
    val hasDotSegment = path.startsWith("/..") || path.contains("%2e", ignoreCase = true)
    if (isOctalIpv4 || '%' in host || hasDotSegment) return null

    val port = uri.port
    if (port == 0 || port > 65535) return null
    val defaultPort = if (scheme == "http") 80 else 443
    val portPart = if (port == -1 || port == defaultPort) "" else ":$port"
    return "$scheme://$host$portPart${path.trimEnd('/')}"
}
