package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.AccentGreen
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.data.model.LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA
import com.stremio.mobile.data.model.LiquidGlassTuning
import com.stremio.mobile.presentation.components.GlobalUiTheme
import com.stremio.mobile.presentation.components.LocalGlassAlpha
import com.stremio.mobile.presentation.components.LocalGlobalUiTheme
import com.stremio.mobile.presentation.components.ThemedButton
import com.stremio.mobile.presentation.components.ThemedCard
import com.stremio.mobile.presentation.components.ThemedChip
import com.stremio.mobile.presentation.components.ThemedIconButton
import com.stremio.mobile.presentation.components.ThemedSlider
import com.stremio.mobile.presentation.components.ThemedSurfaceRole
import com.stremio.mobile.presentation.components.ThemedToggle

@Composable
fun LiquidGlassDevScreen(
    globalUiStyle: String,
    glassEffectsMode: String,
    globalGlassAlpha: Float,
    tuning: LiquidGlassTuning,
    onSetGlobalUiStyle: (String) -> Unit,
    onSetGlassEffectsMode: (String) -> Unit,
    onSetGlobalGlassAlpha: (Float) -> Unit,
    onSetLiquidGlassTuning: (LiquidGlassTuning) -> Unit,
    onResetLiquidGlassTuning: () -> Unit,
    onBack: () -> Unit,
) {
    var draftUiStyle by remember(globalUiStyle) { mutableStateOf(globalUiStyle) }
    var draftEffectsMode by remember(glassEffectsMode) { mutableStateOf(glassEffectsMode) }
    var draftGlassAlpha by remember(globalGlassAlpha) { mutableFloatStateOf(globalGlassAlpha) }
    var draftTuning by remember(tuning) { mutableStateOf(tuning.clamped()) }
    val current = draftTuning.clamped()

    fun updateTuning(transform: (LiquidGlassTuning) -> LiquidGlassTuning) {
        draftTuning = transform(draftTuning.clamped()).clamped()
    }

    fun applyDraft() {
        onSetGlobalUiStyle(draftUiStyle)
        onSetGlassEffectsMode(draftEffectsMode)
        onSetGlobalGlassAlpha(draftGlassAlpha)
        onSetLiquidGlassTuning(draftTuning.clamped())
    }

    SettingsColumn {
        SettingsHeader(title = "Liquid Glass Lab", onBack = onBack)

        Text(
            text = "LIVE MODE",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
        )

        SettingsDropdownRow(
            title = "Global UI Style",
            selectedValue = draftUiStyle,
            options = listOf("classic" to "Classic", "modern" to "Modern (Liquid Glass)"),
            onSelect = { draftUiStyle = it },
            description = "Preview/apply the whole-app style without moving other lab controls",
        )

        SettingsDropdownRow(
            title = "Glass Effects",
            selectedValue = draftEffectsMode,
            options = listOf("balanced" to "Balanced", "full" to "Full Blur", "static" to "Performance"),
            onSelect = { draftEffectsMode = it },
            description = "Preview/apply the effects mode without changing slider values",
        )

        SettingsSliderRow(
            title = "Global Transparency",
            value = draftGlassAlpha,
            onValueChange = { draftGlassAlpha = it },
            valueRange = 0f..0.6f,
            displayValue = "${(draftGlassAlpha * 100).toInt()}%",
        )

        LiquidGlassPreview(
            globalUiStyle = draftUiStyle,
            glassEffectsMode = draftEffectsMode,
            globalGlassAlpha = draftGlassAlpha,
            tuning = current,
        )

        Text(
            text = "GLASS SURFACE",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )

        TuningSlider(
            title = "Blur",
            value = current.blurDp,
            range = 0f..24f,
            displayValue = "${current.blurDp.format(1)} dp",
            onValueChange = { value -> updateTuning { it.copy(blurDp = value) } },
        )
        TuningSlider(
            title = "Refraction Height",
            value = current.refractionHeightDp,
            range = 0f..32f,
            displayValue = "${current.refractionHeightDp.format(1)} dp",
            onValueChange = { value -> updateTuning { it.copy(refractionHeightDp = value) } },
        )
        TuningSlider(
            title = "Refraction Amount",
            value = current.refractionAmountDp,
            range = 0f..64f,
            displayValue = "${current.refractionAmountDp.format(1)} dp",
            onValueChange = { value -> updateTuning { it.copy(refractionAmountDp = value) } },
        )
        TuningSlider(
            title = "Surface Alpha",
            value = current.surfaceAlpha,
            range = 0f..0.8f,
            displayValue = "${(current.surfaceAlpha * 100).toInt()}%",
            onValueChange = { value -> updateTuning { it.copy(surfaceAlpha = value) } },
        )
        TuningSlider(
            title = "Highlight Alpha",
            value = current.highlightAlpha,
            range = 0f..1f,
            displayValue = "${(current.highlightAlpha * 100).toInt()}%",
            onValueChange = { value -> updateTuning { it.copy(highlightAlpha = value) } },
        )
        TuningSlider(
            title = "Border Alpha",
            value = current.borderAlpha,
            range = 0f..1f,
            displayValue = "${(current.borderAlpha * 100).toInt()}%",
            onValueChange = { value -> updateTuning { it.copy(borderAlpha = value) } },
        )
        TuningSlider(
            title = "Shadow Alpha",
            value = current.shadowAlpha,
            range = 0f..0.8f,
            displayValue = "${(current.shadowAlpha * 100).toInt()}%",
            onValueChange = { value -> updateTuning { it.copy(shadowAlpha = value) } },
        )

        Text(
            text = "CONTROLS",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )

        TuningSlider(
            title = "Track Alpha",
            value = current.trackAlpha,
            range = 0f..0.8f,
            displayValue = "${(current.trackAlpha * 100).toInt()}%",
            onValueChange = { value -> updateTuning { it.copy(trackAlpha = value) } },
        )
        TuningSlider(
            title = "Thumb Alpha",
            value = current.thumbAlpha,
            range = 0.2f..1f,
            displayValue = "${(current.thumbAlpha * 100).toInt()}%",
            onValueChange = { value -> updateTuning { it.copy(thumbAlpha = value) } },
        )
        SettingsToggleRow(
            title = "Chromatic Aberration",
            checked = current.chromaticAberration,
            onCheckedChange = { value -> updateTuning { it.copy(chromaticAberration = value) } },
            description = "Toggles the color-fringe refraction effect",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemedButton(
                text = "Modern Full Blur",
                onClick = {
                    draftUiStyle = "modern"
                    draftEffectsMode = "full"
                },
                modifier = Modifier.weight(1f),
                role = ThemedSurfaceRole.Overlay,
            )
            ThemedButton(
                text = "Reset Defaults",
                onClick = {
                    draftGlassAlpha = LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA
                    draftTuning = LiquidGlassTuning()
                },
                modifier = Modifier.weight(1f),
                containerColor = Color.White.copy(alpha = 0.18f),
                role = ThemedSurfaceRole.Overlay,
            )
        }

        ThemedButton(
            text = "Apply To App",
            onClick = { applyDraft() },
            modifier = Modifier.fillMaxWidth(),
            role = ThemedSurfaceRole.Overlay,
        )
    }
}

