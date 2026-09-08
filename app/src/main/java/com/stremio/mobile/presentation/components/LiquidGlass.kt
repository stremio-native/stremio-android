package com.stremio.mobile.presentation.components

import android.os.Build
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.GraphicsLayerScope
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.stremio.mobile.core.theme.*
import com.stremio.mobile.data.model.LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA
import kotlinx.coroutines.flow.collectLatest

val LocalGlassAlpha = compositionLocalOf { LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA }

// ─── GlassHighlightLayer ──────────────────────────────────────────────────────

/**
 * A specular highlight layer that follows the user's finger across a glass
 * surface. When no pointer is active the highlight rests at center.
 */
@Composable
fun GlassHighlightLayer(
    modifier: Modifier = Modifier,
    pointerOffset: Offset = Offset.Unspecified,
) {
    val intensity = LocalGlobalUiTheme.current.liquidGlassTuning.highlightAlpha.coerceIn(0f, 1f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val hasPointer = pointerOffset != Offset.Unspecified

        // pointer-reactive position, centered by default
        val highlightCx = if (hasPointer) pointerOffset.x else w * 0.5f
        val highlightCy = if (hasPointer) pointerOffset.y - h * 0.35f else h * -0.15f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f * intensity),
                    Color.White.copy(alpha = 0.06f * intensity),
                    Color.Transparent,
                ),
                center = Offset(highlightCx, highlightCy),
                radius = w * 0.55f,
            ),
            radius = w * 0.55f,
            center = Offset(highlightCx, highlightCy),
        )

        // inner glow along top edge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f * intensity), Color.Transparent),
                startY = 0f,
                endY = h * 0.35f,
            ),
        )

        // subtle bottom reflection
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.03f * intensity)),
                startY = h * 0.75f,
                endY = h,
            ),
        )
    }
}

// ─── LiquidGlassCard ──────────────────────────────────────────────────────────

/**
 * Frosted-glass card using [drawBackdrop] for GPU-accelerated blur, vibrancy
 * and lens refraction. Interaction handling lives in the content so list rows
 * and settings cards do not pay for pointer tracking or press transforms.
 */
@Composable
fun LiquidGlassCard(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)

    val currentGlassAlpha = LocalGlassAlpha.current
    val theme = LocalGlobalUiTheme.current
    val tuning = theme.liquidGlassTuning.clamped()
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val surfaceAlpha = (tuning.surfaceAlpha * (currentGlassAlpha / LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA) * legibility.surfaceAlphaBoost).coerceIn(0f, 0.90f)
    val borderAlpha = (tuning.borderAlpha * legibility.borderAlphaBoost).coerceIn(0f, 1f)

    Box(modifier = modifier) {
        if (legibility.scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color.Black.copy(alpha = legibility.scrimAlpha)),
            )
        }
        // glass backdrop surface
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBackdropSafe(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(cornerRadius) },
                    effects = {
                        val lensHeight = minOf(tuning.refractionHeightDp.dp.toPx(), cornerRadius.toPx() * 0.8f)
                        vibrancy()
                        blur(tuning.blurDp.dp.toPx())
                        lens(
                            refractionHeight = lensHeight,
                            refractionAmount = tuning.refractionAmountDp.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = tuning.chromaticAberration,
                        )
                    },
                    highlight = { Highlight.Ambient.copy(alpha = tuning.highlightAlpha) },
                    shadow = {
                        Shadow(
                            radius = 24.dp,
                            offset = DpOffset(0.dp, 6.dp),
                            color = Color.Black.copy(alpha = (tuning.shadowAlpha * legibility.shadowAlphaBoost).coerceIn(0f, 0.90f)),
                        )
                    },
                    onDrawSurface = {
                        drawRoundRect(legibility.surfaceTint.copy(alpha = surfaceAlpha))
                    },
                ),
        )

        // luminous border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = borderAlpha),
                            Color.White.copy(alpha = borderAlpha * 0.28f),
                        ),
                    ),
                    shape = shape,
                ),
        )

        Box(modifier = Modifier.matchParentSize().clip(shape)) {
            GlassHighlightLayer()
        }

        content()
    }
}

// ─── LiquidGlassButton ────────────────────────────────────────────────────────

/**
 * Pill-shaped translucent glass button with backdrop blur, press animation,
 * and inner highlight.
 */
