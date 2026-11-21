package com.stremio.mobile.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A streaming server on another device, e.g. Stremio on a PC. */
class RemoteStreamingServerController(
    private val baseUrl: String,
    private val probe: suspend (baseUrl: String) -> Boolean = ::probeRemoteServer,
    private val clock: () -> Long = System::currentTimeMillis,
    private val hasLocalNetworkPermission: () -> Boolean = { true },
) : StreamingServerController {
    private val mutableState = MutableStateFlow<StreamingServerState>(StreamingServerState.Stopped)
    private val startMutex = Mutex()
    private var lastReachableMs = 0L
    override val state: StateFlow<StreamingServerState> = mutableState

    // Runs before every stream list load, so a recent success is reused.
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
                var reachable = probe(baseUrl)
                // Retry if it was up: the first request after idle may hit Wi-Fi power save.
                if (!reachable && wasReady) {
                    delay(RETRY_DELAY_MS)
                    reachable = probe(baseUrl)
                }
                reachable
            } catch (e: CancellationException) {
                mutableState.value = previousState
                throw e
            }
            mutableState.value = if (reachable) {
                lastReachableMs = clock()
                StreamingServerState.Ready(baseUrl)
            } else {
                // No URL: formatServerErrorMessage reads "11470" as a port conflict.
                StreamingServerState.Failed(
                    if (hasLocalNetworkPermission()) "Remote streaming server is not reachable"
                    else "Remote streaming server is not reachable. For a server on your Wi-Fi, allow Nearby devices for this app."
                )
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

/** 2xx only: any HTTP reply would also match e.g. a router page. */
private suspend fun probeRemoteServer(baseUrl: String): Boolean =
    isStreamingServerReachable(baseUrl, connectTimeoutMs = 1_500, readTimeoutMs = 3_000, acceptedCodes = 200..299)
