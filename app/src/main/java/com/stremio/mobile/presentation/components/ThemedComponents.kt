package com.stremio.mobile.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.data.model.LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA
import com.stremio.mobile.data.model.LiquidGlassTuning

// Global Theme and Haptic configurations
data class GlobalUiTheme(
    val style: String = "classic", // "classic" (Material UI) or "modern" (Liquid Glass)
    val glassEffectsMode: String = "balanced",
    val glassAlpha: Float = LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA,
    val hapticsEnabled: Boolean = true,
    val hapticsIntensity: String = "Medium",
    val liquidGlassTuning: LiquidGlassTuning = LiquidGlassTuning(),
    val adaptiveGlassContrast: Boolean = true,
)

data class GlassLegibility(
    val foreground: Color = Color.White,
    val mutedForeground: Color = Color.White.copy(alpha = 0.72f),
    val surfaceTint: Color = Color.White,
    val surfaceAlphaBoost: Float = 1f,
    val borderAlphaBoost: Float = 1f,
    val shadowAlphaBoost: Float = 1f,
    val scrimAlpha: Float = 0f,
) {
    companion object {
        val Default = GlassLegibility()

        fun brightBackground(): GlassLegibility = GlassLegibility(
            foreground = Color(0xFF11131A),
            mutedForeground = Color(0xCC11131A),
            surfaceTint = Color.White,
            surfaceAlphaBoost = 1.45f,
            borderAlphaBoost = 1.35f,
            shadowAlphaBoost = 1.45f,
            scrimAlpha = 0.08f,
        )

        fun mediaOverlay(): GlassLegibility = GlassLegibility(
            foreground = Color.White,
            mutedForeground = Color.White.copy(alpha = 0.78f),
            surfaceTint = Color(0xFF0D0F17),
            surfaceAlphaBoost = 1.28f,
            borderAlphaBoost = 1.35f,
            shadowAlphaBoost = 1.55f,
            scrimAlpha = 0.16f,
        )

        fun navigationBar(): GlassLegibility = GlassLegibility(
            foreground = Color.White,
            mutedForeground = Color.White.copy(alpha = 0.82f),
            surfaceTint = Color(0xFF0B0D14),
            surfaceAlphaBoost = 1.22f,
            borderAlphaBoost = 1.45f,
            shadowAlphaBoost = 1.50f,
            scrimAlpha = 0.12f,
        )

        fun highContrast(): GlassLegibility = GlassLegibility(
            foreground = Color.White,
            mutedForeground = Color.White.copy(alpha = 0.90f),
            surfaceTint = Color.Black,
            surfaceAlphaBoost = 1.85f,
            borderAlphaBoost = 2.0f,
            shadowAlphaBoost = 1.8f,
            scrimAlpha = 0.24f,
        )
    }
}

enum class ThemedSurfaceRole {
    Featured,
    Dense,
    Overlay,
    PlayerOverlay,
}

val LocalGlobalUiTheme = compositionLocalOf { GlobalUiTheme() }
val LocalGlobalBackdrop = compositionLocalOf<LayerBackdrop?> { null }
val LocalGlassLegibility = compositionLocalOf { GlassLegibility.Default }

