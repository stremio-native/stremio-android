package com.stremio.mobile.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Uses a streaming server running elsewhere (e.g. Stremio on a desktop) instead of starting the on-device one. */
class RemoteStreamingServerController(
    private val baseUrl: String,
    private val probe: suspend (baseUrl: String) -> Boolean = ::probeRemoteServer,
    private val clock: () -> Long = System::currentTimeMillis,
) : StreamingServerController {
    private val mutableState = MutableStateFlow<StreamingServerState>(StreamingServerState.Stopped)
    private val startMutex = Mutex()
    private var lastReachableMs = 0L
    override val state: StateFlow<StreamingServerState> = mutableState

    // Called before every stream list load and server-backed playback, so a recent successful probe is reused.
    override suspend fun start() {
        startMutex.withLock {
            val previousState = mutableState.value
            val wasReady = previousState is StreamingServerState.Ready
            if (wasReady && clock() - lastReachableMs < RECHECK_INTERVAL_MS) {
                return
            }
            if (!wasReady) {
                mutableState.value = StreamingServerState.Starting
            }

            val reachable = try {
                // A server that was up gets a second try: the first request after idle may hit Wi-Fi power save or a Tailscale re-handshake.
                probe(baseUrl) || wasReady && run { delay(RETRY_DELAY_MS); probe(baseUrl) }
            } catch (e: CancellationException) {
                mutableState.value = previousState
                throw e
            }
            mutableState.value = if (reachable) {
                lastReachableMs = clock()
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

    internal companion object {
        const val RECHECK_INTERVAL_MS = 30_000L
        const val RETRY_DELAY_MS = 500L
    }
}

/** The remote server must answer `/settings` properly; any HTTP reply would also match e.g. a router page. */
private suspend fun probeRemoteServer(baseUrl: String): Boolean =
    isStreamingServerReachable(baseUrl, connectTimeoutMs = 1_500, readTimeoutMs = 3_000, acceptedCodes = 200..299)
