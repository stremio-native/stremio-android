@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.stremio.mobile.presentation.screens

import android.os.Build
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import org.json.JSONObject
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.AccentGreen
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.data.model.LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA
import com.stremio.mobile.data.model.LiquidGlassTuning
import com.stremio.mobile.player.LanguageCatalog
import com.stremio.mobile.player.PlayerResizeMode
import com.stremio.mobile.player.PlayerRuntimeState
import com.stremio.mobile.player.PlayerSubtitleStyle
import com.stremio.mobile.player.PlayerTrackOption
import com.stremio.mobile.presentation.screens.player.ClassicPlayerControls
import com.stremio.mobile.presentation.screens.player.ModernPlayerControls
import com.stremio.mobile.presentation.screens.player.PlayerControlsActions
import com.stremio.mobile.presentation.screens.player.PlayerControlsState
import com.stremio.mobile.presentation.screens.player.PlayerGestureOverlays
import com.stremio.mobile.presentation.screens.player.playerGestures
import com.stremio.mobile.presentation.screens.player.rememberPlayerGestureState
import com.stremio.mobile.presentation.components.LocalGlassAlpha
import com.stremio.mobile.presentation.components.LocalGlobalUiTheme
import com.stremio.mobile.presentation.components.GlobalUiTheme
import com.stremio.mobile.presentation.components.LocalGlobalBackdrop
import com.stremio.mobile.presentation.components.ThemedButton
import com.stremio.mobile.presentation.components.ThemedTextButton
import com.stremio.mobile.presentation.components.ThemedIconButton
import com.stremio.mobile.presentation.components.LocalIsTv
import com.stremio.mobile.presentation.components.TvRequestFocus
import com.stremio.mobile.presentation.components.disableTvFocus
import com.stremio.mobile.presentation.components.requestTvFocusSafely
import com.stremio.mobile.presentation.components.tvFocusIndicator
import com.stremio.mobile.presentation.components.tvListItemSpacing

