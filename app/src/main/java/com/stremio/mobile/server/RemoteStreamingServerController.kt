package com.stremio.mobile.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Uses a streaming server running elsewhere (e.g. Stremio on a desktop) instead of starting the on-device one. */
class RemoteStreamingServerController(private val baseUrl: String) : StreamingServerController {
    private val mutableState = MutableStateFlow<StreamingServerState>(StreamingServerState.Stopped)
    private val startMutex = Mutex()
    override val state: StateFlow<StreamingServerState> = mutableState

    override suspend fun start() {
        startMutex.withLock {
            if (mutableState.value !is StreamingServerState.Ready) {
                mutableState.value = StreamingServerState.Starting
            }
            mutableState.value = if (isStreamingServerReachable(baseUrl, timeoutMs = 3_000)) {
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
}
