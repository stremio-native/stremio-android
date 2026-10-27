package com.stremio.mobile.player

import android.content.Context
import android.net.Uri
import android.view.View
import kotlinx.coroutines.flow.StateFlow

/**
 * An addon-provided subtitle, independent of the generated stremio-core protobuf type so the
 * player module doesn't need to depend on it.
 */
data class ExternalSubtitle(
    val id: String,
    val lang: String,
    val url: String,
    val label: String?,
    /** The addon that supplied this subtitle, shown alongside the label so the track picker
     * can distinguish addon-provided subtitles from ones embedded in the video container. */
    val source: String? = null,
    val origin: String = "EXTERNAL",
    val fallbackUrl: String? = null,
    val addonSubtitleId: String? = null,
    val embedded: Boolean = false,
    val local: Boolean = false,
    val exclusive: Boolean = false,
)

enum class PlayerEngine(val profileValue: String) {
    EXO("exo"),
    MPV("mpv");

    companion object {
        fun fromProfileValue(value: String?): PlayerEngine {
            return entries.firstOrNull { it.profileValue.equals(value, ignoreCase = true) } ?: EXO
        }
    }
}

enum class PlayerResizeMode {
    FIT,
    STRETCH,
    ZOOM,
}

enum class PlayerTrackType {
    AUDIO,
    SUBTITLE,
}

data class PlayerTrackOption(
    val id: String,
    val type: PlayerTrackType,
    val label: String,
    val language: String?,
    val selected: Boolean,
    val languageCode: String? = LanguageCatalog.toCode(language),
    val origin: String = if (type == PlayerTrackType.SUBTITLE) "EMBEDDED" else "AUDIO",
    val url: String? = null,
    val fallbackUrl: String? = null,
    val addonSubtitleId: String? = null,
    val embedded: Boolean = type == PlayerTrackType.SUBTITLE,
    val local: Boolean = false,
    val exclusive: Boolean = false,
)

data class PlayerSubtitleStyle(
    val sizePercent: Int = 100,
    val offsetPercent: Int = 0,
    val delayMs: Long = 0L,
    /** Subtitle time scale (subtitle fps / video fps); see [SubtitleRatePresets]. */
    val rate: Double = 1.0,
    val textColor: String = "#FFFFFF",
    val backgroundColor: String = "#00000000",
    val outlineColor: String = "#000000",
    val assStyling: Boolean = true,
)
data class PlayerRuntimeState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val speed: Float = 1.0f,
    val audioTracks: List<PlayerTrackOption> = emptyList(),
    val subtitleTracks: List<PlayerTrackOption> = emptyList(),
    val subtitlesDisabled: Boolean = true,
    val error: String? = null,
    val ended: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoFrameRate: Float = 0f,
)

interface Player {
    val engine: PlayerEngine
    val runtimeState: StateFlow<PlayerRuntimeState>

    fun createView(context: Context): View
    fun load(
        uri: Uri,
        startPositionMs: Long = 0,
        subtitles: List<ExternalSubtitle> = emptyList(),
        preferredSubtitleLang: String? = null,
        settings: com.stremio.core.types.profile.Profile.Settings? = null,
    )
    fun retry()
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setResizeMode(mode: PlayerResizeMode)
    fun selectAudioTrack(id: String)
    fun selectSubtitleTrack(id: String)
    fun disableSubtitles()
    fun setSubtitleStyle(style: PlayerSubtitleStyle)
    fun addExternalSubtitleTracks(tracks: List<ExternalSubtitle>)
    fun addLocalSubtitle(track: ExternalSubtitle)
    fun release()
}
