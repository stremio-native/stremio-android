package com.stremio.mobile.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleCueParserTest {
    private val srt = """
        1
        00:00:01,000 --> 00:00:03,500
        <i>Hello</i> there

        2
        00:00:04,000 --> 00:00:06,000
        {\an8}Second
        line

        3
        01:00:00,000 --> 01:00:02,000
        Late cue
    """.trimIndent()

    @Test
    fun parsesSrtCuesAndStripsTags() {
        val cues = SubtitleCueParser.parse(srt)
        assertEquals(3, cues.size)
        assertEquals(SubtitleCue(1000, 3500, "Hello there"), cues[0])
        assertEquals("Second\nline", cues[1].text)
        assertEquals(3_600_000L, cues[2].startMs)
    }

    @Test
    fun parsesWebVttWithHeaderAndCueSettings() {
        val vtt = """
            WEBVTT

            NOTE a comment

            00:01.000 --> 00:02.500 align:start position:10%
            Hi

            intro
            00:00:03.000 --> 00:00:04.000
            Named cue
        """.trimIndent()
        val cues = SubtitleCueParser.parse(vtt)
        assertEquals(listOf(SubtitleCue(1000, 2500, "Hi"), SubtitleCue(3000, 4000, "Named cue")), cues)
    }

    @Test
    fun skipsMalformedAndEmptyBlocks() {
        val bad = "1\nnot a time\ntext\n\n2\n00:00:05,000 --> 00:00:04,000\nbackwards\n\n3\n00:00:01,000 --> 00:00:02,000\n"
        assertTrue(SubtitleCueParser.parse(bad).isEmpty())
    }

    @Test
    fun activeCuesWithoutDelayOrRate() {
        val cues = SubtitleCueParser.parse(srt)
        assertEquals(listOf("Hello there"), SubtitleCueParser.activeCues(cues, 2000, 0, 1.0).map { it.text })
        assertTrue(SubtitleCueParser.activeCues(cues, 500, 0, 1.0).isEmpty())
        assertTrue(SubtitleCueParser.activeCues(cues, 3500, 0, 1.0).isEmpty()) // end is exclusive
    }

    @Test
    fun delayShiftsCuesLater() {
        val cues = SubtitleCueParser.parse(srt)
        // With +2s delay the first cue shows from 3000 ms to 5500 ms of video time.
        assertTrue(SubtitleCueParser.activeCues(cues, 2000, 2000, 1.0).isEmpty())
        assertEquals(listOf("Hello there"), SubtitleCueParser.activeCues(cues, 3000, 2000, 1.0).map { it.text })
        // Negative delay shifts earlier.
        assertEquals(listOf("Hello there"), SubtitleCueParser.activeCues(cues, 500, -1000, 1.0).map { it.text })
    }

    @Test
    fun rateScalesCueTimesLikeFpsConversion() {
        val cues = SubtitleCueParser.parse(srt)
        val rate = SubtitleRatePresets.all.first { it.id == "23976-25" }.rate // ~0.959
        // The cue at 1 hour in the subtitle file appears ~2.46 minutes earlier in the video.
        val videoStart = (3_600_000 * rate).toLong()
        assertEquals(listOf("Late cue"), SubtitleCueParser.activeCues(cues, videoStart + 100, 0, rate).map { it.text })
        // Without the rate the same video time would show nothing (drift of ~2.5 minutes).
        assertTrue(SubtitleCueParser.activeCues(cues, videoStart + 100, 0, 1.0).isEmpty())
    }

    @Test
    fun rateAndDelayCombine() {
        val cues = listOf(SubtitleCue(10_000, 12_000, "x"))
        // video time = cue time * rate + delay  ->  10 s * 0.5 + 1 s = 6 s
        assertEquals(1, SubtitleCueParser.activeCues(cues, 6_000, 1_000, 0.5).size)
        assertTrue(SubtitleCueParser.activeCues(cues, 5_900, 1_000, 0.5).isEmpty())
    }

    @Test
    fun invalidRateFallsBackToOne() {
        val cues = listOf(SubtitleCue(1000, 2000, "x"))
        assertEquals(1, SubtitleCueParser.activeCues(cues, 1500, 0, 0.0).size)
        assertEquals(1, SubtitleCueParser.activeCues(cues, 1500, 0, Double.NaN).size)
    }

    @Test
    fun decodeHandlesBomAndLegacyEncoding() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "café".toByteArray(Charsets.UTF_8)
        assertEquals("café", SubtitleCueParser.decode(bom))
        val latin = byteArrayOf('c'.code.toByte(), 'a'.code.toByte(), 'f'.code.toByte(), 0xE9.toByte())
        assertEquals("café", SubtitleCueParser.decode(latin))
    }
}