@Composable
private fun LiquidGlassPreview(
    globalUiStyle: String,
    glassEffectsMode: String,
    globalGlassAlpha: Float,
    tuning: LiquidGlassTuning,
) {
    var previewToggle by remember { mutableStateOf(true) }
    var previewSlider by remember { mutableFloatStateOf(0.58f) }

    val previewTheme = GlobalUiTheme(
        style = globalUiStyle,
        glassEffectsMode = glassEffectsMode,
        glassAlpha = globalGlassAlpha,
        liquidGlassTuning = tuning.clamped(),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2A7DE1),
                        Color(0xFF57D8B7),
                        Color(0xFFE879F9),
                        Color(0xFF121220),
                    ),
                ),
            )
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentGreen.copy(alpha = 0.76f)),
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentPurple.copy(alpha = 0.84f)),
            )
        }

        CompositionLocalProvider(
            LocalGlobalUiTheme provides previewTheme,
            LocalGlassAlpha provides globalGlassAlpha,
        ) {
            ThemedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                cornerRadius = 28.dp,
                role = ThemedSurfaceRole.Featured,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Liquid Glass Preview",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Cards, buttons, toggle, slider",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                            )
                        }
                        ThemedIconButton(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            onClick = {},
                            selected = true,
                            role = ThemedSurfaceRole.Overlay,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ThemedChip(selected = true) {
                            Icon(
                                imageVector = Icons.Outlined.Subtitles,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(text = "Glass", color = Color.White, fontSize = 13.sp)
                        }
                        ThemedToggle(
                            checked = previewToggle,
                            onCheckedChange = { previewToggle = it },
                        )
                    }

                    ThemedSlider(
                        value = previewSlider,
                        onValueChange = { previewSlider = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TuningSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
) {
    SettingsSliderRow(
        title = title,
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        displayValue = displayValue,
    )
}

private fun Float.format(decimals: Int): String = "%.${decimals}f".format(java.util.Locale.US, this)