@Composable
fun PlayerScreen(
    player: com.stremio.mobile.player.Player?,
    activeUri: String?,
    title: String,
    onAttachView: (View) -> Unit,
    onDetachView: () -> Unit,
    onBack: () -> Unit,
    getSubtitlePrefs: () -> Pair<Int, Int> = { 100 to 0 },
    onSubtitlePrefsChanged: (sizePercent: Int, offsetPercent: Int) -> Unit = { _, _ -> },
    profileSettings: com.stremio.core.types.profile.Profile.Settings? = null,
    getPlayerStreamState: () -> com.stremio.core.models.Player.StreamState? = { null },
    onAudioTrackSelected: (PlayerTrackOption) -> Unit = {},
    onSubtitleTrackSelected: (PlayerTrackOption) -> Unit = {},
    onSubtitlesDisabled: () -> Unit = {},
    onSubtitleStyleChanged: (PlayerSubtitleStyle) -> Unit = {},
    onLocalSubtitlePicked: (Uri) -> Unit = {},
    nextVideo: com.stremio.core.types.resource.Video? = null,
    showNextVideoPopup: Boolean = false,
    onDismissNextVideoPopup: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onTick: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onSeekReported: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onPausedChanged: (Boolean) -> Unit = {},
    onEnded: () -> Unit = {},
    showNoSeedsBanner: Boolean = false,
    noSeedsReason: String? = null,
    onPlayNextBestStream: () -> Unit = {},
    globalUiStyle: String = "classic",
    glassEffectsMode: String = "balanced",
    globalGlassAlpha: Float = LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA,
    adaptiveGlassContrast: Boolean = true,
    glassHapticsEnabled: Boolean = true,
    hapticsIntensity: String = "Medium",
    liquidGlassTuning: LiquidGlassTuning = LiquidGlassTuning(),
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTv.current
    val playerSurfaceFocus = remember { FocusRequester() }
    val playButtonFocus = remember { FocusRequester() }

    val runtimeStateHolder = player?.runtimeState?.collectAsState()
        ?: remember { mutableStateOf(PlayerRuntimeState()) }
    val runtimeState = runtimeStateHolder.value
    val initialSubtitlePrefs = remember { getSubtitlePrefs() }
    val initialStreamState = remember(activeUri, player) { getPlayerStreamState() }

    // Player State
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var bufferedPositionMs by remember { mutableStateOf(0L) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var hasReportedEnded by remember { mutableStateOf(false) }

    var subtitleSizePercent by remember(activeUri, player) {
        mutableIntStateOf(initialStreamState?.subtitleSize?.roundToInt() ?: profileSettings?.subtitlesSize ?: initialSubtitlePrefs.first)
    }
    var subtitleOffsetPercent by remember(activeUri, player) {
        mutableIntStateOf(initialStreamState?.subtitleOffset?.roundToInt() ?: profileSettings?.subtitlesOffset ?: initialSubtitlePrefs.second)
    }
    var subtitleDelayMs by remember(activeUri, player) {
        mutableLongStateOf(initialStreamState?.subtitleDelay ?: 0L)
    }
    val subtitleStyle = remember(
        subtitleSizePercent,
        subtitleOffsetPercent,
        subtitleDelayMs,
        profileSettings?.subtitlesTextColor,
        profileSettings?.subtitlesBackgroundColor,
        profileSettings?.subtitlesOutlineColor,
        profileSettings?.assSubtitlesStyling,
    ) {
        PlayerSubtitleStyle(
            sizePercent = subtitleSizePercent.coerceIn(75, 250),
            offsetPercent = subtitleOffsetPercent.coerceIn(0, 100),
            delayMs = subtitleDelayMs,
            textColor = profileSettings?.subtitlesTextColor ?: "#FFFFFF",
            backgroundColor = profileSettings?.subtitlesBackgroundColor ?: "#00000000",
            outlineColor = profileSettings?.subtitlesOutlineColor ?: "#000000",
            assStyling = profileSettings?.assSubtitlesStyling ?: true,
        )
    }

    // UI Configuration
    var resizeMode by remember { mutableStateOf(PlayerResizeMode.FIT) }
    var showControls by remember { mutableStateOf(true) }
    var lastActivityTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val coroutineScope = rememberCoroutineScope()
    var singleTapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var suppressPlayerExitUntilMs by remember { mutableStateOf(0L) }
    var hardwareBackWillExit by remember { mutableStateOf(false) }

    fun revealPlayerOverlay() {
        val now = System.currentTimeMillis()
        lastActivityTime = now
        showControls = true
        suppressPlayerExitUntilMs = now + 500L
        hardwareBackWillExit = true
    }
    // Timestamp of the last gesture-driven (silent) seek. Used to ignore the pause→buffer→resume
    // playback transitions it triggers, so a swipe/double-tap seek never wakes the controls.
    var lastSilentSeekMs by remember { mutableStateOf(0L) }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val localSubtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onLocalSubtitlePicked)
    }

    // Gesture state lives in its own holder (PlayerGestures.kt) so that background player-state
    // changes (buffering, play/pause) can't resurface the controls overlay mid-gesture.
    val gestureState = rememberPlayerGestureState()
    // isMuted / lastVolume are shared with the controls' mute button, so they stay here.
    var isMuted by remember { mutableStateOf(false) }
    val initialVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    var lastVolume by remember { mutableIntStateOf(if (initialVol > 0) initialVol else (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.3f).toInt()) }

    // Orientation & Status Bar visibility management
    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity?.window
        
        // Force landscape playback
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Hide system/status bars
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    // Dialogs / Track selectors
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var audioDefaultApplied by remember(activeUri, player) { mutableStateOf(false) }
    var subtitleDefaultApplied by remember(activeUri, player) { mutableStateOf(false) }

    // Stats Panel
    var showStatsPanel by remember { mutableStateOf(false) }
    var torrentStats by remember { mutableStateOf<JSONObject?>(null) }

    BackHandler {
        when {
            showAudioDialog -> showAudioDialog = false
            showSubtitleDialog -> showSubtitleDialog = false
            showStatsPanel -> showStatsPanel = false
            !showControls || !hardwareBackWillExit -> revealPlayerOverlay()
            System.currentTimeMillis() < suppressPlayerExitUntilMs -> Unit
            else -> onBack()
        }
    }

    LaunchedEffect(showControls) {
        if (!showControls) {
            hardwareBackWillExit = false
        }
    }

    val infoHash = remember(activeUri) {
        activeUri?.let { uri ->
            Regex("/(?:stream/)?([a-fA-F0-9]{40})").find(uri)?.groupValues?.get(1)
        }
    }

    // Activity Timer Reset Helper. While a gesture is in progress the controls must stay hidden
    // (VLC-style), so background callers (e.g. buffering) can't resurface the overlay mid-gesture.
    fun resetActivityTimer() {
        if (gestureState.isGestureActive) return
        lastActivityTime = System.currentTimeMillis()
        showControls = true
    }

    // Seek helper
    fun seekTo(pos: Long) {
        positionMs = pos
        player?.seekTo(pos)
        onSeekReported(pos, durationMs)
        resetActivityTimer()
    }

    // Same as seekTo, but for gesture-driven seeks (double-tap, swipe) that must not bring up
    // the persistent controls overlay — only their own transient indicator should show.
    fun seekToSilently(pos: Long) {
        positionMs = pos
        player?.seekTo(pos)
        onSeekReported(pos, durationMs)
        lastSilentSeekMs = System.currentTimeMillis()
    }

    LaunchedEffect(player, activeUri) {
        val state = player?.runtimeState?.value ?: PlayerRuntimeState()
        isPlaying = state.isPlaying
        isBuffering = state.isBuffering
        positionMs = state.positionMs
        durationMs = state.durationMs
        bufferedPositionMs = state.bufferedPositionMs
        currentSpeed = state.speed
        hasReportedEnded = false
        playbackError = state.error
        showControls = true
        lastActivityTime = System.currentTimeMillis()
        gestureState.reset()
        showAudioDialog = false
        showSubtitleDialog = false
        showStatsPanel = false
        torrentStats = null
    }

    LaunchedEffect(runtimeState) {
        val wasPlaying = isPlaying
        isPlaying = runtimeState.isPlaying
        isBuffering = runtimeState.isBuffering
        playbackError = runtimeState.error
        positionMs = runtimeState.positionMs
        durationMs = runtimeState.durationMs
        bufferedPositionMs = runtimeState.bufferedPositionMs
        currentSpeed = runtimeState.speed
        // Wake the controls only when playback settles into a real pause — never while buffering,
        // never on resume. A gesture seek causes pause→buffer→resume transitions (and double-tap
        // seek doesn't even set isGestureActive), so those must not wake the controls. The grace
        // window after a silent seek also covers the brief race before isBuffering flips true.
        val settledPause = !runtimeState.isPlaying && wasPlaying && !runtimeState.isBuffering
        val sinceSilentSeekMs = System.currentTimeMillis() - lastSilentSeekMs
        if (settledPause && sinceSilentSeekMs > 1500) {
            resetActivityTimer()
        }
        if (runtimeState.ended && !hasReportedEnded) {
            hasReportedEnded = true
            onEnded()
        }
    }

    LaunchedEffect(player, activeUri, runtimeState.audioTracks, profileSettings?.audioLanguage) {
        if (player == null || audioDefaultApplied || runtimeState.audioTracks.isEmpty()) return@LaunchedEffect
        val savedTrackId = getPlayerStreamState()?.audioTrack?.id
        val savedTrack = savedTrackId?.let { id -> runtimeState.audioTracks.firstOrNull { it.id == id } }
        val defaultTrack = savedTrack ?: runtimeState.audioTracks.firstOrNull { track ->
            LanguageCatalog.matches(track.languageCode ?: track.language, profileSettings?.audioLanguage)
        }
        if (defaultTrack != null) {
            player.selectAudioTrack(defaultTrack.id)
            audioDefaultApplied = true
        }
    }

    LaunchedEffect(player, activeUri, runtimeState.subtitleTracks, profileSettings?.subtitlesLanguage, profileSettings?.subtitlesAutoSelect) {
        if (player == null || subtitleDefaultApplied) return@LaunchedEffect
        val settingsLanguage = profileSettings?.subtitlesLanguage
        val autoSelect = profileSettings?.subtitlesAutoSelect ?: true
        if (settingsLanguage == null || !autoSelect) {
            player.disableSubtitles()
            subtitleDefaultApplied = true
            return@LaunchedEffect
        }
        if (runtimeState.subtitleTracks.isEmpty()) return@LaunchedEffect

        val savedTrack = getPlayerStreamState()?.subtitleTrack
        val savedById = savedTrack?.id?.let { id ->
            runtimeState.subtitleTracks.firstOrNull { track ->
                track.id == id && track.embedded == savedTrack.embedded
            }
        }
        val targetLanguage = savedTrack?.language ?: settingsLanguage
        val byLanguage = runtimeState.subtitleTracks
            .filter { track -> LanguageCatalog.matches(track.languageCode ?: track.language, targetLanguage) }
            .sortedWith(
                compareByDescending<PlayerTrackOption> { it.embedded }
                    .thenBy { subtitleOriginPriority(it) }
                    .thenBy { it.label }
            )
            .firstOrNull()

        val defaultTrack = savedById ?: byLanguage
        if (defaultTrack != null) {
            player.selectSubtitleTrack(defaultTrack.id)
            subtitleDefaultApplied = true
        }
    }

    LaunchedEffect(player, resizeMode, subtitleStyle) {
        player?.setResizeMode(resizeMode)
        player?.setSubtitleStyle(subtitleStyle)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player, profileSettings?.playInBackground) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                val playBg = profileSettings?.playInBackground ?: false
                if (!playBg) {
                    player?.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Continuous Position Tracker
    LaunchedEffect(player, isPlaying) {
        if (player == null || !isPlaying) return@LaunchedEffect
        while (true) {
            val state = player.runtimeState.value
            positionMs = state.positionMs
            bufferedPositionMs = state.bufferedPositionMs
            durationMs = state.durationMs
            onTick(positionMs, durationMs)
            delay(500)
        }
    }

    // Inactivity Auto-Hide Timer
    LaunchedEffect(showControls, isPlaying, lastActivityTime, isTv) {
        if (showControls && isPlaying) {
            val timeoutMs = if (isTv) 5000L else 3000L
            delay(timeoutMs)
            if (System.currentTimeMillis() - lastActivityTime >= timeoutMs) {
                showControls = false
            }
        }
    }

    LaunchedEffect(isTv, showControls, player, activeUri) {
        if (!isTv) return@LaunchedEffect
        withFrameNanos { }
        delay(64)
        if (showControls) {
            playButtonFocus.requestTvFocusSafely()
        } else {
            playerSurfaceFocus.requestTvFocusSafely()
        }
    }

    // Frame Rate Matching
    LaunchedEffect(
        activity,
        runtimeState.videoWidth,
        runtimeState.videoHeight,
        runtimeState.videoFrameRate,
        profileSettings?.frameRateMatchingStrategy
    ) {
        val currentActivity = activity ?: return@LaunchedEffect
        val strategy = profileSettings?.frameRateMatchingStrategy ?: com.stremio.core.types.profile.Profile.FrameRateMatchingStrategy.DISABLED
        if (strategy == com.stremio.core.types.profile.Profile.FrameRateMatchingStrategy.DISABLED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                currentActivity.window?.let { window ->
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = 0
                    }
                }
            }
            return@LaunchedEffect
        }

        val videoWidth = runtimeState.videoWidth
        val videoHeight = runtimeState.videoHeight
        val videoFps = runtimeState.videoFrameRate
        if (videoWidth <= 0 || videoHeight <= 0 || videoFps <= 0f) return@LaunchedEffect

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { currentActivity.display }.getOrNull()
            } else {
                @Suppress("DEPRECATION")
                currentActivity.windowManager?.defaultDisplay
            } ?: return@LaunchedEffect

            val supportedModes = display.supportedModes ?: return@LaunchedEffect
            val activeMode = display.mode ?: return@LaunchedEffect

            var bestMode: android.view.Display.Mode? = null
            var minFpsDiff = Float.MAX_VALUE
            var bestResolutionMatch = false

            for (mode in supportedModes) {
                val modeFps = mode.refreshRate
                val fpsDiff = kotlin.math.abs(modeFps - videoFps)
                val normalizedDiff = if (fpsDiff < 0.1f) 0f else fpsDiff
                
                val widthMatches = mode.physicalWidth == videoWidth
                val heightMatches = mode.physicalHeight == videoHeight
                val resolutionMatches = widthMatches && heightMatches

                if (strategy == com.stremio.core.types.profile.Profile.FrameRateMatchingStrategy.FRAME_RATE_AND_RESOLUTION) {
                    if (resolutionMatches) {
                        if (normalizedDiff < minFpsDiff) {
                            minFpsDiff = normalizedDiff
                            bestMode = mode
                            bestResolutionMatch = true
                        }
                    } else if (!bestResolutionMatch) {
                        if (normalizedDiff < minFpsDiff) {
                            minFpsDiff = normalizedDiff
                            bestMode = mode
                        }
                    }
                } else if (strategy == com.stremio.core.types.profile.Profile.FrameRateMatchingStrategy.FRAME_RATE_ONLY) {
                    val currentResolutionMatches = mode.physicalWidth == activeMode.physicalWidth && 
                                                   mode.physicalHeight == activeMode.physicalHeight
                    if (currentResolutionMatches) {
                        if (normalizedDiff < minFpsDiff) {
                            minFpsDiff = normalizedDiff
                            bestMode = mode
                        }
                    }
                }
            }

            bestMode?.let { matchedMode ->
                if (matchedMode.modeId != activeMode.modeId) {
                    currentActivity.window?.let { window ->
                        window.attributes = window.attributes.apply {
                            preferredDisplayModeId = matchedMode.modeId
                        }
                    }
                }
            }
        }
    }

    // Live Torrent Stats Polling
    LaunchedEffect(infoHash, showStatsPanel) {
        torrentStats = null
        if (infoHash == null || !showStatsPanel) {
            return@LaunchedEffect
        }
        while (true) {
            val statsStr = withContext(Dispatchers.IO) {
                fetchEngineStats(infoHash)
            }
            if (statsStr != null) {
                torrentStats = try { JSONObject(statsStr) } catch (e: Exception) { null }
            } else {
                torrentStats = null
            }
            delay(2000)
        }
    }

    val showFatalPlaybackError = playbackError != null && !isPlaying && !isBuffering
    val realPlayerGlassEnabled = globalUiStyle == "modern" && glassEffectsMode != "static"
    val controlsBackdrop = if (realPlayerGlassEnabled) {
        rememberLayerBackdrop {
            drawContent()
        }
    } else {
        null
    }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val volumeFraction = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val controlsState = PlayerControlsState(
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        positionMs = positionMs,
        durationMs = durationMs,
        bufferedPositionMs = bufferedPositionMs,
        currentSpeed = currentSpeed,
        resizeMode = resizeMode,
        title = title,
        showControls = showControls,
        hasInfoHash = infoHash != null,
        isStatsVisible = showStatsPanel,
        canSelectSubtitles = runtimeState.subtitleTracks.isNotEmpty(),
        canSelectAudio = runtimeState.audioTracks.isNotEmpty(),
        isMuted = isMuted,
        volumeFraction = volumeFraction,
    )
    val controlsActions = PlayerControlsActions(
        onBack = onBack,
        onPlayPause = {
            val willPause = isPlaying
            if (willPause) player?.pause() else player?.play()
            onPausedChanged(willPause)
            resetActivityTimer()
        },
        onSeekTo = { seekTo(it) },
        onSkipForward = {
            val seekDur = profileSettings?.seekTimeDuration ?: 10000L
            val newPos = (positionMs + seekDur).coerceAtMost(durationMs)
            seekTo(newPos)
            gestureState.showRightSeekIndicator = true
        },
        onSkipBack = {
            val seekDur = profileSettings?.seekTimeDuration ?: 10000L
            val newPos = (positionMs - seekDur).coerceAtLeast(0)
            seekTo(newPos)
            gestureState.showLeftSeekIndicator = true
        },
        onCycleSpeed = {
            val currentIndex = speedOptions
                .indexOfFirst { abs(it - currentSpeed) < 0.01f }
                .let { if (it >= 0) it else speedOptions.indexOf(1.0f) }
            val nextSpeed = speedOptions[(currentIndex + 1) % speedOptions.size]
            currentSpeed = nextSpeed
            player?.setPlaybackSpeed(nextSpeed)
            resetActivityTimer()
        },
        onCycleAspect = {
            resizeMode = when (resizeMode) {
                PlayerResizeMode.FIT -> PlayerResizeMode.STRETCH
                PlayerResizeMode.STRETCH -> PlayerResizeMode.ZOOM
                PlayerResizeMode.ZOOM -> PlayerResizeMode.FIT
            }
            resetActivityTimer()
        },
        onToggleMute = {
            if (isMuted) {
                val restoreVol = if (lastVolume > 0) lastVolume else (maxVolume * 0.3f).toInt().coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVol, 0)
                isMuted = false
            } else {
                lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                isMuted = true
            }
            resetActivityTimer()
        },
        onShowSubtitles = {
            showSubtitleDialog = true
            resetActivityTimer()
        },
        onShowAudio = {
            showAudioDialog = true
            resetActivityTimer()
        },
        onToggleStats = {
            showStatsPanel = !showStatsPanel
            resetActivityTimer()
        },
    )

    val globalTheme = remember(
        globalUiStyle,
        glassEffectsMode,
        globalGlassAlpha,
        adaptiveGlassContrast,
        glassHapticsEnabled,
        hapticsIntensity,
        liquidGlassTuning,
    ) {
        GlobalUiTheme(
            style = globalUiStyle,
            glassEffectsMode = glassEffectsMode,
            glassAlpha = globalGlassAlpha,
            adaptiveGlassContrast = adaptiveGlassContrast,
            hapticsEnabled = glassHapticsEnabled,
            hapticsIntensity = hapticsIntensity,
            liquidGlassTuning = liquidGlassTuning,
        )
    }

    CompositionLocalProvider(
        LocalGlobalUiTheme provides globalTheme,
        LocalGlassAlpha provides globalGlassAlpha,
        LocalGlobalBackdrop provides controlsBackdrop
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(
                    if (isTv) {
                        Modifier
                            .focusGroup()
                            .focusProperties {
                                canFocus = !showControls
                                exit = { FocusRequester.Cancel }
                            }
                            .then(
                                if (!showControls) {
                                    Modifier
                                        .focusRequester(playerSurfaceFocus)
                                        .focusable()
                                } else {
                                    Modifier
                                }
                            )
                            .onPreviewKeyEvent { event ->
                                val keyCode = event.nativeKeyEvent.keyCode
                                val isBack = keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                                    keyCode == android.view.KeyEvent.KEYCODE_ESCAPE ||
                                    event.key == Key.Back ||
                                    event.key == Key.Escape
                                if (isBack) {
                                    false
                                } else if (event.type != KeyEventType.KeyDown) {
                                    false
                                } else {
                                    resetActivityTimer()
                                    if (showControls) {
                                        false
                                    } else when (keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER,
                                        android.view.KeyEvent.KEYCODE_SPACE,
                                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> true

                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            controlsActions.onSkipBack()
                                            true
                                        }

                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            controlsActions.onSkipForward()
                                            true
                                        }

                                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> true

                                        android.view.KeyEvent.KEYCODE_MEDIA_STOP -> {
                                            onBack()
                                            true
                                        }

                                        else -> false
                                    }
                                }
                            }
                    } else {
                        Modifier
                    }
                ),
        ) {
            // Backend-provided player view.
            key(player, activeUri) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (controlsBackdrop != null) Modifier.layerBackdrop(controlsBackdrop) else Modifier)
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusProperties { canFocus = false },
                        factory = { context ->
                            (player?.createView(context) ?: View(context)).apply {
                                keepScreenOn = true
                                if (isTv) {
                                    disableTvFocus()
                                }
                                onAttachView(this)
                            }
                        },
                        update = { view ->
                            if (isTv) {
                                view.disableTvFocus()
                            }
                            player?.setResizeMode(resizeMode)
                            player?.setSubtitleStyle(subtitleStyle)
                        }
                    )
                }
            }

        DisposableEffect(player, activeUri) {
            onDispose { onDetachView() }
        }

        // Tap / swipe gesture layer. Detection + the transient HUDs live in PlayerGestures.kt;
        // player-owned actions are routed back through callbacks so the gesture layer never
        // touches showControls directly (VLC-style decoupling).
        if (!isTv) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .playerGestures(
                    state = gestureState,
                    audioManager = audioManager,
                    activity = activity,
                    context = context,
                    position = { positionMs },
                    duration = { durationMs },
                    onGestureStart = {
                        // A drag started: drop the controls and cancel any pending tap toggle so a
                        // quick flick can't later flash the overlay back on.
                        singleTapJob?.cancel()
                        singleTapJob = null
                        showControls = false
                    },
                    onSingleTap = {
                        singleTapJob?.cancel()
                        singleTapJob = coroutineScope.launch {
                            delay(250)
                            if (showControls) {
                                showControls = false
                            } else {
                                resetActivityTimer()
                            }
                            singleTapJob = null
                        }
                    },
                    onDoubleTapSeek = { forward ->
                        singleTapJob?.cancel()
                        singleTapJob = null
                        showControls = false
                        val seekDur = profileSettings?.seekTimeDuration ?: 10000L
                        val newPos = if (forward) {
                            (positionMs + seekDur).coerceAtMost(durationMs)
                        } else {
                            (positionMs - seekDur).coerceAtLeast(0)
                        }
                        seekToSilently(newPos)
                    },
                    onSeekCommit = { seekToSilently(it) },
                    key1 = player,
                    key2 = activeUri,
                )
        )
        }

        // Playback error overlay — shown when the codec/source fails so it's visible instead
        // of an infinite buffering spinner, with a one-tap retry.
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = "Couldn't play this stream",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Text(
                        text = playbackError ?: "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemedButton(text = "Back", onClick = onBack)
                        ThemedButton(
                            text = "Retry",
                            onClick = {
                                playbackError = null
                                player?.retry()
                            },
                        )
                    }
                }
            }
        }

        if (playbackError != null && !showFatalPlaybackError) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD9141422))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = playbackError ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Floating Stats Overlay Panel
        if (showStatsPanel && torrentStats != null) {
            // Full-screen backdrop detector to close the stats panel when clicking outside
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            showStatsPanel = false
                        }
                    }
            ) {
                val stats = torrentStats!!
                val downSpeed = stats.optDouble("downloadSpeed", 0.0)
                val upSpeed = stats.optDouble("uploadSpeed", 0.0)
                val activePeers = stats.optLong("peers", 0L)
                val swarmSize = stats.optLong("swarmSize", 0L)
                val totalDown = stats.optLong("downloaded", 0L)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 80.dp, end = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC101018))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                // Consume taps inside the stats card so they do not propagate to the backdrop and close it
                            }
                        }
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Torrent Statistics",
                            color = AccentPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.width(160.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Text("Download", color = Color.Gray, fontSize = 12.sp)
                            Text(formatSpeed(downSpeed), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Text("Upload", color = Color.Gray, fontSize = 12.sp)
                            Text(formatSpeed(upSpeed), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Text("Peers", color = Color.Gray, fontSize = 12.sp)
                            Text("$activePeers / $swarmSize", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Text("Downloaded", color = Color.Gray, fontSize = 12.sp)
                            Text(formatBytes(totalDown), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Buffer Loading Overlay
        if (isBuffering) {
            CircularProgressIndicator(
                color = AccentPurple,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
            )
        }

        // Transient gesture HUDs — volume/brightness bar, swipe-seek preview, double-tap indicators.
        PlayerGestureOverlays(
            state = gestureState,
            modifier = Modifier.fillMaxSize(),
        )

        when (globalUiStyle) {
            "modern" -> {
                CompositionLocalProvider(LocalGlassAlpha provides globalGlassAlpha) {
                    ModernPlayerControls(
                        state = controlsState,
                        actions = controlsActions,
                        backdrop = controlsBackdrop,
                        glassEffectsMode = glassEffectsMode,
                        hapticsEnabled = glassHapticsEnabled,
                        hapticsIntensity = hapticsIntensity,
                        playFocusRequester = playButtonFocus,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            else -> ClassicPlayerControls(
                state = controlsState,
                actions = controlsActions,
                backdrop = controlsBackdrop,
                playFocusRequester = playButtonFocus,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // No Seeds / Too Slow Banner ΓÇö independent of showControls, opposite corner from the next-episode popup
        if (showNoSeedsBanner) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                NoSeedsBanner(
                    reason = noSeedsReason ?: "No seeds found for this stream.",
                    onPlayNextBestStream = onPlayNextBestStream,
                )
            }
        }

        // Next Episode Popup — shown near the end of an episode, independent of showControls, no animation
        if (showNextVideoPopup && nextVideo != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp)
            ) {
                NextEpisodePopup(
                    video = nextVideo,
                    onPlayNext = onPlayNext,
                    onDismiss = onDismissNextVideoPopup,
                )
            }
        }
    }
}

    // Audio Tracks Dialogue
    if (showAudioDialog) {
        AudioTracksDialog(
            tracks = runtimeState.audioTracks,
            onSelect = { track ->
                player?.selectAudioTrack(track.id)
                onAudioTrackSelected(track)
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    // Subtitle Tracks Dialogue
    if (showSubtitleDialog) {
        WebSubtitlesDialog(
            tracks = runtimeState.subtitleTracks,
            subtitlesDisabled = runtimeState.subtitlesDisabled,
            subtitlesLanguage = profileSettings?.subtitlesLanguage,
            interfaceLanguage = profileSettings?.interfaceLanguage,
            subtitleStyle = subtitleStyle,
            onSelectTrack = { track ->
                player?.selectSubtitleTrack(track.id)
                onSubtitleTrackSelected(track)
                showSubtitleDialog = false
            },
            onDisableSubtitles = {
                player?.disableSubtitles()
                onSubtitlesDisabled()
                showSubtitleDialog = false
            },
            onSubtitleSizeChange = { size ->
                subtitleSizePercent = size
                onSubtitleStyleChanged(subtitleStyle.copy(sizePercent = size))
            },
            onSubtitleOffsetChange = { offset ->
                subtitleOffsetPercent = offset
                onSubtitleStyleChanged(subtitleStyle.copy(offsetPercent = offset))
            },
            onSubtitleDelayChange = { delay ->
                subtitleDelayMs = delay
                onSubtitleStyleChanged(subtitleStyle.copy(delayMs = delay))
            },
            onImportLocalSubtitle = {
                localSubtitleLauncher.launch(
                    arrayOf(
                        "application/x-subrip",
                        "text/vtt",
                        "text/plain",
                        "application/octet-stream",
                    )
                )
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }
}

@Composable
fun NoSeedsBanner(
    reason: String,
    onPlayNextBestStream: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF2141422))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFFFC66D),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = reason,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        ThemedButton(
            text = "Try Next Best Stream",
            onClick = onPlayNextBestStream,
            containerColor = AccentPurple,
        )
    }
}

@Composable
fun NextEpisodePopup(
    video: com.stremio.core.types.resource.Video,
    onPlayNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF2141422))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A26)),
        ) {
            if (video.thumbnail != null) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val seriesInfo = video.seriesInfo
            Text(
                text = if (seriesInfo != null) "Next: S${seriesInfo.season}E${seriesInfo.episode}" else "Next Episode",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ThemedIconButton(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Dismiss",
            onClick = onDismiss,
            modifier = Modifier.size(36.dp),
        )
        ThemedIconButton(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play next episode",
            onClick = onPlayNext,
            modifier = Modifier
                .size(36.dp),
            selected = true,
        )
    }
}

