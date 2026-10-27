package com.stremio.mobile.player

import kotlin.math.abs

/**
 * Subtitle time scale presets. A rate is `subtitle fps / video fps`, so that
 * `video time = cue time * rate + delay`. It fixes subtitles that drift because they were
 * authored for a different frame rate than the video (e.g. 23.976 fps subs on a 25 fps video).
 */
object SubtitleRatePresets {
    data class Preset(val id: String, val label: String, val rate: Double)

    private const val FPS_23_976 = 24000.0 / 1001.0
    private const val FPS_29_97 = 30000.0 / 1001.0
    private const val EPSILON = 1e-6

    const val MIN_RATE = 0.5
    const val MAX_RATE = 2.0

    val SAME = Preset("off", "Same fps", 1.0)

    val all: List<Preset> = listOf(
        SAME,
        Preset("23976-25", "23.976 → 25 fps", FPS_23_976 / 25.0),
        Preset("25-23976", "25 → 23.976 fps", 25.0 / FPS_23_976),
        Preset("24-25", "24 → 25 fps", 24.0 / 25.0),
        Preset("25-24", "25 → 24 fps", 25.0 / 24.0),
        Preset("23976-24", "23.976 → 24 fps", FPS_23_976 / 24.0),
        Preset("24-23976", "24 → 23.976 fps", 24.0 / FPS_23_976),
        Preset("2997-25", "29.97 → 25 fps", FPS_29_97 / 25.0),
        Preset("25-2997", "25 → 29.97 fps", 25.0 / FPS_29_97),
        Preset("30-2997", "30 → 29.97 fps", 30.0 / FPS_29_97),
        Preset("2997-30", "29.97 → 30 fps", FPS_29_97 / 30.0),
    )

    /** Index of the preset matching [rate], or -1 for a custom value. */
    fun indexOf(rate: Double): Int = all.indexOfFirst { abs(it.rate - rate) < EPSILON }

    fun isValid(rate: Double): Boolean = rate.isFinite() && rate in MIN_RATE..MAX_RATE
}
