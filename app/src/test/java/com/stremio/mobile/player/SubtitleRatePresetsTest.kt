package com.stremio.mobile.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SubtitleRatePresetsTest {
    @Test
    fun presetsAreUniqueAndValid() {
        assertEquals(SubtitleRatePresets.all.size, SubtitleRatePresets.all.map { it.id }.toSet().size)
        assertTrue(SubtitleRatePresets.all.all { SubtitleRatePresets.isValid(it.rate) })
    }

    @Test
    fun sameFpsIsIdentity() {
        assertEquals(0, SubtitleRatePresets.indexOf(1.0))
        assertEquals(1.0, SubtitleRatePresets.SAME.rate, 0.0)
    }

    @Test
    fun filmToPalRateMatchesSpeedUp() {
        // 23.976 -> 25 fps shortens the runtime by ~4.1%, so cue times scale by ~0.959.
        val rate = SubtitleRatePresets.all.first { it.id == "23976-25" }.rate
        assertTrue(abs(rate - 0.95904) < 1e-4)
        // A cue 100 minutes into the subtitle file lands ~4.1 minutes earlier in the video.
        val cueMs = 100 * 60 * 1000.0
        assertTrue(abs(cueMs * rate - 5754240.0) < 100.0)
    }

    @Test
    fun inverseRatesMultiplyToOne() {
        val a = SubtitleRatePresets.all.first { it.id == "23976-25" }.rate
        val b = SubtitleRatePresets.all.first { it.id == "25-23976" }.rate
        assertEquals(1.0, a * b, 1e-9)
    }

    @Test
    fun customRateIsNotAPreset() {
        assertEquals(-1, SubtitleRatePresets.indexOf(0.97))
    }

    @Test
    fun rejectsOutOfRangeRates() {
        assertFalse(SubtitleRatePresets.isValid(0.0))
        assertFalse(SubtitleRatePresets.isValid(3.0))
        assertFalse(SubtitleRatePresets.isValid(Double.NaN))
    }
}
