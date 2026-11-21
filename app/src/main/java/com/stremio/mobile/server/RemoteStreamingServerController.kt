package com.stremio.mobile.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Uses a streaming server running elsewhere (e.g. Stremio on a desktop) instead of starting the on-device one. */
class RemoteStreamingServerController(private val baseUrl: String) : StreamingServerController {
    private val mutableState = MutableStateFlow<StreamingServerState>(StreamingServerState.Stopped)
    private val startMutex = Mutex()
    private var lastReachableMs = 0L
    override val state: StateFlow<StreamingServerState> = mutableState

    // Called before every stream list load and server-backed playback, so a recent successful probe is reused.
    override suspend fun start() {
        startMutex.withLock {
            val isReady = mutableState.value is StreamingServerState.Ready
            if (isReady && System.currentTimeMillis() - lastReachableMs < RECHECK_INTERVAL_MS) {
                return
            }
            if (!isReady) {
                mutableState.value = StreamingServerState.Starting
            }

            val reachable = isStreamingServerReachable(
                baseUrl,
                connectTimeoutMs = 1_500,
                readTimeoutMs = 3_000,
                acceptedCodes = 200..200,
            )
            mutableState.value = if (reachable) {
                lastReachableMs = System.currentTimeMillis()
                StreamingServerState.Ready(baseUrl)
            } else {
                // No URL in the message: formatServerErrorMessage treats any "11470" as a local port conflict.
                StreamingServerState.Failed("Remote streaming server is not reachable")
            }
        }
    }

    override suspend fun stop() {
        mutableState.value = StreamingServerState.Stopped
    }

    private companion object {
        const val RECHECK_INTERVAL_MS = 30_000L
    }
}
