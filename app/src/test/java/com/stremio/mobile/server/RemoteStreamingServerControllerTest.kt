package com.stremio.mobile.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RemoteStreamingServerControllerTest {
    private val url = "http://192.168.1.10:11470"
    private var nowMs = 1_000_000L
    private val probeResults = ArrayDeque<Boolean>()
    private var probeCount = 0

    private fun controller(probe: suspend (String) -> Boolean = { probeCount++; probeResults.removeFirst() }) =
        RemoteStreamingServerController(url, probe, clock = { nowMs })

    @Test
    fun `becomes ready when the server answers`() = runBlocking {
        probeResults += true
        val controller = controller()
        controller.start()
        assertEquals(StreamingServerState.Ready(url), controller.state.value)
    }

    @Test
    fun `fails without retry when the server was never reached`() = runBlocking {
        probeResults += false
        val controller = controller()
        controller.start()
        assertTrue(controller.state.value is StreamingServerState.Failed)
        assertEquals(1, probeCount)
    }

    @Test
    fun `reuses a recent successful probe`() = runBlocking {
        probeResults += true
        val controller = controller()
        controller.start()
        nowMs += RemoteStreamingServerController.RECHECK_INTERVAL_MS - 1
        controller.start()
        assertEquals(1, probeCount)
    }

    @Test
    fun `re-probes after the interval and retries once if it was ready`() = runBlocking {
        probeResults += listOf(true, false, true)
        val controller = controller()
        controller.start()
        nowMs += RemoteStreamingServerController.RECHECK_INTERVAL_MS
        controller.start()
        assertEquals(3, probeCount)
        assertEquals(StreamingServerState.Ready(url), controller.state.value)
    }

    @Test
    fun `fails when the retry also misses`() = runBlocking {
        probeResults += listOf(true, false, false)
        val controller = controller()
        controller.start()
        nowMs += RemoteStreamingServerController.RECHECK_INTERVAL_MS
        controller.start()
        assertTrue(controller.state.value is StreamingServerState.Failed)
    }

    @Test
    fun `restores the previous state when cancelled during the probe`() = runBlocking {
        val controller = controller(probe = { throw CancellationException("cancelled") })
        try {
            controller.start()
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals(StreamingServerState.Stopped, controller.state.value)
        }
    }
}