@Composable
fun rememberGlobalHapticFeedback(): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    val theme = LocalGlobalUiTheme.current
    return remember(hapticFeedback, theme.hapticsEnabled, theme.hapticsIntensity) {
        {
            if (theme.hapticsEnabled) {
                val type = when (theme.hapticsIntensity) {
                    "Light" -> HapticFeedbackType.TextHandleMove
                    else -> HapticFeedbackType.LongPress
                }
                hapticFeedback.performHapticFeedback(type)
            }
        }
    }
}

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    cornerRadius: Dp = 28.dp,
    role: ThemedSurfaceRole = ThemedSurfaceRole.Dense,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalGlobalUiTheme.current
    val resolvedBackdrop = backdrop ?: LocalGlobalBackdrop.current
    if (shouldUseRealGlass(theme, role, resolvedBackdrop)) {
        LiquidGlassCard(
            backdrop = resolvedBackdrop!!,
            modifier = modifier,
            cornerRadius = cornerRadius,
            content = content
        )
    } else if (theme.style == "modern") {
        StaticGlassCard(
            modifier = modifier,
            cornerRadius = cornerRadius,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
fun ThemedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    enabled: Boolean = true,
    containerColor: Color = AccentPurple,
    contentColor: Color = Color.White,
    role: ThemedSurfaceRole = ThemedSurfaceRole.Dense,
    content: @Composable RowScope.() -> Unit,
) {
    val theme = LocalGlobalUiTheme.current
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val resolvedBackdrop = backdrop ?: LocalGlobalBackdrop.current
    val resolvedContentColor = if (theme.style == "modern" && contentColor == Color.White) {
        legibility.foreground
    } else {
        contentColor
    }
    val triggerHaptic = rememberGlobalHapticFeedback()
    val combinedOnClick = {
        triggerHaptic()
        onClick()
    }
    
    if (shouldUseRealGlass(theme, role, resolvedBackdrop)) {
        LiquidGlassCard(
            backdrop = resolvedBackdrop!!,
            modifier = modifier,
            cornerRadius = 999.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(containerColor.copy(alpha = if (enabled) 0.34f else 0.10f))
                    .tvClickable(enabled = enabled, shape = RoundedCornerShape(999.dp), onClick = combinedOnClick)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content,
            )
        }
    } else if (theme.style == "modern") {
        Row(
            modifier = modifier
                .defaultMinSize(minHeight = 52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(containerColor.copy(alpha = if (enabled) 0.34f else 0.10f))
                .border(0.8.dp, Color.White.copy(alpha = if (enabled) 0.22f else 0.08f), RoundedCornerShape(999.dp))
                .tvClickable(enabled = enabled, shape = RoundedCornerShape(999.dp), onClick = combinedOnClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    } else {
        Button(
            onClick = combinedOnClick,
            modifier = modifier.tvFocusIndicator(),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = resolvedContentColor,
                disabledContainerColor = GlassSurface,
                disabledContentColor = MutedText,
            ),
            content = content,
        )
    }
}

@Composable
fun ThemedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    enabled: Boolean = true,
    containerColor: Color = AccentPurple,
    contentColor: Color = Color.White,
    role: ThemedSurfaceRole = ThemedSurfaceRole.Dense,
) {
    val theme = LocalGlobalUiTheme.current
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val resolvedContentColor = if (theme.style == "modern" && contentColor == Color.White) {
        legibility.foreground
    } else {
        contentColor
    }
    ThemedButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = resolvedContentColor,
        role = role,
    ) {
        Text(text = text, color = resolvedContentColor)
    }
}