@Composable
fun AudioTracksDialog(
    tracks: List<PlayerTrackOption>,
    onSelect: (PlayerTrackOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val surfaceColor = themedSurfaceColor()
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Audio Tracks",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(8.dp)),
                    contentPadding = playerListContentPadding(),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(tracks) { track ->
                        TrackInfoRow(
                            title = LanguageCatalog.label(context, track.languageCode ?: track.language),
                            subtitle = track.label,
                            isSelected = track.selected,
                            onClick = { onSelect(track) },
                        )
                    }
                }
                ThemedTextButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun WebSubtitlesDialog(
    tracks: List<PlayerTrackOption>,
    subtitlesDisabled: Boolean,
    subtitlesLanguage: String?,
    interfaceLanguage: String?,
    subtitleStyle: PlayerSubtitleStyle,
    onSelectTrack: (PlayerTrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
    onSubtitleSizeChange: (Int) -> Unit,
    onSubtitleOffsetChange: (Int) -> Unit,
    onSubtitleDelayChange: (Long) -> Unit,
    onImportLocalSubtitle: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val surfaceColor = themedSurfaceColor()
    val selectedTrack = tracks.firstOrNull { it.selected }
    val selectedLanguage = if (subtitlesDisabled) null else selectedTrack?.let { subtitleLanguageCode(it) }
    val subtitleLanguages = remember(tracks, subtitlesLanguage, interfaceLanguage, context) {
        val priority = listOfNotNull(
            LanguageCatalog.LOCAL_SUBTITLES_LANGUAGE,
            LanguageCatalog.toCode(subtitlesLanguage),
            LanguageCatalog.toCode(interfaceLanguage),
        ).distinct()
        tracks.mapNotNull { subtitleLanguageCode(it) }
            .distinct()
            .sortedWith(
                compareBy<String> { code ->
                    priority.indexOf(code).takeIf { it >= 0 } ?: Int.MAX_VALUE
                }.thenBy { code ->
                    LanguageCatalog.label(context, code).lowercase(java.util.Locale.ROOT)
                }
            )
    }
    val variants = remember(tracks, selectedLanguage) {
        if (selectedLanguage == null) {
            emptyList()
        } else {
            tracks.filter { subtitleLanguageCode(it) == selectedLanguage }
                .sortedWith(
                    compareBy<PlayerTrackOption> { subtitleOriginPriority(it) }
                        .thenBy { it.label.lowercase(java.util.Locale.ROOT) }
                )
        }
    }

    fun selectFirstForLanguage(language: String?) {
        if (language == null) {
            onDisableSubtitles()
            return
        }
        tracks.filter { subtitleLanguageCode(it) == language }
            .sortedWith(
                compareBy<PlayerTrackOption> { subtitleOriginPriority(it) }
                    .thenBy { it.label.lowercase(java.util.Locale.ROOT) }
            )
            .firstOrNull()
            ?.let(onSelectTrack)
    }

    fun copyText(label: String, value: String) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Subtitles",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    ThemedButton(
                        onClick = onImportLocalSubtitle,
                        containerColor = Color.Transparent,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileOpen,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Local", color = AccentPurple, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuHeader("Languages")
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(6.dp)),
                            contentPadding = playerListContentPadding(),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                TrackRow(
                                    label = "Off",
                                    isSelected = selectedLanguage == null,
                                    onClick = { selectFirstForLanguage(null) }
                                )
                            }
                            items(subtitleLanguages) { code ->
                                TrackRow(
                                    label = LanguageCatalog.label(context, code),
                                    isSelected = selectedLanguage == code,
                                    onClick = { selectFirstForLanguage(code) }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1.25f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuHeader("Variants")
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(6.dp)),
                            contentPadding = playerListContentPadding(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (variants.isEmpty()) {
                                item {
                                    Text(
                                        text = "Disabled",
                                        color = MutedText,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                items(variants) { track ->
                                    SubtitleVariantRow(
                                        track = track,
                                        onSelect = { onSelectTrack(track) },
                                        onOpenUrl = { url ->
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        },
                                        onCopyUrl = { url -> copyText("Subtitle URL", url) },
                                        onCopyId = { id -> copyText("Subtitle ID", id) },
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.width(210.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MenuHeader("Settings")
                        Stepper(
                            label = "DELAY",
                            value = subtitleStyle.delayMs / 1000f,
                            unit = "s",
                            step = 0.25f,
                            min = -10.0f,
                            max = 10.0f,
                            formatValue = { String.format(java.util.Locale.US, if (it >= 0) "+%.2f" else "%.2f", it) },
                            onChange = { onSubtitleDelayChange((it * 1000).roundToLong()) }
                        )
                        Stepper(
                            label = "SIZE",
                            value = subtitleStyle.sizePercent.toFloat(),
                            unit = "%",
                            step = 25f,
                            min = 75f,
                            max = 250f,
                            formatValue = { "${it.toInt()}" },
                            onChange = { onSubtitleSizeChange(it.roundToInt()) }
                        )
                        Stepper(
                            label = "POSITION",
                            value = subtitleStyle.offsetPercent.toFloat(),
                            unit = "%",
                            step = 1f,
                            min = 0f,
                            max = 100f,
                            formatValue = { "${it.toInt()}" },
                            onChange = { onSubtitleOffsetChange(it.roundToInt()) }
                        )
                    }
                }

                ThemedTextButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MenuHeader(label: String) {
    Text(
        text = label,
        color = Color.Gray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp)
    )
}

private val PlayerListRowShape = RoundedCornerShape(12.dp)

@Composable
private fun playerListContentPadding(): PaddingValues =
    if (LocalIsTv.current) PaddingValues(horizontal = 6.dp, vertical = 6.dp) else PaddingValues(0.dp)

@Composable
private fun PlayerSelectableRow(
    isSelected: Boolean,
    onClick: () -> Unit,
    minHeight: Dp,
    contentPadding: PaddingValues,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    val isTv = LocalIsTv.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val showFocus = isTv && focused
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(PlayerListRowShape)
            .background(
                when {
                    showFocus -> Color.White
                    isSelected -> Color(0x337457F2)
                    else -> Color.Transparent
                },
                PlayerListRowShape,
            )
            .then(
                if (isTv) {
                    Modifier.tvFocusIndicator(PlayerListRowShape, focusedScale = 1.06f)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = if (isTv) null else LocalIndication.current,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = { content(showFocus) },
    )
}

@Composable
private fun TrackInfoRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    PlayerSelectableRow(
        isSelected = isSelected,
        onClick = onClick,
        minHeight = 54.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) { focused ->
        val titleColor = when {
            focused -> Color.Black
            isSelected -> Color.White
            else -> Color(0xFFE2E2E6)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = if (focused) Color.Black.copy(alpha = 0.62f) else MutedText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (focused) Color.Black else AccentPurple,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun SubtitleVariantRow(
    track: PlayerTrackOption,
    onSelect: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onCopyId: (String) -> Unit,
) {
    val downloadUrl = track.fallbackUrl ?: track.url
    PlayerSelectableRow(
        isSelected = track.selected,
        onClick = onSelect,
        minHeight = 58.dp,
        contentPadding = PaddingValues(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) { focused ->
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.label,
                color = when {
                    focused -> Color.Black
                    track.selected -> Color.White
                    else -> Color(0xFFE2E2E6)
                },
                fontSize = 13.sp,
                fontWeight = if (track.selected || focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleOriginLabel(track),
                color = if (focused) Color.Black.copy(alpha = 0.62f) else MutedText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!track.embedded) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!downloadUrl.isNullOrBlank()) {
                    ThemedIconButton(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open subtitle URL",
                        onClick = { onOpenUrl(downloadUrl) },
                        modifier = Modifier.size(32.dp),
                        iconTint = if (focused) Color.Black else Color.White,
                    )
                    ThemedIconButton(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy subtitle URL",
                        onClick = { onCopyUrl(downloadUrl) },
                        modifier = Modifier.size(32.dp),
                        iconTint = if (focused) Color.Black else Color.White,
                    )
                }
                if (!track.addonSubtitleId.isNullOrBlank()) {
                    ThemedIconButton(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = "Copy subtitle ID",
                        onClick = { onCopyId(track.addonSubtitleId) },
                        modifier = Modifier.size(32.dp),
                        iconTint = if (focused) Color.Black else Color.White,
                    )
                }
            }
        }

        if (track.selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (focused) Color.Black else AccentPurple,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
            )
        }
    }
}

private fun subtitleLanguageCode(track: PlayerTrackOption): String? {
    if (track.local) return LanguageCatalog.LOCAL_SUBTITLES_LANGUAGE
    return track.languageCode ?: LanguageCatalog.toCode(track.language) ?: "und"
}

private fun subtitleOriginPriority(track: PlayerTrackOption): Int = when {
    track.local || track.origin.equals("LOCAL", ignoreCase = true) -> 0
    track.embedded || track.origin.equals("EMBEDDED", ignoreCase = true) -> 1
    track.exclusive || track.origin.equals("EXCLUSIVE", ignoreCase = true) -> 2
    else -> 3
}

private fun subtitleOriginLabel(track: PlayerTrackOption): String = when {
    track.local || track.origin.equals("LOCAL", ignoreCase = true) -> "LOCAL"
    track.embedded || track.origin.equals("EMBEDDED", ignoreCase = true) -> "EMBEDDED"
    track.exclusive || track.origin.equals("EXCLUSIVE", ignoreCase = true) -> "EXCLUSIVE"
    else -> track.origin
}

// Track Options helper data structure
data class TrackOption(
    val id: String,
    val label: String,
    val isSelected: Boolean
)

@Composable
fun TrackSelectorDialog(
    title: String,
    options: List<TrackOption>,
    hasNoneOption: Boolean = false,
    isNoneSelected: Boolean = false,
    onSelect: (TrackOption) -> Unit,
    onSelectNone: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val surfaceColor = themedSurfaceColor()
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(8.dp)),
                    contentPadding = playerListContentPadding(),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (hasNoneOption) {
                        item {
                            TrackRow(
                                label = "None",
                                isSelected = isNoneSelected,
                                onClick = onSelectNone
                            )
                        }
                    }

                    items(options) { option ->
                        TrackRow(
                            label = option.label,
                            isSelected = option.isSelected && (!hasNoneOption || !isNoneSelected),
                            onClick = { onSelect(option) }
                        )
                    }
                }

                ThemedTextButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun TrackRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    PlayerSelectableRow(
        isSelected = isSelected,
        onClick = onClick,
        minHeight = 48.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) { focused ->
        Text(
            text = label,
            color = when {
                focused -> Color.Black
                isSelected -> Color.White
                else -> Color(0xFFE2E2E6)
            },
            fontSize = 15.sp,
            fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (focused) Color.Black else AccentPurple,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp)
            )
        }
    }
}

// Bytes Formatting Helper
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Speed Formatting Helper
private fun formatSpeed(bytesPerSec: Double): String {
    val kb = bytesPerSec / 1024.0
    if (kb < 1024.0) {
        return String.format("%.1f KB/s", kb)
    }
    val mb = kb / 1024.0
    return String.format("%.1f MB/s", mb)
}

// Network fetch stats helper
private fun fetchEngineStats(infoHash: String): String? {
    return try {
        val url = URL("http://127.0.0.1:11470/$infoHash/stats.json")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 1500
        conn.readTimeout = 1500
        if (conn.responseCode == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun SubtitlesCustomizationDialog(
    options: List<TrackOption>,
    isNoneSelected: Boolean,
    subtitleSize: Float,
    subtitleOffset: Float,
    subtitleDelay: Float,
    onSubtitleSizeChange: (Float) -> Unit,
    onSubtitleOffsetChange: (Float) -> Unit,
    onSubtitleDelayChange: (Float) -> Unit,
    onSelect: (TrackOption) -> Unit,
    onSelectNone: () -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = themedSurfaceColor()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Subtitles Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tracks",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(6.dp)),
                            contentPadding = playerListContentPadding(),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                TrackRow(
                                    label = "None",
                                    isSelected = isNoneSelected,
                                    onClick = onSelectNone
                                )
                            }

                            items(options) { option ->
                                TrackRow(
                                    label = option.label,
                                    isSelected = option.isSelected && !isNoneSelected,
                                    onClick = { onSelect(option) }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color(0x22FFFFFF))
                    )

                    Column(
                        modifier = Modifier.width(200.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Customize",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Stepper(
                            label = "DELAY",
                            value = subtitleDelay,
                            unit = "s",
                            step = 0.25f,
                            min = -10.0f,
                            max = 10.0f,
                            formatValue = { String.format(java.util.Locale.US, if (it >= 0) "+%.2f" else "%.2f", it) },
                            onChange = onSubtitleDelayChange
                        )

                        Stepper(
                            label = "SIZE",
                            value = subtitleSize * 100f,
                            unit = "%",
                            step = 25f,
                            min = 75f,
                            max = 250f,
                            formatValue = { "${it.toInt()}" },
                            onChange = { onSubtitleSizeChange(it / 100f) }
                        )

                        Stepper(
                            label = "POSITION",
                            value = subtitleOffset * 100f,
                            unit = "%",
                            step = 1f,
                            min = 0f,
                            max = 100f,
                            formatValue = { "${it.toInt()}" },
                            onChange = { onSubtitleOffsetChange(it / 100f) }
                        )
                    }
                }

                ThemedTextButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun Stepper(
    label: String,
    value: Float?,
    unit: String,
    step: Float,
    min: Float? = null,
    max: Float? = null,
    disabled: Boolean = false,
    formatValue: (Float) -> String = { "${it.toInt()}" },
    onChange: (Float) -> Unit
) {
    val decreaseDisabled = disabled || value == null || (min != null && value <= min)
    val increaseDisabled = disabled || value == null || (max != null && value >= max)
    val valueLabel = if (disabled || value == null) "--" else formatValue(value) + unit

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(180.dp)
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1AFFFFFF))
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            ThemedIconButton(
                imageVector = Icons.Outlined.Remove,
                contentDescription = "Decrease",
                onClick = {
                    val current = value ?: return@ThemedIconButton
                    if (!disabled && (min == null || current > min)) {
                        onChange((current - step).coerceAtLeast(min ?: (current - step)))
                    }
                },
                enabled = !decreaseDisabled,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = valueLabel,
                color = if (disabled) Color.Gray else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(56.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            ThemedIconButton(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Increase",
                onClick = {
                    val current = value ?: return@ThemedIconButton
                    if (!disabled && (max == null || current < max)) {
                        onChange((current + step).coerceAtMost(max ?: (current + step)))
                    }
                },
                enabled = !increaseDisabled,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun themedSurfaceColor(): Color {
    val theme = LocalGlobalUiTheme.current
    return if (theme.style == "modern") {
        Color.White.copy(alpha = (theme.glassAlpha * 0.42f + 0.10f).coerceIn(0.10f, 0.36f))
    } else {
        GlassSurface
    }
}
