package com.stremio.mobile.presentation.screens.player

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.player.PlayerResizeMode
import com.stremio.mobile.presentation.components.LiquidGlassCard
import com.stremio.mobile.presentation.components.GlassLegibility
import com.stremio.mobile.presentation.components.LocalGlassLegibility
import com.stremio.mobile.presentation.components.StaticGlassCard
import com.stremio.mobile.presentation.components.LocalIsTv
import com.stremio.mobile.presentation.components.tvClickable
import com.stremio.mobile.presentation.components.tvFocusIndicator
import com.stremio.mobile.presentation.components.tvFocusTrap
import com.stremio.mobile.presentation.components.tvFocusVertical
import com.stremio.mobile.presentation.components.tvSliderFocusNavigation
import kotlin.math.abs

data class PlayerControlsState(
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedPositionMs: Long,
    val currentSpeed: Float,
    val resizeMode: PlayerResizeMode,
    val title: String,
    val showControls: Boolean,
    val hasInfoHash: Boolean,
    val isStatsVisible: Boolean,
    val canSelectSubtitles: Boolean,
    val canSelectAudio: Boolean,
    val isMuted: Boolean,
    val volumeFraction: Float,
)

class PlayerControlsActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onSkipForward: () -> Unit,
    val onSkipBack: () -> Unit,
    val onCycleSpeed: () -> Unit,
    val onCycleAspect: () -> Unit,
    val onToggleMute: () -> Unit,
    val onShowSubtitles: () -> Unit,
    val onShowAudio: () -> Unit,
    val onToggleStats: () -> Unit,
)

