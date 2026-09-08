package com.stremio.mobile.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceHolder
import android.view.View
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPVLib
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MpvStreamPlayer(
    context: Context,
    private val settings: com.stremio.core.types.profile.Profile.Settings? = null
) : Player {
    override val engine: PlayerEngine = PlayerEngine.MPV

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableRuntimeState = MutableStateFlow(PlayerRuntimeState())

    override val runtimeState: StateFlow<PlayerRuntimeState> = mutableRuntimeState

    private var view: StremioMpvView? = null
    private var initialized = false
    private var surfaceReady = false
    private var released = false
    private var fileLoaded = false
    private var eofReached = false
    private val addedSubtitleIds = mutableSetOf<String>()
    private var resizeMode = PlayerResizeMode.FIT

    private var currentUri: Uri? = null
    private var currentStartPositionMs: Long = 0L
    private var currentSubtitles: List<ExternalSubtitle> = emptyList()
    private var currentPreferredSubtitleLang: String? = null
    private var currentSubtitleStyle = PlayerSubtitleStyle()

    private val propertyReader = object : MpvTrackPropertyReader {
        override fun getInt(property: String): Int? = runCatching { MPVLib.getPropertyInt(property) }.getOrNull()
        override fun getString(property: String): String? = runCatching { MPVLib.getPropertyString(property) }.getOrNull()
        override fun getBoolean(property: String): Boolean? = runCatching { MPVLib.getPropertyBoolean(property) }.getOrNull()
    }

    private val observer = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {
            if (property == "track-list") publishState()
        }

        override fun eventProperty(property: String, value: Long) {
            publishState()
        }

        override fun eventProperty(property: String, value: Boolean) {
            if (property == "eof-reached") eofReached = value
            publishState()
        }

        override fun eventProperty(property: String, value: String) {
            publishState()
        }

        override fun eventProperty(property: String, value: Double) {
            publishState()
        }

        override fun event(eventId: Int) {
            when (eventId) {
                MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                    fileLoaded = true
                    addMissingExternalSubtitles()
                    if (currentStartPositionMs > 0) seekTo(currentStartPositionMs)
                    publishState(error = null, ended = false)
                }
                MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                    if (released) return
                    val duration = mutableRuntimeState.value.durationMs
                    val position = mutableRuntimeState.value.positionMs
                    val reachedEnd = eofReached || (duration > 0 && duration - position <= 1_500)
                    if (reachedEnd) {
                        publishState(error = null, ended = true)
                    } else {
                        publishState(error = "Playback failed", ended = false)
                    }
                }
                MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> publishState(error = null, ended = false)
            }
        }
    }

    init {
        MPVLib.addObserver(observer)
        scope.launch {
            while (isActive) {
                publishState()
                delay(500)
            }
        }
    }

    override fun createView(context: Context): View {
        view?.destroy()
        copyMpvAssets(appContext)
        return StremioMpvView(
            context = context,
            settings = settings,
            onSurfaceReady = {
                surfaceReady = true
                applyResizeMode()
                loadIfReady()
            },
            onSurfaceDestroyed = {
                surfaceReady = false
            },
        ).also { mpvView ->
            view = mpvView
            mpvView.initialize(
                configDir = appContext.filesDir.absolutePath,
                cacheDir = appContext.cacheDir.absolutePath,
            )
            initialized = true
            applyResizeMode()
            applySubtitleStyle()
            loadIfReady()
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
        fileLoaded = false
        eofReached = false
        addedSubtitleIds.clear()
        mutableRuntimeState.value = PlayerRuntimeState(isBuffering = true)
        loadIfReady()
    }

    override fun retry() {
        currentStartPositionMs = mutableRuntimeState.value.positionMs
        fileLoaded = false
        eofReached = false
        addedSubtitleIds.clear()
        mutableRuntimeState.value = mutableRuntimeState.value.copy(error = null, ended = false, isBuffering = true)
        loadIfReady(force = true)
    }

    override fun play() {
        if (!initialized) return
        MPVLib.setPropertyBoolean("pause", false)
        publishState()
    }

    override fun pause() {
        if (!initialized) return
        MPVLib.setPropertyBoolean("pause", true)
        publishState()
    }

    override fun seekTo(positionMs: Long) {
        if (!initialized) return
        MPVLib.setPropertyDouble("time-pos", positionMs / 1000.0)
        publishState()
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (!initialized) return
        MPVLib.setPropertyDouble("speed", speed.toDouble())
        publishState()
    }

    override fun setResizeMode(mode: PlayerResizeMode) {
        resizeMode = mode
        applyResizeMode()
    }

    override fun selectAudioTrack(id: String) {
        val parsed = MpvTrackId.parse(id) ?: return
        if (parsed.kind != "audio" || !initialized) return
        MPVLib.setPropertyInt("aid", parsed.mpvId)
        publishState()
    }

    override fun selectSubtitleTrack(id: String) {
        val parsed = MpvTrackId.parse(id) ?: return
        if (parsed.kind != "sub" || !initialized) return
        MPVLib.setPropertyInt("sid", parsed.mpvId)
        publishState()
    }

    override fun disableSubtitles() {
        if (!initialized) return
        MPVLib.setPropertyString("sid", "no")
        publishState()
    }

    override fun setSubtitleStyle(style: PlayerSubtitleStyle) {
        currentSubtitleStyle = style
        applySubtitleStyle()
    }

    override fun addExternalSubtitleTracks(tracks: List<ExternalSubtitle>) {
        val unique = (currentSubtitles + tracks)
            .distinctBy { it.id }
        if (unique.size == currentSubtitles.size) return
        currentSubtitles = unique
        if (fileLoaded) addMissingExternalSubtitles()
    }

    override fun addLocalSubtitle(track: ExternalSubtitle) {
        val localTrack = track.copy(
            lang = track.lang.ifBlank { LanguageCatalog.LOCAL_SUBTITLES_LANGUAGE },
            origin = "LOCAL",
            embedded = false,
            local = true,
        )
        addExternalSubtitleTracks(listOf(localTrack))
    }

    override fun release() {
        released = true
        scope.cancel()
        runCatching { MPVLib.removeObserver(observer) }
        view?.destroy()
        view = null
        initialized = false
        mutableRuntimeState.value = PlayerRuntimeState()
    }

    private fun loadIfReady(force: Boolean = false) {
        val uri = currentUri ?: return
        if (!initialized || !surfaceReady) return
        if (!force && fileLoaded) return

        val startSeconds = currentStartPositionMs / 1000.0
        val command = if (startSeconds > 0.0) {
            arrayOf("loadfile", uri.toString(), "replace", "-1", "start=$startSeconds")
        } else {
            arrayOf("loadfile", uri.toString(), "replace")
        }
        MPVLib.command(command)
        MPVLib.setPropertyBoolean("pause", false)
        publishState(error = null, ended = false)
    }

    private fun addMissingExternalSubtitles() {
        if (!initialized) return
        currentSubtitles.forEach { subtitle ->
            if (!addedSubtitleIds.add(subtitle.id)) return@forEach
            val baseLabel = subtitle.label ?: subtitle.lang
            val label = if (subtitle.source != null) "$baseLabel (${subtitle.source})" else baseLabel
            val select = currentPreferredSubtitleLang != null &&
                LanguageCatalog.matches(subtitle.lang, currentPreferredSubtitleLang)
            MPVLib.command(
                arrayOf(
                    "sub-add",
                    subtitle.url,
                    if (select || subtitle.local) "select" else "auto",
                    label,
                    subtitle.lang,
                )
            )
        }
    }

    private fun applyResizeMode() {
        if (!initialized) return
        val properties = MpvResizeMapper.properties(resizeMode)
        MPVLib.setPropertyBoolean("keepaspect", properties.keepAspect)
        MPVLib.setPropertyDouble("panscan", properties.panscan)
    }

    private fun applySubtitleStyle() {
        if (!initialized) return
        val style = currentSubtitleStyle
        MPVLib.setPropertyDouble("sub-scale", style.sizePercent / 100.0)
        MPVLib.setPropertyDouble("sub-pos", 100.0 - ((style.offsetPercent / 100.0) * 30.0))
        MPVLib.setPropertyDouble("sub-delay", style.delayMs / 1000.0)
        runCatching { MPVLib.setPropertyString("sub-color", style.textColor) }
        runCatching { MPVLib.setPropertyString("sub-back-color", style.backgroundColor) }
        runCatching { MPVLib.setPropertyString("sub-border-color", style.outlineColor) }
        runCatching {
            MPVLib.setPropertyString("sub-ass-override", if (style.assStyling) "no" else "force")
        }
    }

    private fun publishState(
        error: String? = mutableRuntimeState.value.error,
        ended: Boolean = mutableRuntimeState.value.ended,
    ) {
        if (!initialized) return

        val durationMs = (runCatching { MPVLib.getPropertyDouble("duration/full") }.getOrNull() ?: 0.0)
            .coerceAtLeast(0.0)
            .times(1000.0)
            .toLong()
        val positionMs = (runCatching { MPVLib.getPropertyDouble("time-pos/full") }.getOrNull() ?: 0.0)
            .coerceAtLeast(0.0)
            .times(1000.0)
            .toLong()
        val paused = runCatching { MPVLib.getPropertyBoolean("pause") }.getOrNull() ?: true
        val buffering = runCatching { MPVLib.getPropertyBoolean("paused-for-cache") }.getOrNull() ?: false
        val speed = (runCatching { MPVLib.getPropertyDouble("speed") }.getOrNull() ?: 1.0).toFloat()
        val (audioTracks, parsedSubtitleTracks) = MpvTrackParser.parse(propertyReader)
        val subtitleTracks = parsedSubtitleTracks.map { enrichSubtitleTrack(it) }
        val sid = runCatching { MPVLib.getPropertyString("sid") }.getOrNull()

        val width = runCatching { MPVLib.getPropertyInt("width") }.getOrNull() ?: 0
        val height = runCatching { MPVLib.getPropertyInt("height") }.getOrNull() ?: 0
        val fps = runCatching { MPVLib.getPropertyDouble("container-fps") }.getOrNull()?.toFloat() ?: 0f

        mutableRuntimeState.value = PlayerRuntimeState(
            isPlaying = !paused && !buffering && error == null && !ended,
            isBuffering = buffering || (!fileLoaded && error == null && !ended),
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedPositionMs = durationMs,
            speed = speed,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            subtitlesDisabled = sid == "no" || subtitleTracks.none { it.selected },
            error = error,
            ended = ended,
            videoWidth = width,
            videoHeight = height,
            videoFrameRate = fps,
        )
    }

    private fun enrichSubtitleTrack(track: PlayerTrackOption): PlayerTrackOption {
        if (track.embedded) return track
        val external = currentSubtitles.firstOrNull { subtitle ->
            LanguageCatalog.matches(subtitle.lang, track.language) &&
                (subtitle.label == null || track.label.contains(subtitle.label, ignoreCase = true))
        } ?: currentSubtitles.firstOrNull { subtitle ->
            LanguageCatalog.matches(subtitle.lang, track.language)
        } ?: return track

        return track.copy(
            origin = external.origin,
            url = external.url,
            fallbackUrl = external.fallbackUrl,
            addonSubtitleId = external.addonSubtitleId,
            local = external.local,
            exclusive = external.exclusive,
        )
    }
    private class StremioMpvView(
        context: Context,
        private val settings: com.stremio.core.types.profile.Profile.Settings?,
        private val onSurfaceReady: () -> Unit,
        private val onSurfaceDestroyed: () -> Unit,
    ) : BaseMPVView(context, null) {
        init {
            isFocusable = false
            isFocusableInTouchMode = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            isFocusable = false
            isFocusableInTouchMode = false
        }

        override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean = false

        override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean = false

        override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean = false
        override fun initOptions() {
            setVo("gpu")
            MPVLib.setOptionString("profile", "fast")
            MPVLib.setOptionString("gpu-context", "android")
            MPVLib.setOptionString("opengl-es", "yes")

            val hardwareDecoding = settings?.hardwareDecoding ?: true
            if (hardwareDecoding) {
                MPVLib.setOptionString("hwdec", "mediacodec,mediacodec-copy")
                MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
            } else {
                MPVLib.setOptionString("hwdec", "no")
            }

            val audioPassthrough = settings?.audioPassthrough ?: false
            if (audioPassthrough) {
                MPVLib.setOptionString("audio-spdif", "ac3,dts,eac3,truehd,dts-hd")
            } else {
                MPVLib.setOptionString("audio-spdif", "")
            }

            val surroundSound = settings?.surroundSound ?: false
            if (surroundSound) {
                MPVLib.setOptionString("audio-channels", "auto-safe")
            } else {
                MPVLib.setOptionString("audio-channels", "stereo")
            }

            MPVLib.setOptionString("ao", "audiotrack,opensles")
            MPVLib.setOptionString("audio-set-media-role", "yes")
            MPVLib.setOptionString("tls-verify", "yes")
            MPVLib.setOptionString("tls-ca-file", "${context.filesDir.absolutePath}/cacert.pem")
            MPVLib.setOptionString("input-default-bindings", "no")
            MPVLib.setOptionString("input-vo-keyboard", "no")
            MPVLib.setOptionString("osc", "no")
            MPVLib.setOptionString("demuxer-max-bytes", "${64 * 1024 * 1024}")
            MPVLib.setOptionString("demuxer-max-back-bytes", "${64 * 1024 * 1024}")
        }

        override fun postInitOptions() {
            MPVLib.setOptionString("save-position-on-quit", "no")
        }

        override fun observeProperties() {
            MPVLib.observeProperty("time-pos/full", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("duration/full", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("eof-reached", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("speed", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("sid", MPVLib.MpvFormat.MPV_FORMAT_STRING)
            MPVLib.observeProperty("aid", MPVLib.MpvFormat.MPV_FORMAT_STRING)
            MPVLib.observeProperty("track-list", MPVLib.MpvFormat.MPV_FORMAT_NONE)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            super.surfaceCreated(holder)
            onSurfaceReady()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            onSurfaceDestroyed()
            super.surfaceDestroyed(holder)
        }
    }
}

private fun copyMpvAssets(context: Context) {
    val outFile = File(context.filesDir, "cacert.pem")
    val assetManager = context.assets
    assetManager.open("cacert.pem").use { input ->
        if (outFile.exists() && outFile.length() == input.available().toLong()) {
            return
        }
        outFile.outputStream().use { output -> input.copyTo(output) }
    }
}