@Composable
fun ThemedTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val theme = LocalGlobalUiTheme.current
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val triggerHaptic = rememberGlobalHapticFeedback()
    val combinedOnClick = {
        triggerHaptic()
        onClick()
    }

    if (theme.style == "modern") {
        StaticGlassChip(
            modifier = modifier.tvFocusIndicator(),
            enabled = enabled,
            onClick = combinedOnClick,
        ) {
            Text(text = text, color = legibility.foreground.copy(alpha = if (enabled) 1f else 0.45f))
        }
    } else {
        TextButton(
            onClick = combinedOnClick,
            modifier = modifier.tvFocusIndicator(),
            enabled = enabled,
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun ThemedIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    containerColor: Color? = null,
    iconTint: Color = Color.White,
    backdrop: LayerBackdrop? = null,
    role: ThemedSurfaceRole = ThemedSurfaceRole.Dense,
) {
    val theme = LocalGlobalUiTheme.current
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    val resolvedBackdrop = backdrop ?: LocalGlobalBackdrop.current
    val resolvedIconTint = if (theme.style == "modern" && iconTint == Color.White) {
        legibility.foreground
    } else {
        iconTint
    }
    val triggerHaptic = rememberGlobalHapticFeedback()
    val combinedOnClick = {
        triggerHaptic()
        onClick()
    }

    if (shouldUseRealGlass(theme, role, resolvedBackdrop)) {
        LiquidGlassCard(
            backdrop = resolvedBackdrop!!,
            modifier = modifier.size(44.dp),
            cornerRadius = 999.dp,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .tvClickable(enabled = enabled, shape = CircleShape, onClick = combinedOnClick),
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
    } else if (theme.style == "modern") {
        StaticGlassIconButton(
            imageVector = imageVector,
            contentDescription = contentDescription,
            onClick = combinedOnClick,
            modifier = modifier,
            enabled = enabled,
            selected = selected,
            iconTint = resolvedIconTint,
        )
    } else {
        IconButton(
            onClick = combinedOnClick,
            enabled = enabled,
            modifier = if (containerColor != null) {
                modifier
                    .clip(CircleShape)
                    .background(containerColor)
            } else {
                modifier.tvFocusIndicator()
            },
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (enabled) resolvedIconTint else MutedText,
            )
        }
    }
}

@Composable
fun ThemedChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val theme = LocalGlobalUiTheme.current
    val triggerHaptic = rememberGlobalHapticFeedback()
    val combinedOnClick = onClick?.let {
        {
            triggerHaptic()
            it()
        }
    }

    if (theme.style == "modern") {
        StaticGlassChip(
            modifier = modifier,
            selected = selected,
            enabled = enabled,
            onClick = combinedOnClick,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    } else {
        val shape = RoundedCornerShape(999.dp)
        Row(
            modifier = modifier
                .clip(shape)
                .background(if (selected) AccentPurple else GlassSurface)
                .then(
                    if (combinedOnClick != null) {
                        Modifier.tvClickable(enabled = enabled, shape = RoundedCornerShape(999.dp), onClick = combinedOnClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun ThemedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalGlobalUiTheme.current
    val legibility = if (theme.adaptiveGlassContrast) LocalGlassLegibility.current else GlassLegibility.Default
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.background(
            if (theme.style == "modern") {
                legibility.surfaceTint.copy(alpha = (theme.glassAlpha * 0.42f * legibility.surfaceAlphaBoost + 0.10f).coerceIn(0.10f, 0.56f))
            } else {
                GlassSurface
            }
        ),
        content = content,
    )
}

@Composable
fun ThemedToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null
) {
    val theme = LocalGlobalUiTheme.current
    val resolvedBackdrop = backdrop ?: LocalGlobalBackdrop.current
    val triggerHaptic = rememberGlobalHapticFeedback()
    val combinedOnCheckedChange: (Boolean) -> Unit = {
        triggerHaptic()
        onCheckedChange(it)
    }
    
    if (theme.style == "modern") {
        LiquidToggle(
            checked = checked,
            onCheckedChange = combinedOnCheckedChange,
            modifier = modifier,
            backdrop = resolvedBackdrop
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = combinedOnCheckedChange,
            modifier = modifier.tvFocusIndicator(RoundedCornerShape(999.dp)),
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = AccentPurple,
                uncheckedThumbColor = com.stremio.mobile.core.theme.MutedText,
                uncheckedTrackColor = androidx.compose.ui.graphics.Color(0x33FFFFFF)
            )
        )
    }
}

private fun shouldUseRealGlass(
    theme: GlobalUiTheme,
    role: ThemedSurfaceRole,
    backdrop: LayerBackdrop?,
): Boolean {
    if (theme.style != "modern" || backdrop == null) return false
    if (role == ThemedSurfaceRole.Dense) return false
    return when (theme.glassEffectsMode) {
        "full" -> true
        "static" -> false
        else -> role != ThemedSurfaceRole.Dense
    }
}

@Composable
fun ThemedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    backdrop: LayerBackdrop? = null
) {
    val theme = LocalGlobalUiTheme.current
    val resolvedBackdrop = backdrop ?: LocalGlobalBackdrop.current
    val triggerHaptic = rememberGlobalHapticFeedback()
    
    val sliderModifier = modifier.tvSliderFocusNavigation()
    if (theme.style == "modern") {
        LiquidSlider(
            value = value,
            onValueChange = {
                triggerHaptic()
                onValueChange(it)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            backdrop = resolvedBackdrop,
            modifier = sliderModifier
        )
    } else {
        Slider(
            value = value,
            onValueChange = {
                triggerHaptic()
                onValueChange(it)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            modifier = sliderModifier.tvFocusIndicator(RoundedCornerShape(999.dp)),
            colors = SliderDefaults.colors(
                thumbColor = AccentPurple,
                activeTrackColor = AccentPurple
            )
        )
    }
}