@Composable
fun ClassicPlayerControls(
    state: PlayerControlsState,
    actions: PlayerControlsActions,
    backdrop: LayerBackdrop?,
    playFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    if (state.showControls) {
        val isTv = LocalIsTv.current
        val backFocus = remember { FocusRequester() }
        val sliderFocus = remember { FocusRequester() }
        val bottomFocus = remember { FocusRequester() }
        Box(modifier = modifier.fillMaxSize().tvFocusTrap()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xE6050510), Color.Transparent)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xE6050510))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = actions.onBack,
                    modifier = Modifier
                        .then(if (isTv) Modifier.focusRequester(backFocus).tvFocusVertical(down = playFocusRequester) else Modifier)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .tvFocusIndicator(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = state.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!state.isBuffering || LocalIsTv.current) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    var skipBackFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = actions.onSkipBack,
                        modifier = Modifier
                            .size(if (isTv) 56.dp else 38.dp)
                            .then(if (isTv) Modifier.tvFocusVertical(up = backFocus, down = sliderFocus) else Modifier)
                            .onFocusChanged { skipBackFocused = it.isFocused || it.hasFocus }
                            .clip(CircleShape)
                            .background(if (skipBackFocused) Color.White else Color(0x66000000))
                            .tvFocusIndicator(CircleShape, focusedScale = 1.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = if (skipBackFocused) Color.Black else Color.White,
                            modifier = Modifier.size(if (isTv) 30.dp else 20.dp)
                        )
                    }

                    IconButton(
                        onClick = actions.onPlayPause,
                        modifier = Modifier
                            .size(56.dp)
                            .then(if (playFocusRequester != null) Modifier.focusRequester(playFocusRequester) else Modifier)
                            .then(if (isTv) Modifier.tvFocusVertical(up = backFocus, down = sliderFocus) else Modifier)
                            .clip(CircleShape)
                            .background(Color(0x4D000000))
                            .tvFocusIndicator(CircleShape)
                            .border(1.5.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    var skipForwardFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = actions.onSkipForward,
                        modifier = Modifier
                            .size(if (isTv) 56.dp else 38.dp)
                            .then(if (isTv) Modifier.tvFocusVertical(up = backFocus, down = sliderFocus) else Modifier)
                            .onFocusChanged { skipForwardFocused = it.isFocused || it.hasFocus }
                            .clip(CircleShape)
                            .background(if (skipForwardFocused) Color.White else Color(0x66000000))
                            .tvFocusIndicator(CircleShape, focusedScale = 1.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FastForward,
                            contentDescription = "Forward 10s",
                            tint = if (skipForwardFocused) Color.Black else Color.White,
                            modifier = Modifier.size(if (isTv) 30.dp else 20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                PlayerTimeline(
                    state = state,
                    actions = actions,
                    sliderFocus = if (isTv) sliderFocus else null,
                    upFocus = playFocusRequester,
                    downFocus = bottomFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = actions.onPlayPause,
                            modifier = Modifier
                                .size(44.dp)
                                .then(if (isTv) Modifier.focusRequester(bottomFocus).tvFocusVertical(up = sliderFocus) else Modifier)
                                .clip(RoundedCornerShape(12.dp))
                                .tvFocusIndicator(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = actions.onToggleMute,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .tvFocusIndicator(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = volumeIcon(state),
                                contentDescription = "Mute Toggle",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (state.hasInfoHash) {
                            IconButton(
                                onClick = actions.onToggleStats,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .tvFocusIndicator(RoundedCornerShape(12.dp))
                                    .background(if (state.isStatsVisible) AccentPurple else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Torrent Stats",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = actions.onCycleSpeed,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(44.dp).tvFocusIndicator(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = "Playback Speed",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.currentSpeed}x",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        PlayerIconButton(
                            imageVector = Icons.Outlined.Subtitles,
                            contentDescription = "Subtitles",
                            enabled = state.canSelectSubtitles,
                            onClick = actions.onShowSubtitles,
                        )

                        PlayerIconButton(
                            imageVector = Icons.Outlined.Audiotrack,
                            contentDescription = "Audio Tracks",
                            enabled = state.canSelectAudio,
                            onClick = actions.onShowAudio,
                        )

                        TextButton(
                            onClick = actions.onCycleAspect,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(44.dp).tvFocusIndicator(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = resizeLabel(state.resizeMode),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernPlayerControls(
    state: PlayerControlsState,
    actions: PlayerControlsActions,
    backdrop: LayerBackdrop?,
    glassEffectsMode: String,
    hapticsEnabled: Boolean = true,
    hapticsIntensity: String = "Medium",
    playFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    if (state.showControls) {
        val isTv = LocalIsTv.current
        val backFocus = remember { FocusRequester() }
        val sliderFocus = remember { FocusRequester() }
        CompositionLocalProvider(LocalGlassLegibility provides GlassLegibility.mediaOverlay()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .tvFocusTrap()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xE60A0A14), Color.Transparent, Color(0xE60A0A14))
                        )
                    )
            ) {
                ModernTopPill(
                    state = state,
                    actions = actions,
                    backdrop = backdrop,
                    glassEffectsMode = glassEffectsMode,
                    backFocus = if (isTv) backFocus else null,
                    downFocus = playFocusRequester,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                )

                if (!state.isBuffering || LocalIsTv.current) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpringGlassIconButton(
                            imageVector = Icons.Outlined.FastRewind,
                            contentDescription = "Rewind 10s",
                            backdrop = backdrop,
                            glassEffectsMode = glassEffectsMode,
                            size = if (isTv) 52 else 38,
                            modifier = if (isTv) Modifier.tvFocusVertical(up = backFocus, down = sliderFocus) else Modifier,
                            onClick = {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(
                                        if (hapticsIntensity == "Light") HapticFeedbackType.TextHandleMove
                                        else HapticFeedbackType.LongPress
                                    )
                                }
                                actions.onSkipBack()
                            },
                        )

                    val glowColor = if (state.isPlaying) Color(0x338B5CF6) else Color(0x3310B981)
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(glowColor, Color.Transparent),
                                        center = center,
                                        radius = size.width / 2
                                    ),
                                    radius = size.width / 2
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PlayerGlassCard(
                            backdrop = backdrop,
                            glassEffectsMode = glassEffectsMode,
                            cornerRadius = 999.dp,
                            modifier = Modifier.size(58.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .then(if (playFocusRequester != null) Modifier.focusRequester(playFocusRequester) else Modifier)
                                    .then(if (isTv) Modifier.tvFocusVertical(up = backFocus, down = sliderFocus) else Modifier)
                                    .clip(CircleShape)
                                    .background(Color(0x16000000))
                                    .tvFocusIndicator(CircleShape)
                                    .clickable(onClick = {
                                        if (hapticsEnabled) {
                                            haptic.performHapticFeedback(
                                                if (hapticsIntensity == "Light") HapticFeedbackType.TextHandleMove
                                                else HapticFeedbackType.LongPress
                                            )
                                        }
                                        actions.onPlayPause()
                                    }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }

                    SpringGlassIconButton(
                        imageVector = Icons.Outlined.FastForward,
                        contentDescription = "Forward 10s",
                        backdrop = backdrop,
                        glassEffectsMode = glassEffectsMode,
                        size = if (isTv) 52 else 38,
                        modifier = if (isTv) Modifier.tvFocusVertical(up = backFocus, down = sliderFocus) else Modifier,
                        onClick = {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(
                                    if (hapticsIntensity == "Light") HapticFeedbackType.TextHandleMove
                                    else HapticFeedbackType.LongPress
                                )
                            }
                            actions.onSkipForward()
                        },
                    )
                    }
                }
    
                PlayerGlassCard(
                    backdrop = backdrop,
                    glassEffectsMode = glassEffectsMode,
                    cornerRadius = 30.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                ) {
                    // Just the scrubber here — keeping this card short so it can't creep down
                    // into the video's own subtitle row. Everything else (including Subtitles
                    // and Audio) lives in the top pill's overflow menu.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        ModernTimeline(
                            state = state,
                            actions = actions,
                            hapticsEnabled = hapticsEnabled,
                            hapticsIntensity = hapticsIntensity,
                            sliderFocus = if (isTv) sliderFocus else null,
                            upFocus = playFocusRequester,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernTopPill(
    state: PlayerControlsState,
    actions: PlayerControlsActions,
    backdrop: LayerBackdrop?,
    glassEffectsMode: String,
    modifier: Modifier = Modifier,
    backFocus: FocusRequester? = null,
    downFocus: FocusRequester? = null,
) {
    // iOS 27 Liquid Glass favors a quiet, content-led chrome: back + title + a single
    // overflow affordance. No clock/battery chip (the system status bar already owns that
    // when revealed) and no duplicate controls — every action lives in exactly one place.
    var moreExpanded by remember { mutableStateOf(false) }

    PlayerGlassCard(
        backdrop = backdrop,
        glassEffectsMode = glassEffectsMode,
        cornerRadius = 999.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = actions.onBack,
                modifier = Modifier
                    .size(40.dp)
                    .then(if (backFocus != null) Modifier.focusRequester(backFocus).tvFocusVertical(down = downFocus) else Modifier)
                    .tvFocusIndicator(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = state.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )

            Box {
                IconButton(
                    onClick = { moreExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .tvFocusVertical(down = downFocus)
                        .tvFocusIndicator(CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                    )
                }
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { moreExpanded = false },
                    modifier = Modifier.background(Color(0xEE141422)),
                ) {
                    if (state.canSelectSubtitles) {
                        PlayerMenuItem(
                            text = "Subtitles",
                            icon = Icons.Outlined.Subtitles,
                            onClick = {
                                moreExpanded = false
                                actions.onShowSubtitles()
                            },
                        )
                    }
                    if (state.canSelectAudio) {
                        PlayerMenuItem(
                            text = "Audio Tracks",
                            icon = Icons.Outlined.Audiotrack,
                            onClick = {
                                moreExpanded = false
                                actions.onShowAudio()
                            },
                        )
                    }
                    PlayerMenuItem(
                        text = "Speed: ${state.currentSpeed}x",
                        icon = Icons.Outlined.Speed,
                        onClick = {
                            moreExpanded = false
                            actions.onCycleSpeed()
                        },
                    )
                    PlayerMenuItem(
                        text = "Aspect: ${resizeLabel(state.resizeMode)}",
                        icon = Icons.Outlined.AspectRatio,
                        onClick = {
                            moreExpanded = false
                            actions.onCycleAspect()
                        },
                    )
                    PlayerMenuItem(
                        text = if (state.isMuted) "Unmute" else "Mute",
                        icon = volumeIcon(state),
                        onClick = {
                            moreExpanded = false
                            actions.onToggleMute()
                        },
                    )
                    if (state.hasInfoHash) {
                        PlayerMenuItem(
                            text = if (state.isStatsVisible) "Hide statistics" else "Show statistics",
                            icon = Icons.Outlined.Info,
                            onClick = {
                                moreExpanded = false
                                actions.onToggleStats()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTimeline(
    state: PlayerControlsState,
    actions: PlayerControlsActions,
    modifier: Modifier = Modifier,
    sliderFocus: FocusRequester? = null,
    upFocus: FocusRequester? = null,
    downFocus: FocusRequester? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatTime(state.positionMs),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center,
        )

        val duration = state.durationMs.coerceAtLeast(1L)
        val progress = (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
        val bufferedProgress = (state.bufferedPositionMs.toFloat() / duration).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .weight(1f)
                .height(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            TimelineTrack(bufferedProgress = bufferedProgress, modifier = Modifier.fillMaxWidth())
            Slider(
                value = progress,
                onValueChange = { actions.onSeekTo((it * duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AccentPurple,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (sliderFocus != null) Modifier.focusRequester(sliderFocus) else Modifier)
                    .tvFocusVertical(up = upFocus, down = downFocus)
                    .tvSliderFocusNavigation(up = upFocus, down = downFocus)
                    .tvFocusIndicator(RoundedCornerShape(999.dp)),
            )
        }

        var remainingTimeMode by remember { mutableStateOf(false) }
        val rightTimeText = if (remainingTimeMode) {
            "-" + formatTime((state.durationMs - state.positionMs).coerceAtLeast(0L))
        } else {
            formatTime(state.durationMs)
        }

        Text(
            text = rightTimeText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .width(44.dp)
                .tvClickable { remainingTimeMode = !remainingTimeMode },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ModernTimeline(
    state: PlayerControlsState,
    actions: PlayerControlsActions,
    hapticsEnabled: Boolean = true,
    hapticsIntensity: String = "Medium",
    modifier: Modifier = Modifier,
    sliderFocus: FocusRequester? = null,
    upFocus: FocusRequester? = null,
) {
    val haptic = LocalHapticFeedback.current
    val duration = state.durationMs.coerceAtLeast(1L)
    val liveProgress = (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(liveProgress) }
    val displayProgress = if (isScrubbing) scrubProgress else liveProgress
    val displayPosition = (displayProgress * duration).toLong()

    val step = remember(duration) {
        when {
            duration < 5 * 60 * 1000L -> 5000L // 5 seconds
            duration < 30 * 60 * 1000L -> 15000L // 15 seconds
            duration < 120 * 60 * 1000L -> 60000L // 1 minute
            else -> 120000L // 2 minutes
        }
    }
    var lastTriggeredStep by remember { mutableStateOf(-1L) }

    LaunchedEffect(liveProgress, isScrubbing) {
        if (!isScrubbing) {
            lastTriggeredStep = displayPosition / step
        }
    }

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center,
            ) {
                TimelineTrack(
                    progress = displayProgress,
                    bufferedProgress = (state.bufferedPositionMs.toFloat() / duration).coerceIn(0f, 1f),
                    showThumb = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = displayProgress,
                    onValueChange = {
                        isScrubbing = true
                        scrubProgress = it
                        actions.onSeekTo((it * duration).toLong())

                        val currentStep = ((it * duration).toLong()) / step
                        if (currentStep != lastTriggeredStep) {
                            lastTriggeredStep = currentStep
                            if (hapticsEnabled && hapticsIntensity != "Light") {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (sliderFocus != null) Modifier.focusRequester(sliderFocus) else Modifier)
                        .tvFocusVertical(up = upFocus)
                        .tvSliderFocusNavigation(up = upFocus)
                        .tvFocusIndicator(RoundedCornerShape(999.dp)),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(displayPosition),
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "-" + formatTime((duration - displayPosition).coerceAtLeast(0L)),
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun TimelineTrack(
    bufferedProgress: Float,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    showThumb: Boolean = false,
) {
    Canvas(
        modifier = modifier
            .height(12.dp),
    ) {
        val trackHeight = 4.dp.toPx()
        val glowHeight = 8.dp.toPx()
        val thumbRadius = 10.dp.toPx()
        val startX = thumbRadius
        val trackWidth = size.width - (thumbRadius * 2)
        val centerY = size.height / 2
        // Modern "wave" gradient: a richer multi-stop sweep instead of a flat two-tone fill.
        val gradient = Brush.horizontalGradient(
            colors = listOf(AccentPurple, Color(0xFFC084FC), Color(0xFFFF8A65))
        )
        
        // Background track
        drawRoundRect(
            color = Color(0x26FFFFFF),
            topLeft = Offset(startX, centerY - trackHeight / 2),
            size = Size(trackWidth, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )
        
        // Buffer track
        drawRoundRect(
            color = Color(0x4DFFFFFF),
            topLeft = Offset(startX, centerY - trackHeight / 2),
            size = Size(trackWidth * bufferedProgress, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )

        if (progress > 0f) {
            // Glow track underlay (15% opacity, taller height)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AccentPurple.copy(alpha = 0.15f),
                        Color(0xFFC084FC).copy(alpha = 0.15f),
                        Color(0xFFFF8A65).copy(alpha = 0.15f),
                    )
                ),
                topLeft = Offset(startX, centerY - glowHeight / 2),
                size = Size(trackWidth * progress, glowHeight),
                cornerRadius = CornerRadius(glowHeight / 2),
            )

            // Active progress track
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(startX, centerY - trackHeight / 2),
                size = Size(trackWidth * progress, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2),
            )
        }

        if (showThumb) {
            val handleWidth = 18.dp.toPx()
            val handleHeight = 8.dp.toPx()
            val handleX = (startX + trackWidth * progress - handleWidth / 2)
                .coerceIn(startX - handleWidth / 2, startX + trackWidth - handleWidth / 2)

            drawRoundRect(
                color = Color.White.copy(alpha = 0.96f),
                topLeft = Offset(handleX, centerY - handleHeight / 2),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(handleHeight / 2),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset(handleX + handleWidth * 0.18f, centerY - handleHeight * 0.42f),
                size = Size(handleWidth * 0.64f, handleHeight * 0.18f),
                cornerRadius = CornerRadius(handleHeight * 0.09f),
            )
        }
    }
}

@Composable
private fun PlayerIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isTv = LocalIsTv.current
    var focused by remember { mutableStateOf(false) }
    val showFocus = isTv && focused
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clip(RoundedCornerShape(12.dp))
            .background(if (showFocus) Color.White else Color.Transparent)
            .tvFocusIndicator(RoundedCornerShape(12.dp)),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MutedText
                showFocus -> Color.Black
                else -> Color.White
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PlayerMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val isTv = LocalIsTv.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val showFocus = isTv && focused
    val contentColor = if (showFocus) Color.Black else Color.White
    DropdownMenuItem(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (showFocus) Color.White else Color.Transparent)
            .tvFocusIndicator(RoundedCornerShape(8.dp), focusedScale = 1.02f),
        text = {
            Text(
                text = text,
                color = contentColor,
                fontWeight = if (showFocus) FontWeight.Bold else FontWeight.Medium,
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = contentColor)
        },
        onClick = onClick,
        interactionSource = interactionSource,
    )
}

@Composable
private fun SpringGlassIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    backdrop: LayerBackdrop?,
    glassEffectsMode: String,
    size: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when {
            pressed -> 0.9f
            focused -> 1.16f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 650f),
        label = "glassIconScale",
    )
    PlayerGlassCard(
        backdrop = backdrop,
        glassEffectsMode = glassEffectsMode,
        cornerRadius = 999.dp,
        modifier = Modifier
            .size((if (focused) size + 10 else size).dp)
            .scale(scale),
    ) {
        Box(
            modifier = modifier
                .matchParentSize()
                .onFocusChanged { focused = it.isFocused || it.hasFocus }
                .clip(CircleShape)
                .background(if (focused) Color.White else Color.Transparent)
                .tvFocusIndicator(CircleShape, focusedScale = 1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size((size * 0.52f).dp),
            )
        }
    }
}

@Composable
private fun PlayerGlassCard(
    backdrop: LayerBackdrop?,
    glassEffectsMode: String,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (backdrop != null && glassEffectsMode != "static") {
        LiquidGlassCard(
            backdrop = backdrop,
            modifier = modifier,
            cornerRadius = cornerRadius,
            content = content,
        )
    } else {
        StaticGlassCard(
            modifier = modifier,
            cornerRadius = cornerRadius,
            content = content,
        )
    }
}


private fun volumeIcon(state: PlayerControlsState): ImageVector {
    return if (state.isMuted || state.volumeFraction == 0f) {
        Icons.AutoMirrored.Outlined.VolumeMute
    } else if (state.volumeFraction < 0.5f) {
        Icons.AutoMirrored.Outlined.VolumeDown
    } else {
        Icons.AutoMirrored.Outlined.VolumeUp
    }
}

private fun resizeLabel(mode: PlayerResizeMode): String = when (mode) {
    PlayerResizeMode.FIT -> "Fit"
    PlayerResizeMode.STRETCH -> "Stretch"
    PlayerResizeMode.ZOOM -> "Zoom"
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
