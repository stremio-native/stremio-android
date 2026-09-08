package com.stremio.mobile.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.presentation.navigation.AppView
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StremioBottomBar(
    selectedView: AppView,
    backdrop: LayerBackdrop?,
    onSelect: (AppView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val theme = LocalGlobalUiTheme.current
    val tuning = theme.liquidGlassTuning.clamped()
    val useRealGlass = theme.style == "modern" && theme.glassEffectsMode != "static" && backdrop != null
    val legibility = if (theme.style == "modern" && theme.adaptiveGlassContrast) {
        GlassLegibility.navigationBar()
    } else {
        GlassLegibility.Default
    }
    val barShape = RoundedCornerShape(30.dp)
    val indicatorShape = RoundedCornerShape(999.dp)
    val barSurfaceAlpha = (tuning.surfaceAlpha * legibility.surfaceAlphaBoost).coerceIn(0.08f, 0.90f)
    val barBorderAlpha = (0.14f * legibility.borderAlphaBoost).coerceIn(0.08f, 0.44f)
    val barShadowAlpha = (tuning.shadowAlpha * legibility.shadowAlphaBoost).coerceIn(0f, 0.70f)
    val selectedTabColor = if (theme.style == "modern") legibility.foreground else Color.White
    val unselectedTabColor = if (theme.style == "modern") legibility.mutedForeground else Color(0xFF9B96A8)
    val selectedIconColor = if (theme.style == "modern" && theme.adaptiveGlassContrast) {
        legibility.foreground
    } else {
        AccentPurple
    }

    var rawPointerOffset by remember { mutableStateOf(Offset.Unspecified) }
    var isBarPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var settleDragSelection by remember { mutableStateOf(false) }
    val hasPointer = rawPointerOffset != Offset.Unspecified

    val animatedPtrX by animateFloatAsState(
        targetValue = if (hasPointer) rawPointerOffset.x else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "barPtrX",
    )
    val animatedPtrY by animateFloatAsState(
        targetValue = if (hasPointer) rawPointerOffset.y else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "barPtrY",
    )
    val barPointer = if (hasPointer) Offset(animatedPtrX, animatedPtrY) else Offset.Unspecified

    val barScale by animateFloatAsState(
        targetValue = if (isBarPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "barScale",
    )

    BoxWithConstraints(
        modifier = modifier
            .scale(barScale)
            .fillMaxWidth(0.94f)
            .navigationBarsPadding()
            .padding(bottom = 10.dp)
            .height(74.dp)
            .pointerInput(theme.hapticsEnabled) {
                detectTapGestures(
                    onPress = { offset ->
                        rawPointerOffset = offset
                        isBarPressed = true
                        if (theme.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        tryAwaitRelease()
                        isBarPressed = false
                        if (!isDragging) {
                            rawPointerOffset = Offset.Unspecified
                        }
                    },
                )
            }
            .pointerInput(theme.hapticsEnabled) {
                detectDragGestures(
                    onDragStart = { offset ->
                        rawPointerOffset = offset
                        isBarPressed = true
                        isDragging = true
                        settleDragSelection = false
                        if (theme.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDrag = { change, _ ->
                        rawPointerOffset = change.position
                    },
                    onDragEnd = {
                        isBarPressed = false
                        isDragging = false
                        settleDragSelection = true
                        rawPointerOffset = Offset.Unspecified
                    },
                    onDragCancel = {
                        isBarPressed = false
                        isDragging = false
                        settleDragSelection = false
                        rawPointerOffset = Offset.Unspecified
                    },
                )
            },
    ) {
        val selectedIndex = AppView.entries.indexOf(selectedView).coerceAtLeast(0)
        val tabCount = AppView.entries.size
        val horizontalPadding = 8.dp
        val tabWidth = (maxWidth - horizontalPadding * 2) / tabCount

        var lastVibratedIndex by remember { mutableStateOf(selectedIndex) }
        val indicatorOffset = remember { Animatable((horizontalPadding + tabWidth * selectedIndex).value) }

        LaunchedEffect(selectedIndex, isDragging) {
            if (!isDragging) {
                indicatorOffset.animateTo(
                    targetValue = (horizontalPadding + tabWidth * selectedIndex).value,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 430f),
                )
            }
        }

        LaunchedEffect(rawPointerOffset, isDragging) {
            if (isDragging && rawPointerOffset != Offset.Unspecified) {
                val pointerXDp = with(density) { rawPointerOffset.x.toDp() }
                val target = (pointerXDp - tabWidth / 2)
                    .coerceIn(horizontalPadding, maxWidth - horizontalPadding - tabWidth)
                indicatorOffset.snapTo(target.value)
            }
        }

        LaunchedEffect(isDragging, settleDragSelection) {
            if (!isDragging && settleDragSelection) {
                val currentOffsetDp = indicatorOffset.value.dp
                val nearestIndex = ((currentOffsetDp - horizontalPadding) / tabWidth)
                    .roundToInt()
                    .coerceIn(0, tabCount - 1)
                onSelect(AppView.entries[nearestIndex])
                settleDragSelection = false
            }
        }

        val currentOffsetDp = indicatorOffset.value.dp
        val nearestIndex = ((currentOffsetDp - horizontalPadding) / tabWidth)
            .roundToInt()
            .coerceIn(0, tabCount - 1)

        LaunchedEffect(nearestIndex) {
            if (nearestIndex != lastVibratedIndex) {
                if (theme.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                lastVibratedIndex = nearestIndex
            }
        }

        val distanceX = remember(indicatorOffset.value, barPointer) {
            if (barPointer != Offset.Unspecified) {
                val fingerXDp = with(density) { barPointer.x.toDp() }
                val centerXDp = indicatorOffset.value + tabWidth.value / 2
                abs(fingerXDp.value - centerXDp)
            } else {
                0f
            }
        }

        val lift by animateFloatAsState(
            targetValue = if (isBarPressed) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
            label = "indicatorLift",
        )

        val stretch by animateFloatAsState(
            targetValue = (distanceX / tabWidth.value).coerceIn(0f, 0.35f) * lift,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
            label = "indicatorStretch",
        )
        val scaleX = 1f + 0.12f * lift + stretch
        val scaleY = 1f + 0.08f * lift - stretch * 0.4f

        if (useRealGlass) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBackdropSafe(
                        backdrop = backdrop,
                        shape = { barShape },
                        effects = {
                            vibrancy()
                            blur(tuning.blurDp.dp.toPx())
                            lens(
                                refractionHeight = tuning.refractionHeightDp.dp.toPx(),
                                refractionAmount = tuning.refractionAmountDp.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = tuning.chromaticAberration,
                            )
                        },
                        highlight = { Highlight.Ambient.copy(alpha = tuning.highlightAlpha) },
                        shadow = {
                            Shadow(
                                radius = 28.dp,
                                offset = DpOffset(0.dp, 8.dp),
                                color = Color.Black.copy(alpha = barShadowAlpha),
                            )
                        },
                        onDrawSurface = {
                            if (legibility.scrimAlpha > 0f) {
                                drawRoundRect(Color.Black.copy(alpha = legibility.scrimAlpha))
                            }
                            drawRoundRect(legibility.surfaceTint.copy(alpha = barSurfaceAlpha))
                        },
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(barShape)
                    .background(
                        if (theme.style == "modern") {
                            legibility.surfaceTint.copy(alpha = (0.82f * legibility.surfaceAlphaBoost).coerceIn(0.56f, 0.94f))
                        } else {
                            Color(0xE0131220)
                        }
                    )
                    .border(0.7.dp, Color.White.copy(alpha = barBorderAlpha), barShape)
            )
        }

        if (useRealGlass) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(barShape),
            ) {
                GlassHighlightLayer(pointerOffset = barPointer)
            }
        }

        if (useRealGlass) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset.value.dp, y = 7.dp)
                    .width(tabWidth)
                    .height(60.dp)
                    .graphicsLayer(
                        scaleX = scaleX,
                        scaleY = scaleY,
                        transformOrigin = TransformOrigin.Center,
                    )
                    .drawBackdropSafe(
                        backdrop = backdrop,
                        shape = { indicatorShape },
                        effects = {
                            vibrancy()
                            blur(tuning.blurDp.dp.toPx())
                            lens(
                                refractionHeight = tuning.refractionHeightDp.dp.toPx(),
                                refractionAmount = tuning.refractionAmountDp.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = tuning.chromaticAberration,
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = (tuning.highlightAlpha * (0.65f + 0.20f * lift)).coerceIn(0f, 1f)) },
                        shadow = {
                            Shadow(
                                radius = (18f + 6f * lift).dp,
                                offset = DpOffset(0.dp, 4.dp),
                                color = AccentPurple.copy(alpha = (tuning.shadowAlpha * legibility.shadowAlphaBoost * (0.70f + 0.20f * lift)).coerceIn(0f, 1f)),
                            )
                        },
                        onDrawSurface = {
                            val alpha = (tuning.surfaceAlpha * (0.80f + 0.35f * lift) * legibility.surfaceAlphaBoost).coerceIn(0f, 0.86f)
                            drawRoundRect(AccentPurple.copy(alpha = alpha))
                        },
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset.value.dp, y = 7.dp)
                    .width(tabWidth)
                    .height(60.dp)
                    .graphicsLayer(
                        scaleX = scaleX,
                        scaleY = scaleY,
                        transformOrigin = TransformOrigin.Center,
                    )
                    .clip(indicatorShape)
                    .background(AccentPurple.copy(alpha = ((0.30f + 0.10f * lift) * legibility.surfaceAlphaBoost).coerceIn(0f, 0.62f)))
                    .border(0.7.dp, AccentPurple.copy(alpha = (0.38f * legibility.borderAlphaBoost).coerceIn(0f, 0.82f)), indicatorShape)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppView.entries.forEach { view ->
                BottomTab(
                    view = view,
                    selected = view == selectedView,
                    onClick = { onSelect(view) },
                    selectedColor = selectedTabColor,
                    unselectedColor = unselectedTabColor,
                    selectedIconColor = selectedIconColor,
                )
            }
        }
    }
}

@Composable
private fun BottomTab(
    view: AppView,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    selectedIconColor: Color,
) {
    val color by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "bottomNavTabColor",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 25.dp else 21.dp,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 520f),
        label = "bottomNavIconSize",
    )

    Column(
        modifier = Modifier
            .width(70.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .tvFocusIndicator(RoundedCornerShape(999.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = view.icon,
            contentDescription = view.label,
            tint = if (selected) selectedIconColor else color,
            modifier = Modifier
                .padding(top = 7.dp)
                .size(iconSize),
        )
        Text(
            text = view.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 7.dp),
        )
    }
}