@Composable
fun LiquidGlassButton(
    text: String,
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tuning = LocalGlobalUiTheme.current.liquidGlassTuning.clamped()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "btnScale",
    )

    val shape = RoundedCornerShape(999.dp)

    Box(modifier = modifier.scale(scale).height(52.dp)) {
        // glass backdrop fill
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBackdropSafe(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(999.dp) },
                    effects = {
                        vibrancy()
                        blur((tuning.blurDp + 4f).dp.toPx())
                        lens(
                            refractionHeight = tuning.refractionHeightDp.dp.toPx(),
                            refractionAmount = tuning.refractionAmountDp.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = tuning.chromaticAberration,
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = tuning.highlightAlpha * 0.55f) },
                    shadow = {
                        Shadow(
                            radius = 14.dp,
                            offset = DpOffset(0.dp, 3.dp),
                            color = AccentPurple.copy(alpha = tuning.shadowAlpha * 0.70f),
                        )
                    },
                    onDrawSurface = {
                        drawRoundRect(AccentPurple.copy(alpha = tuning.surfaceAlpha * 0.55f))
                    },
                ),
        )

        // luminous border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.7.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = tuning.borderAlpha),
                            AccentPurple.copy(alpha = tuning.borderAlpha * 0.70f),
                        ),
                    ),
                    shape = shape,
                ),
        )

        // inner shimmer
        Box(modifier = Modifier.matchParentSize().clip(shape)) {
            GlassHighlightLayer()
        }

        // clickable label
        Box(
            modifier = Modifier
                .matchParentSize()
                .tvFocusIndicator(shape)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
fun StaticGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha = LocalGlassAlpha.current.coerceIn(0f, 0.6f)
    val theme = LocalGlobalUiTheme.current
    val tuning = theme.liquidGlassTuning.clamped()
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(legibility.surfaceTint.copy(alpha = (tuning.surfaceAlpha * (alpha / LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA) * 0.72f * legibility.surfaceAlphaBoost + 0.06f).coerceIn(0.04f, 0.78f)))
            .border(
                width = 0.7.dp,
                color = Color.White.copy(alpha = (tuning.borderAlpha * legibility.borderAlphaBoost).coerceIn(0.08f, 1f)),
                shape = shape,
            ),
    ) {
        content()
    }
}

