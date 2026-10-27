package com.stremio.mobile.player

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/**
 * Minimal SRT / WebVTT parser used to render external subtitles ourselves, so that subtitle
 * delay and FPS rate can be applied live without reloading the media item.
 *
 * A cue is visible at video time `v` when `startMs * rate + delayMs <= v < endMs * rate + delayMs`.
 */
object SubtitleCueParser {
    private const val MAX_OVERLAPPING_CUES = 8

    private val blockSeparator = Regex("\\r?\\n\\s*\\r?\\n")
    private val timeLine = Regex(
        "((?:\\d+:)?\\d{1,2}:\\d{2}[,.]\\d{1,3})\\s*-->\\s*((?:\\d+:)?\\d{1,2}:\\d{2}[,.]\\d{1,3})"
    )
    private val tags = Regex("<[^>]*>|\\{\\\\[^}]*\\}")

    /** Decodes subtitle bytes as UTF-8, falling back to Windows-1252 for legacy files. */
    fun decode(bytes: ByteArray): String {
        var offset = 0
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            offset = 3
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.size - offset)).toString()
        } catch (_: CharacterCodingException) {
            String(bytes, offset, bytes.size - offset, charset("windows-1252"))
        }
    }

    fun parse(content: String): List<SubtitleCue> {
        val cues = ArrayList<SubtitleCue>()
        for (block in content.trim().split(blockSeparator)) {
            val lines = block.lines()
            val timeIndex = lines.indexOfFirst { timeLine.containsMatchIn(it) }
            if (timeIndex < 0) continue
            val match = timeLine.find(lines[timeIndex]) ?: continue
            val start = parseTime(match.groupValues[1]) ?: continue
            val end = parseTime(match.groupValues[2]) ?: continue
            if (end <= start) continue
            val text = lines.drop(timeIndex + 1)
                .joinToString("\n") { cleanLine(it) }
                .trim()
            if (text.isEmpty()) continue
            cues.add(SubtitleCue(start, end, text))
        }
        cues.sortBy { it.startMs }
        return cues
    }

    /** Cues visible at [videoTimeMs], given the subtitle [delayMs] and [rate] (subtitle fps / video fps). */
    fun activeCues(cues: List<SubtitleCue>, videoTimeMs: Long, delayMs: Long, rate: Double): List<SubtitleCue> {
        if (cues.isEmpty()) return emptyList()
        val safeRate = if (rate.isFinite() && rate > 0.0) rate else 1.0
        val cueTime = (videoTimeMs - delayMs) / safeRate

        // Last cue starting at or before cueTime (cues are sorted by start).
        var low = 0
        var high = cues.size - 1
        var index = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (cues[mid].startMs <= cueTime) {
                index = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (index < 0) return emptyList()

        val active = ArrayList<SubtitleCue>(2)
        var checked = 0
        var i = index
        while (i >= 0 && checked < MAX_OVERLAPPING_CUES) {
            val cue = cues[i]
            if (cueTime < cue.endMs) active.add(cue)
            i--
            checked++
        }
        active.reverse()
        return active
    }

    private fun parseTime(value: String): Long? {
        val parts = value.replace(',', '.').split(':')
        return try {
            val (hours, minutes, secondsPart) = when (parts.size) {
                3 -> Triple(parts[0].toLong(), parts[1].toLong(), parts[2])
                2 -> Triple(0L, parts[0].toLong(), parts[1])
                else -> return null
            }
            val secParts = secondsPart.split('.')
            val seconds = secParts[0].toLong()
            val millis = (secParts.getOrNull(1) ?: "0").padEnd(3, '0').take(3).toLong()
            ((hours * 60 + minutes) * 60 + seconds) * 1000 + millis
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun cleanLine(line: String): String {
        return tags.replace(line, "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .trim()
    }
}
