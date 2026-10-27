package com.stremio.mobile.player

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.text.Cue
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ExoStreamPlayer(
    context: Context,
    private val settings: com.stremio.core.types.profile.Profile.Settings? = null
) : Player {
    override val engine: PlayerEngine = PlayerEngine.EXO

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableRuntimeState = MutableStateFlow(PlayerRuntimeState())

    override val runtimeState: StateFlow<PlayerRuntimeState> = mutableRuntimeState

    private val exoPlayer = run {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(appContext, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(dataSourceFactory)

        val hardwareDecoding = settings?.hardwareDecoding ?: true
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink {
                val audioPassthrough = settings?.audioPassthrough ?: false
                val surroundSound = settings?.surroundSound ?: false
                val audioCapabilities = if (!audioPassthrough || !surroundSound) {
                    val encodings = if (audioPassthrough) {
                        intArrayOf(
                            android.media.AudioFormat.ENCODING_PCM_16BIT,
                            android.media.AudioFormat.ENCODING_AC3,
                            android.media.AudioFormat.ENCODING_E_AC3,
                            android.media.AudioFormat.ENCODING_DTS,
                            android.media.AudioFormat.ENCODING_DTS_HD
                        )
                    } else {
                        intArrayOf(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    }
                    val maxChannels = if (surroundSound) 8 else 2
                    androidx.media3.exoplayer.audio.AudioCapabilities(encodings, maxChannels)
                } else {
                    androidx.media3.exoplayer.audio.AudioCapabilities.getCapabilities(context)
                }
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setAudioCapabilities(audioCapabilities)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }.setEnableDecoderFallback(true)

        if (!hardwareDecoding) {
            renderersFactory.setMediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
                val decoders = androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT
                    .getDecoderInfos(mimeType, requiresSecure, requiresTunneling)
                decoders.filter { info ->
                    val name = info.name.lowercase()
                    name.startsWith("omx.google.") || name.startsWith("c2.android.") || name.contains(".sw.") || name.endsWith(".sw")
                }
            }
        }

        ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .setRenderersFactory(renderersFactory)
            .build()
    }

    private var playerView: PlayerView? = null
    private var currentUri: Uri? = null
    private var currentStartPositionMs: Long = 0L
    private var currentSubtitles: List<ExternalSubtitle> = emptyList()
    private var currentPreferredSubtitleLang: String? = null
    private var currentSubtitleStyle = PlayerSubtitleStyle()

    // External SRT/VTT subtitles are rendered by our own overlay so that subtitle delay and FPS
    // rate apply live (ExoPlayer has no timing offset for subtitle tracks).
    private var overlayView: SubtitleView? = null
    private var overlayCues: List<SubtitleCue> = emptyList()
    private var overlaySubtitleId: String? = null
    private var overlayActive = false
    private var overlayLoadJob: Job? = null
    private var overlayShown: List<SubtitleCue> = emptyList()
    private val overlayHandler = Handler(Looper.getMainLooper())
    private val overlayTick = object : Runnable {
        override fun run() {
            renderOverlay()
            if (overlayActive) overlayHandler.postDelayed(this, OVERLAY_TICK_MS)
        }
    }

    private val listener = object : androidx.media3.common.Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            publishState(ended = playbackState == androidx.media3.common.Player.STATE_ENDED)
        }

        override fun onPlayerError(error: PlaybackException) {
            publishState(error = error.message ?: "Playback failed")
        }

        override fun onTracksChanged(tracks: Tracks) {
            publishState()
            updateExternalOverlay()
        }

        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
            publishState()
        }
    }

    init {
        exoPlayer.addListener(listener)
        scope.launch {
            while (isActive) {
                publishState()
                delay(500)
            }
        }
    }

    override fun createView(context: Context): View {
        return PlayerView(context).apply {
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            keepScreenOn = true
            resizeMode = playerView?.resizeMode ?: AspectRatioFrameLayout.RESIZE_MODE_FIT
            player = exoPlayer
            playerView = this
            overlayView = SubtitleView(context).also { overlay ->
                overlayFrameLayout?.addView(
                    overlay,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                )
                overlay.visibility = View.GONE
            }
            applySubtitleStyleToView()
            updateExternalOverlay()
        }
    }

    override fun load(
        uri: Uri,
        startPositionMs: Long,
        subtitles: List<ExternalSubtitle>,
        preferredSubtitleLang: String?,
        settings: com.stremio.core.types.profile.Profile.Settings?,
    ) {
        currentUri = uri
        currentStartPositionMs = startPositionMs
        currentSubtitles = subtitles
        currentPreferredSubtitleLang = preferredSubtitleLang

        val mediaItem = buildMediaItem(uri, subtitles, preferredSubtitleLang)
        exoPlayer.setMediaItem(mediaItem, startPositionMs)
        exoPlayer.prepare()
        publishState(error = null, ended = false)
    }

    override fun retry() {
        mutableRuntimeState.value = mutableRuntimeState.value.copy(error = null, ended = false)
        currentUri?.let { uri ->
            val resumePosition = exoPlayer.currentPosition.coerceAtLeast(currentStartPositionMs)
            exoPlayer.setMediaItem(buildMediaItem(uri, currentSubtitles, currentPreferredSubtitleLang), resumePosition)
        }
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun reportNonFatalError(message: String?) {
        if (message == null) return
        publishState(error = message, ended = false)
    }

    override fun play() {
        exoPlayer.play()
        publishState()
    }

    override fun pause() {
        exoPlayer.pause()
        publishState()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        publishState()
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        publishState()
    }

    override fun setResizeMode(mode: PlayerResizeMode) {
        playerView?.resizeMode = when (mode) {
            PlayerResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            PlayerResizeMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            PlayerResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    override fun selectAudioTrack(id: String) {
        val parsed = ExoTrackId.parse(id) ?: return
        if (parsed.type != PlayerTrackType.AUDIO) return
        selectTrack(parsed, C.TRACK_TYPE_AUDIO)
    }

    override fun selectSubtitleTrack(id: String) {
        val parsed = ExoTrackId.parse(id) ?: return
        if (parsed.type != PlayerTrackType.SUBTITLE) return
        enableTextTracks()
        selectTrack(parsed, C.TRACK_TYPE_TEXT)
        updateExternalOverlay()
    }

    override fun disableSubtitles() {
        val disabledTypes = exoPlayer.trackSelectionParameters.disabledTrackTypes.toMutableSet()
        disabledTypes.add(C.TRACK_TYPE_TEXT)
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setDisabledTrackTypes(disabledTypes)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
        publishState()
        updateExternalOverlay()
    }

    override fun setSubtitleStyle(style: PlayerSubtitleStyle) {
        currentSubtitleStyle = style
        applySubtitleStyleToView()
        renderOverlay()
    }

    override fun addExternalSubtitleTracks(tracks: List<ExternalSubtitle>) {
        val unique = (currentSubtitles + tracks)
            .distinctBy { it.id }
        if (unique.size == currentSubtitles.size) return
        currentSubtitles = unique
        rebuildMediaItemPreservingPlayback()
    }

    override fun addLocalSubtitle(track: ExternalSubtitle) {
        currentPreferredSubtitleLang = LanguageCatalog.LOCAL_SUBTITLES_LANGUAGE
        addExternalSubtitleTracks(
            listOf(
                track.copy(
                    lang = track.lang.ifBlank { LanguageCatalog.LOCAL_SUBTITLES_LANGUAGE },
                    origin = "LOCAL",
                    embedded = false,
                    local = true,
                )
            )
        )
        enableTextTracks()
    }

    override fun release() {
        overlayActive = false
        overlayHandler.removeCallbacks(overlayTick)
        overlayLoadJob?.cancel()
        scope.cancel()
        playerView?.player = null
        playerView = null
        exoPlayer.removeListener(listener)
        exoPlayer.release()
        mutableRuntimeState.value = PlayerRuntimeState()
    }

    private fun applySubtitleStyleToView() {
        listOfNotNull(playerView?.subtitleView, overlayView).forEach { subView ->
            val style = currentSubtitleStyle
            subView.setApplyEmbeddedFontSizes(false)
            subView.setApplyEmbeddedStyles(false)
            subView.setFractionalTextSize(0.0533f * (style.sizePercent / 100f))

            val textColor = parseSubtitleColor(style.textColor, Color.WHITE)
            val backgroundColor = parseSubtitleColor(style.backgroundColor, Color.TRANSPARENT)
            val outlineColor = parseSubtitleColor(style.outlineColor, Color.BLACK)
            subView.setStyle(
                CaptionStyleCompat(
                    textColor,
                    backgroundColor,
                    Color.TRANSPARENT,
                    if (Color.alpha(outlineColor) == 0) {
                        CaptionStyleCompat.EDGE_TYPE_NONE
                    } else {
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE
                    },
                    outlineColor,
                    null,
                )
            )

            val maxPaddingPx = with(subView.resources.displayMetrics) { 150 * density }
            val bottomPadding = ((style.offsetPercent / 100f) * maxPaddingPx).toInt()
            subView.setPadding(subView.paddingLeft, subView.paddingTop, subView.paddingRight, bottomPadding)
        }
    }

    private fun selectedExternalSubtitle(): ExternalSubtitle? {
        for (group in exoPlayer.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            for (index in 0 until group.length) {
                if (!group.isTrackSelected(index)) continue
                val format = group.getTrackFormat(index)
                return findExternalSubtitle(format.id, group.mediaTrackGroup.id, format.label, format.language)
            }
        }
        return null
    }

    /**
     * Shows the selected external subtitle through our own overlay (so delay and FPS rate apply)
     * and hides ExoPlayer's built-in subtitle view for it. Embedded tracks, ASS/SSA and subtitles
     * that cannot be parsed keep using ExoPlayer's own rendering.
     */
    private fun updateExternalOverlay() {
        val disabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        val subtitle = if (disabled) null else selectedExternalSubtitle()
        if (subtitle == null || inferSubtitleMime(subtitle.url) == MimeTypes.TEXT_SSA) {
            deactivateOverlay()
            return
        }
        if (subtitle.id == overlaySubtitleId) return

        overlayLoadJob?.cancel()
        overlaySubtitleId = subtitle.id
        overlayLoadJob = scope.launch {
            val cues = withContext(Dispatchers.IO) {
                runCatching { SubtitleCueParser.parse(SubtitleCueParser.decode(readSubtitleBytes(subtitle.url))) }
                    .getOrNull()
            }
            if (overlaySubtitleId != subtitle.id) return@launch
            if (cues.isNullOrEmpty()) {
                // Unparseable: leave ExoPlayer's own rendering in place.
                deactivateOverlay(keepId = true)
                return@launch
            }
            overlayCues = cues
            overlayShown = emptyList()
            overlayActive = true
            playerView?.subtitleView?.visibility = View.GONE
            overlayView?.visibility = View.VISIBLE
            overlayHandler.removeCallbacks(overlayTick)
            overlayHandler.post(overlayTick)
        }
    }

    private fun deactivateOverlay(keepId: Boolean = false) {
        overlayLoadJob?.cancel()
        overlayActive = false
        if (!keepId) overlaySubtitleId = null
        overlayCues = emptyList()
        overlayShown = emptyList()
        overlayHandler.removeCallbacks(overlayTick)
        overlayView?.setCues(emptyList())
        overlayView?.visibility = View.GONE
        playerView?.subtitleView?.visibility = View.VISIBLE
    }

    private fun renderOverlay() {
        if (!overlayActive) return
        val style = currentSubtitleStyle
        val active = SubtitleCueParser.activeCues(
            overlayCues,
            exoPlayer.currentPosition.coerceAtLeast(0L),
            style.delayMs,
            style.rate,
        )
        if (active == overlayShown) return
        overlayShown = active
        overlayView?.setCues(active.map { Cue.Builder().setText(it.text).build() })
    }

    private fun readSubtitleBytes(url: String): ByteArray {
        val uri = Uri.parse(url)
        if (uri.scheme == "http" || uri.scheme == "https") {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            return try {
                connection.inputStream.use { it.readNBytes(MAX_SUBTITLE_BYTES) }
            } finally {
                connection.disconnect()
            }
        }
        val stream = appContext.contentResolver.openInputStream(uri)
            ?: throw java.io.FileNotFoundException(url)
        return stream.use { it.readNBytes(MAX_SUBTITLE_BYTES) }
    }

    private fun rebuildMediaItemPreservingPlayback() {
        val uri = currentUri ?: return
        val position = exoPlayer.currentPosition.coerceAtLeast(0L)
        val wasPlaying = exoPlayer.isPlaying || exoPlayer.playWhenReady
        val speed = exoPlayer.playbackParameters.speed
        exoPlayer.setMediaItem(buildMediaItem(uri, currentSubtitles, currentPreferredSubtitleLang), position)
        exoPlayer.prepare()
        exoPlayer.setPlaybackSpeed(speed)
        if (wasPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
        publishState(error = null, ended = false)
    }

    private fun buildMediaItem(
        uri: Uri,
        subtitles: List<ExternalSubtitle>,
        preferredSubtitleLang: String?,
    ): MediaItem {
        val subtitleConfigs = subtitles.map { sub ->
            val isDefault = preferredSubtitleLang != null &&
                LanguageCatalog.matches(sub.lang, preferredSubtitleLang)
            val baseLabel = sub.label ?: sub.lang
            val label = if (sub.source != null) "$baseLabel (${sub.source})" else baseLabel
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setId(sub.id)
                .setMimeType(inferSubtitleMime(sub.url))
                .setLanguage(sub.lang)
                .setLabel(label)
                .setSelectionFlags(if (isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
        return MediaItem.Builder()
            .setUri(uri)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()
    }

    private fun selectTrack(parsed: ExoTrackId, media3Type: Int) {
        val group = exoPlayer.currentTracks.groups.getOrNull(parsed.groupIndex) ?: return
        if (group.type != media3Type || parsed.trackIndex !in 0 until group.length) return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, parsed.trackIndex))
            .build()
        publishState()
    }

    private fun enableTextTracks() {
        val disabledTypes = exoPlayer.trackSelectionParameters.disabledTrackTypes.toMutableSet()
        disabledTypes.remove(C.TRACK_TYPE_TEXT)
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setDisabledTrackTypes(disabledTypes)
            .build()
    }

    private fun publishState(
        error: String? = mutableRuntimeState.value.error,
        ended: Boolean = exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED,
    ) {
        val videoFormat = exoPlayer.videoFormat
        val videoWidth = videoFormat?.width ?: 0
        val videoHeight = videoFormat?.height ?: 0
        val videoFrameRate = videoFormat?.frameRate ?: 0f

        mutableRuntimeState.value = PlayerRuntimeState(
            isPlaying = exoPlayer.isPlaying,
            isBuffering = exoPlayer.playbackState == androidx.media3.common.Player.STATE_BUFFERING,
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = if (exoPlayer.duration == C.TIME_UNSET) 0L else exoPlayer.duration.coerceAtLeast(0L),
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L),
            speed = exoPlayer.playbackParameters.speed,
            audioTracks = getTrackOptions(C.TRACK_TYPE_AUDIO),
            subtitleTracks = getTrackOptions(C.TRACK_TYPE_TEXT),
            subtitlesDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT),
            error = error,
            ended = ended,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            videoFrameRate = videoFrameRate,
        )
    }

    private fun getTrackOptions(trackType: Int): List<PlayerTrackOption> {
        val options = mutableListOf<PlayerTrackOption>()
        for ((groupIndex, group) in exoPlayer.currentTracks.groups.withIndex()) {
            if (group.type != trackType) continue
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val externalSubtitle = if (trackType == C.TRACK_TYPE_TEXT) {
                    findExternalSubtitle(format.id, group.mediaTrackGroup.id, format.label, format.language)
                } else {
                    null
                }
                val lang = LanguageCatalog.toCode(format.language)
                    ?: LanguageCatalog.toCode(externalSubtitle?.lang)
                val label = externalSubtitle?.let { buildExternalSubtitleLabel(it) }
                    ?: format.label
                    ?: lang?.uppercase(Locale.ROOT)
                    ?: if (trackType == C.TRACK_TYPE_AUDIO) {
                        "Track ${options.size + 1}"
                    } else {
                        "Subtitles ${options.size + 1}"
                    }
                val optionType = if (trackType == C.TRACK_TYPE_AUDIO) PlayerTrackType.AUDIO else PlayerTrackType.SUBTITLE
                options.add(
                    PlayerTrackOption(
                        id = ExoTrackId(
                            type = optionType,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                        ).encode(),
                        type = optionType,
                        label = label,
                        language = format.language ?: externalSubtitle?.lang,
                        selected = group.isTrackSelected(trackIndex),
                        languageCode = lang,
                        origin = externalSubtitle?.origin ?: if (trackType == C.TRACK_TYPE_TEXT) "EMBEDDED" else "AUDIO",
                        url = externalSubtitle?.url,
                        fallbackUrl = externalSubtitle?.fallbackUrl,
                        addonSubtitleId = externalSubtitle?.addonSubtitleId,
                        embedded = trackType == C.TRACK_TYPE_TEXT && externalSubtitle == null,
                        local = externalSubtitle?.local == true,
                        exclusive = externalSubtitle?.exclusive == true,
                    )
                )
            }
        }
        return options
    }

    private fun findExternalSubtitle(
        formatId: String?,
        groupId: String?,
        label: String?,
        language: String?,
    ): ExternalSubtitle? {
        return currentSubtitles.firstOrNull { subtitle ->
            // ExoPlayer prefixes media-item subtitle ids with the track-group index ("1:<id>").
            formatId.matchesSubtitleId(subtitle.id) || groupId.matchesSubtitleId(subtitle.id)
        } ?: currentSubtitles.firstOrNull { subtitle ->
            subtitle.label != null &&
                (subtitle.label == label || buildExternalSubtitleLabel(subtitle) == label) &&
                LanguageCatalog.matches(subtitle.lang, language)
        }
    }

    private fun String?.matchesSubtitleId(id: String): Boolean =
        this != null && (this == id || endsWith(":$id"))

    private fun buildExternalSubtitleLabel(subtitle: ExternalSubtitle): String {
        val base = subtitle.label?.takeIf { it.isNotBlank() } ?: subtitle.lang.uppercase(Locale.ROOT)
        return if (subtitle.source != null) "$base (${subtitle.source})" else base
    }
}

/** Addon subtitle URLs rarely carry a useful Content-Type; guess from the file extension. */
private const val OVERLAY_TICK_MS = 100L
private const val MAX_SUBTITLE_BYTES = 5 * 1024 * 1024

private fun inferSubtitleMime(url: String): String {
    val path = url.substringBefore('?').substringBefore('#')
    return when {
        path.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
        path.endsWith(".ass", ignoreCase = true) || path.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

private fun parseSubtitleColor(value: String?, fallback: Int): Int {
    val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return fallback
    return runCatching {
        when {
            raw.length == 9 && raw.startsWith("#") -> {
                val alpha = raw.substring(1, 3)
                val rgb = raw.substring(3)
                Color.parseColor("#$alpha$rgb")
            }
            raw.length == 7 && raw.startsWith("#") -> Color.parseColor(raw)
            else -> Color.parseColor(raw)
        }
    }.getOrDefault(fallback)
}
