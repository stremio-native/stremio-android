package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.AppFont
import com.stremio.mobile.core.theme.MutedText

@Composable
fun InterfaceSettingsScreen(
    settings: com.stremio.core.types.profile.Profile.Settings?,
    globalUiStyle: String,
    glassEffectsMode: String,
    globalGlassAlpha: Float,
    adaptiveGlassContrast: Boolean,
    glassHapticsEnabled: Boolean,
    hapticsIntensity: String,
    selectedFont: AppFont,
    onUpdateSettings: (com.stremio.core.types.profile.Profile.Settings) -> Unit,
    onSetGlobalUiStyle: (String) -> Unit,
    onSetGlassEffectsMode: (String) -> Unit,
    onSetGlobalGlassAlpha: (Float) -> Unit,
    onSetAdaptiveGlassContrastEnabled: (Boolean) -> Unit,
    onSetGlassHapticsEnabled: (Boolean) -> Unit,
    onSetHapticsIntensity: (String) -> Unit,
    onSetSelectedFont: (AppFont) -> Unit,
    onNavigateToLiquidGlassLab: () -> Unit,
    onBack: () -> Unit
) {
    SettingsColumn {
        SettingsHeader(title = "Interface Settings", onBack = onBack)


        if (settings != null) {
            val languages = listOf(
                "eng" to "English",
                "spa" to "Spanish",
                "fre" to "French",
                "ger" to "German",
                "ita" to "Italian",
                "por" to "Portuguese",
                "rus" to "Russian",
                "zho" to "Chinese"
            )
            val uiStyles = listOf(
                "classic" to "Classic",
                "modern" to "Modern (Liquid Glass)",
            )
            val glassEffects = listOf(
                "balanced" to "Balanced",
                "full" to "Full Blur",
                "static" to "Performance",
            )
            val intensities = listOf(
                "Light" to "Light",
                "Medium" to "Medium",
                "Heavy" to "Heavy",
            )

            Text(
                text = "APPEARANCE",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsDropdownRow(
                title = "Global UI Style",
                selectedValue = globalUiStyle,
                options = uiStyles,
                onSelect = onSetGlobalUiStyle,
            )

            val fonts = AppFont.entries.map { it to it.displayName }
            SettingsDropdownRow(
                title = "App Typography",
                selectedValue = selectedFont,
                options = fonts,
                onSelect = onSetSelectedFont,
                description = "Choose the default system font for the application"
            )

            SettingsDropdownRow(
                title = "Glass Effects",
                selectedValue = glassEffectsMode,
                options = glassEffects,
                onSelect = onSetGlassEffectsMode,
                description = "Controls Liquid Glass performance and blur usage"
            )

            SettingsSliderRow(
                title = "Glass Transparency",
                value = globalGlassAlpha,
                onValueChange = onSetGlobalGlassAlpha,
                valueRange = 0f..0.6f,
                description = "Adjust opacity of Liquid Glass surfaces"
            )

            SettingsToggleRow(
                title = "Adaptive Glass Contrast",
                checked = adaptiveGlassContrast,
                onCheckedChange = onSetAdaptiveGlassContrastEnabled,
                description = "Boosts tint, borders, and shadows over bright or busy content"
            )

            SettingsToggleRow(
                title = "Haptic Feedback",
                checked = glassHapticsEnabled,
                onCheckedChange = onSetGlassHapticsEnabled,
                description = "Enable tactile ticks on interactions"
            )

            if (glassHapticsEnabled) {
                SettingsDropdownRow(
                    title = "Haptic Intensity",
                    selectedValue = hapticsIntensity,
                    options = intensities,
                    onSelect = onSetHapticsIntensity
                )
            }

            SettingsClickRow(
                title = "Liquid Glass Lab",
                onClick = onNavigateToLiquidGlassLab,
                description = "Fine-tune blur, refraction, highlights, and custom values"
            )

            Text(
                text = "INTERFACE",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsDropdownRow(
                title = "Interface Language",
                selectedValue = settings.interfaceLanguage,
                options = languages,
                onSelect = { onUpdateSettings(settings.copy(interfaceLanguage = it)) },
                description = "Choose language for menus and catalogs"
            )

            SettingsToggleRow(
                title = "Hide Spoilers",
                checked = settings.hideSpoilers,
                onCheckedChange = { onUpdateSettings(settings.copy(hideSpoilers = it)) },
                description = "Blur unwatched movies and series episode posters"
            )

            SettingsToggleRow(
                title = "Gamepad Support",
                checked = settings.gamepadSupport,
                onCheckedChange = { onUpdateSettings(settings.copy(gamepadSupport = it)) },
                description = "Enable remote control and gamepad navigation support"
            )
        }
    }
}