@Composable
fun StaticGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = LocalGlassAlpha.current.coerceIn(0f, 0.6f)
    val theme = LocalGlobalUiTheme.current
    val tuning = theme.liquidGlassTuning.clamped()
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(legibility.surfaceTint.copy(alpha = if (enabled) (tuning.surfaceAlpha * (alpha / LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA) * 0.72f * legibility.surfaceAlphaBoost + 0.08f).coerceIn(0.05f, 0.78f) else 0.08f))
            .border(0.7.dp, Color.White.copy(alpha = if (enabled) (tuning.borderAlpha * legibility.borderAlphaBoost).coerceIn(0f, 1f) else 0.10f), shape)
            .tvClickable(enabled = enabled, shape = shape, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = legibility.foreground.copy(alpha = if (enabled) 1f else 0.45f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun StaticGlassIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    iconTint: Color = Color.Unspecified,
) {
    val alpha = LocalGlassAlpha.current.coerceIn(0f, 0.6f)
    val theme = LocalGlobalUiTheme.current
    val tuning = theme.liquidGlassTuning.clamped()
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val resolvedIconTint = if (iconTint.isSpecified) iconTint else legibility.foreground
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(shape)
            .background(
                if (selected) AccentPurple.copy(alpha = 0.34f)
                else legibility.surfaceTint.copy(alpha = if (enabled) (tuning.surfaceAlpha * (alpha / LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA) * 0.64f * legibility.surfaceAlphaBoost + 0.06f).coerceIn(0.04f, 0.74f) else 0.06f)
            )
            .border(0.7.dp, Color.White.copy(alpha = if (enabled) (tuning.borderAlpha * legibility.borderAlphaBoost * 0.75f).coerceIn(0f, 1f) else 0.08f), shape)
            .tvClickable(enabled = enabled, shape = shape, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = resolvedIconTint.copy(alpha = if (enabled) 1f else 0.45f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun StaticGlassChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha = LocalGlassAlpha.current.coerceIn(0f, 0.6f)
    val theme = LocalGlobalUiTheme.current
    val tuning = theme.liquidGlassTuning.clamped()
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val shape = RoundedCornerShape(999.dp)
    val clickableModifier = if (onClick != null) {
        Modifier.tvClickable(enabled = enabled, shape = shape, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) AccentPurple.copy(alpha = 0.34f)
                else legibility.surfaceTint.copy(alpha = if (enabled) (tuning.surfaceAlpha * (alpha / LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA) * 0.58f * legibility.surfaceAlphaBoost + 0.06f).coerceIn(0.04f, 0.72f) else 0.06f)
            )
            .border(
                width = 0.7.dp,
                color = if (selected) AccentPurple.copy(alpha = 0.52f) else Color.White.copy(alpha = (tuning.borderAlpha * legibility.borderAlphaBoost * 0.62f).coerceIn(0f, 1f)),
                shape = shape,
            )
            .then(clickableModifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

fun isEmulator(): Boolean {
    val fp = Build.FINGERPRINT
    val model = Build.MODEL
    val brand = Build.BRAND
    val device = Build.DEVICE
    val product = Build.PRODUCT
    val hardware = Build.HARDWARE
    return fp.startsWith("generic")
            || fp.startsWith("unknown")
            || model.contains("google_sdk")
            || model.contains("Emulator")
            || model.contains("Android SDK built for")
            || brand.startsWith("generic") && device.startsWith("generic")
            || "google_sdk" == product
            || hardware.contains("goldfish")
            || hardware.contains("ranchu")
}

@Composable
fun Modifier.drawBackdropSafe(
    backdrop: Backdrop,
    shape: () -> Shape = { RectangleShape },
    effects: BackdropEffectScope.() -> Unit = {},
    highlight: (() -> Highlight?)? = null,
    shadow: (() -> Shadow?)? = null,
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: GraphicsLayerScope.() -> Unit = {},
    onDrawSurface: DrawScope.() -> Unit = {},
): Modifier {
    val canUseBackdropEffects = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    return if (!canUseBackdropEffects) {
        val resolvedShape = remember { shape() }
        this
            .clip(resolvedShape)
            .drawBehind {
                onDrawSurface()
            }
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = shape,
            effects = effects,
            highlight = highlight,
            shadow = shadow,
            innerShadow = innerShadow,
            layerBlock = layerBlock,
            onDrawSurface = onDrawSurface
        )
    }
}

@Composable
fun LiquidButton(
    text: String,
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassButton(
        text = text,
        backdrop = backdrop,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null
) {
    val tuning = LocalGlobalUiTheme.current.liquidGlassTuning.clamped()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val animationScope = rememberCoroutineScope()
    val dragWidth = 20f.dp
    val dragWidthPx = with(density) { dragWidth.toPx() }
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onCheckedChange(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (checked) 0f else 1f
                    onCheckedChange(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidthPx
                fraction = if (isLtr) {
                    (fraction + delta).coerceIn(0f, 1f)
                } else {
                    (fraction - delta).coerceIn(0f, 1f)
                }
            },
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }.collectLatest { dampedDragAnimation.updateValue(it) }
    }
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (target != fraction) {
            fraction = target
            dampedDragAnimation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val shape = RoundedCornerShape(999.dp)
    val accentColor = AccentGreen
    val trackColor = Color(0xFF787880).copy(alpha = (tuning.trackAlpha * 1.8f).coerceIn(0.12f, 0.48f))

    Box(
        modifier = modifier
            .tvFocusIndicator(shape)
            .then(
                if (LocalIsTv.current) {
                    Modifier.toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .layerBackdrop(trackBackdrop)
                .clip(shape)
                .drawBehind {
                    val color = androidx.compose.ui.graphics.lerp(trackColor, accentColor, dampedDragAnimation.value)
                    drawRoundRect(color)
                }
                .size(width = 64.dp, height = 28.dp),
        )

        val thumbBackdrop = if (backdrop != null) {
            rememberCombinedBackdrop(
                backdrop,
                rememberBackdrop(trackBackdrop) { drawBackdrop ->
                    val progress = dampedDragAnimation.pressProgress
                    val scaleX = lerp(2f / 3f, 0.75f, progress)
                    val scaleY = lerp(0f, 0.75f, progress)
                    scale(scaleX, scaleY) {
                        drawBackdrop()
                    }
                },
            )
        } else {
            null
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    val dragWidthPx = dragWidth.toPx()
                    translationX = if (isLtr) {
                        lerp(padding, padding + dragWidthPx, dampedDragAnimation.value)
                    } else {
                        lerp(-padding, -(padding + dragWidthPx), dampedDragAnimation.value)
                    }
                }
                .semantics { role = Role.Switch }
                .then(dampedDragAnimation.modifier)
                .then(
                    if (thumbBackdrop != null) {
                        Modifier.drawBackdropSafe(
                            backdrop = thumbBackdrop,
                            shape = { RoundedCornerShape(999.dp) },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                blur(8f.dp.toPx() * (1f - progress))
                                lens(
                                    refractionHeight = 5f.dp.toPx() * progress,
                                    refractionAmount = 10f.dp.toPx() * progress,
                                    chromaticAberration = tuning.chromaticAberration,
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Ambient.copy(
                                    width = Highlight.Ambient.width / 1.5f,
                                    blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                    alpha = progress * tuning.highlightAlpha,
                                )
                            },
                            shadow = {
                                Shadow(
                                    radius = 4f.dp,
                                    color = Color.Black.copy(alpha = 0.05f),
                                )
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 4f.dp * progress,
                                    alpha = progress,
                                )
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 50f
                                scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(Color.White.copy(alpha = tuning.thumbAlpha * (1f - progress)))
                            },
                        )
                    } else {
                        Modifier
                            .clip(shape)
                            .background(Color.White.copy(alpha = tuning.thumbAlpha))
                            .graphicsLayer {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                            }
                    }
                )
                .size(width = 40.dp, height = 24.dp),
        )
    }
}

@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    backdrop: LayerBackdrop? = null
) {
    val tuning = LocalGlobalUiTheme.current.liquidGlassTuning.clamped()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val dampedDragAnimation = remember(animationScope, valueRange) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = value,
            valueRange = valueRange,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    onValueChange(targetValue)
                    onValueChangeFinished?.invoke()
                    didDrag = false
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidthPx)
                val next = if (isLtr) {
                    targetValue + delta
                } else {
                    targetValue - delta
                }.coerceIn(valueRange)
                onValueChange(next)
            },
        )
    }
    LaunchedEffect(dampedDragAnimation, value) {
        snapshotFlow { value }.collectLatest {
            if (dampedDragAnimation.targetValue != it) {
                dampedDragAnimation.updateValue(it)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .tvFocusIndicator(RoundedCornerShape(999.dp))
            .then(
                if (!LocalIsTv.current) {
                    Modifier
                } else {
                    Modifier
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) return@onKeyEvent false
                            val step = (valueRange.endInclusive - valueRange.start) / 20f
                            val next = when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> (value - step).coerceIn(valueRange)
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> (value + step).coerceIn(valueRange)
                                else -> return@onKeyEvent false
                            }
                            onValueChange(next)
                            onValueChangeFinished?.invoke()
                            true
                        }
                },
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackBackdrop = rememberLayerBackdrop()
        val accentColor = Color(0xFF0088FF)
        val trackColor = Color(0xFF787880).copy(alpha = (tuning.trackAlpha * 1.8f).coerceIn(0.12f, 0.48f))
        SideEffect {
            trackWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(trackColor)
                    .pointerInput(animationScope, valueRange) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / size.width)
                            val targetValue = if (isLtr) {
                                valueRange.start + delta
                            } else {
                                valueRange.endInclusive - delta
                            }.coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                            onValueChangeFinished?.invoke()
                        }
                    }
                    .height(6.dp)
                    .fillMaxWidth(),
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor)
                    .height(6.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).toInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
            )
        }

        val thumbBackdrop = if (backdrop != null) {
            rememberCombinedBackdrop(
                backdrop,
                rememberBackdrop(trackBackdrop) { drawBackdrop ->
                    val progress = dampedDragAnimation.pressProgress
                    val scaleX = lerp(2f / 3f, 1f, progress)
                    val scaleY = lerp(0f, 1f, progress)
                    scale(scaleX, scaleY) {
                        drawBackdrop()
                    }
                },
            )
        } else {
            null
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    val trackWidth = constraints.maxWidth.toFloat()
                    translationX = (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                        .coerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .then(
                    if (thumbBackdrop != null) {
                        Modifier.drawBackdropSafe(
                            backdrop = thumbBackdrop,
                            shape = { RoundedCornerShape(999.dp) },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                blur(8f.dp.toPx() * (1f - progress))
                                lens(
                                    refractionHeight = 10f.dp.toPx() * progress,
                                    refractionAmount = 14f.dp.toPx() * progress,
                                    chromaticAberration = tuning.chromaticAberration,
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Ambient.copy(
                                    width = Highlight.Ambient.width / 1.5f,
                                    blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                    alpha = progress * tuning.highlightAlpha,
                                )
                            },
                            shadow = {
                                Shadow(
                                    radius = 4f.dp,
                                    color = Color.Black.copy(alpha = 0.05f),
                                )
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 4f.dp * progress,
                                    alpha = progress,
                                )
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(Color.White.copy(alpha = tuning.thumbAlpha * (1f - progress)))
                            },
                        )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = tuning.thumbAlpha))
                            .graphicsLayer {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                            }
                    }
                )
                .size(width = 40.dp, height = 24.dp),
        )
    }
}

@Composable
fun LiquidBottomTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x12FFFFFF))
            .border(0.7.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedTabIndex
            val animFraction by animateFloatAsState(
                targetValue = if (isSelected) 1.0f else 0.0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                label = "tabAnim"
            )
            val activeColor = AccentPurple.copy(alpha = 0.32f * animFraction)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(activeColor)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
