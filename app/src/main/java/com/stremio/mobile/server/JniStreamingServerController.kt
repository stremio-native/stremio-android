package com.stremio.mobile.server

import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class JniStreamingServerController(
    private val context: Context,
    private val useForegroundService: () -> Boolean
) : StreamingServerController {
    private val mutableState = MutableStateFlow<StreamingServerState>(StreamingServerState.Stopped)
    private val startMutex = Mutex()
    override val state: StateFlow<StreamingServerState> = mutableState

    companion object {
        private const val TAG = "JniStreamingServer"

        init {
            System.loadLibrary("stream_server")
        }

        @JvmStatic
        private external fun startServerNative(context: Context, configDir: String, cacheDir: String, port: Int): String?

        @JvmStatic
        private external fun stopServerNative()

        @JvmStatic
        private external fun getServerUrlNative(): String?
    }

    override suspend fun start() {
        startMutex.withLock {
            val current = mutableState.value
            if (current is StreamingServerState.Ready) {
                if (waitForServerReady(current.baseUrl, timeoutMs = 750)) {
                    return
                }
                Timber.tag(TAG).w("State was Ready but %s is not reachable; restarting native server", current.baseUrl)
                dumpServerLogsToLogcat("ready-state unreachable")
                stopNativeServer()
            }

            mutableState.value = StreamingServerState.Starting
            val useForeground = useForegroundService()
            val serviceIntent = Intent(context, ServerService::class.java).apply {
                putExtra(ServerService.EXTRA_FOREGROUND, useForeground)
            }
            try {
                val url = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && useForeground) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

                    val configDir = File(context.filesDir, "stream-server").absolutePath
                    val cacheDir = File(context.cacheDir, "stream-server").absolutePath
                    startServerNative(context.applicationContext, configDir, cacheDir, 11470)
                }

                if (url != null && waitForServerReady(url)) {
                    Timber.tag(TAG).i("Streaming server ready at %s", url)
                    mutableState.value = StreamingServerState.Ready(url)
                } else {
                    val message = if (url == null) {
                        "Native start returned null"
                    } else {
                        "Native server returned $url but did not become reachable"
                    }
                    Timber.tag(TAG).e(message)
                    dumpServerLogsToLogcat(message)
                    stopNativeServer()
                    mutableState.value = StreamingServerState.Failed(message)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error starting native server")
                dumpServerLogsToLogcat("start exception")
                mutableState.value = StreamingServerState.Failed(e.message ?: "Unknown native error")
                stopNativeServer()
            }
        }
    }

    override suspend fun stop() {
        stopNativeServer()
        mutableState.value = StreamingServerState.Stopped
    }

    private suspend fun stopNativeServer() {
        withContext(Dispatchers.IO) {
            runCatching { stopServerNative() }
            try {
                context.stopService(Intent(context, ServerService::class.java))
            } catch (ignored: Exception) {}
        }
    }

    private suspend fun waitForServerReady(baseUrl: String, timeoutMs: Long = 5_000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isStreamingServerReachable(baseUrl, connectTimeoutMs = 500)) {
                return true
            }
            delay(150)
        }
        return false
    }

    private fun dumpServerLogsToLogcat(reason: String) {
        val logDir = File(context.filesDir, "stream-server/logs")
        Timber.tag(TAG).e("Checking server logs after %s: %s", reason, logDir.absolutePath)
        if (!logDir.exists()) {
            Timber.tag(TAG).e("Server log directory does not exist")
            return
        }

        val candidates = logDir.listFiles()
            ?.filter { it.isFile && (it.name == "server_current.log" || it.extension == "log" || it.extension == "jsonl") }
            ?.sortedByDescending { it.lastModified() }
            ?.let { files ->
                val current = files.firstOrNull { it.name == "server_current.log" }
                listOfNotNull(current) + files.filter { it != current }.take(1)
            }
            .orEmpty()

        if (candidates.isEmpty()) {
            Timber.tag(TAG).e("No server log files found in %s", logDir.absolutePath)
            return
        }

        candidates.forEach { file ->
            Timber.tag(TAG).e("---- %s (%d bytes) ----", file.name, file.length())
            runCatching {
                file.readLines().takeLast(120).forEach { line ->
                    Timber.tag(TAG).e(line.take(3500))
                }
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed to read %s", file.absolutePath)
            }
        }
    }
}
