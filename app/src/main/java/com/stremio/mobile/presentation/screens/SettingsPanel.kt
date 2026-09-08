package com.stremio.mobile.presentation.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.presentation.components.LocalIsTv
import com.stremio.mobile.presentation.components.contentGutter
import com.stremio.mobile.presentation.components.SectionTitle
import com.stremio.mobile.presentation.components.ThemedToggle
import com.stremio.mobile.presentation.components.ThemedSlider
import com.stremio.mobile.presentation.components.ThemedCard
import com.stremio.mobile.presentation.components.ThemedButton
import com.stremio.mobile.presentation.components.ThemedDropdownMenu
import com.stremio.mobile.presentation.components.rememberGlobalHapticFeedback
import com.stremio.mobile.presentation.components.tvClickable
import com.stremio.mobile.presentation.components.tvContentFocus
import com.stremio.mobile.presentation.components.tvListItemSpacing
import com.stremio.mobile.presentation.components.tvNotFocusable
import com.stremio.mobile.presentation.components.TvBackButton

enum class SettingsSubScreen {
    Main,
    Addons,
    General,
    Interface,
    Player,
    Streaming,
    Android,
    LiquidGlassLab,
    Info,
}

private val SettingsRowShape = RoundedCornerShape(16.dp)
private const val SettingsRowFocusScale = 1.025f

@Composable
fun settingsContentPadding() = contentGutter()

@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = settingsContentPadding()),
        verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(16.dp)),
        content = content,
    )
}

internal fun Modifier.settingsRowClickable(
    onClick: () -> Unit,
    requester: FocusRequester? = null,
): Modifier = fillMaxWidth()
    .tvContentFocus(requester)
    .tvClickable(
        shape = SettingsRowShape,
        focusedScale = SettingsRowFocusScale,
        onClick = onClick,
    )

@Composable
fun SettingsPanel(
    email: String?,
    onLogout: () -> Unit,
    onNavigateTo: (SettingsSubScreen) -> Unit,
    initialFocusRequester: FocusRequester? = null,
) {
    SettingsColumn {
        SectionTitle("Settings")

        // Account / Profile card
        ThemedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = email ?: "Stremio Account",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (email != null) "Logged in" else "Guest Mode",
                            color = MutedText,
                            fontSize = 13.sp,
                        )
                    }
                }
                AuthButton(
                    text = "Log out",
                    containerColor = Color(0x15FFFFFF),
                    onClick = onLogout,
                )
            }
        }

        // Navigation Menu Categories
        Column(
            verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(10.dp))
        ) {
            SettingsMenuRow(
                icon = Icons.Outlined.Extension,
                title = "Addons",
                description = "Manage installed and community addons",
                onClick = { onNavigateTo(SettingsSubScreen.Addons) },
                initialFocusRequester = initialFocusRequester,
            )
            SettingsMenuRow(
                icon = Icons.Outlined.AccountCircle,
                title = "General Settings",
                description = "Trakt integration, support, and account links",
                onClick = { onNavigateTo(SettingsSubScreen.General) }
            )
            SettingsMenuRow(
                icon = Icons.Outlined.Language,
                title = "Interface Settings",
                description = "Language preferences and UI toggles",
                onClick = { onNavigateTo(SettingsSubScreen.Interface) }
            )
            SettingsMenuRow(
                icon = Icons.Outlined.PlayCircle,
                title = "Player Settings",
                description = "Subtitles, hardware decoding, and playback tools",
                onClick = { onNavigateTo(SettingsSubScreen.Player) }
            )
            SettingsMenuRow(
                icon = Icons.Outlined.Cloud,
                title = "Streaming Server",
                description = "Configure connections, cache, and peer limits",
                onClick = { onNavigateTo(SettingsSubScreen.Streaming) }
            )
            SettingsMenuRow(
                icon = Icons.Outlined.Android,
                title = "Android Settings",
                description = "Auto-start on boot, WakeLock, and foreground service",
                onClick = { onNavigateTo(SettingsSubScreen.Android) }
            )

            SettingsMenuRow(
                icon = Icons.Outlined.Info,
                title = "Info & About",
                description = "App version, directories, and diagnostics info",
                onClick = { onNavigateTo(SettingsSubScreen.Info) }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AuthButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
) {
    ThemedButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = containerColor,
    )
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
) {
    val triggerHaptic = rememberGlobalHapticFeedback()
    ThemedCard(
        modifier = Modifier.settingsRowClickable(
            onClick = {
                triggerHaptic()
                onClick()
            },
            requester = initialFocusRequester,
        ),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = description,
                        color = MutedText,
                        fontSize = 12.sp,
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingsHeader(
    title: String,
    onBack: () -> Unit
) {
    val triggerHaptic = rememberGlobalHapticFeedback()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (LocalIsTv.current) {
            TvBackButton(
                onClick = {
                    triggerHaptic()
                    onBack()
                },
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        triggerHaptic()
                        onBack()
                    },
            )
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    ThemedCard(
        modifier = Modifier.settingsRowClickable(
            onClick = { onCheckedChange(!checked) },
        ),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = MutedText,
                        fontSize = 12.sp,
                    )
                }
            }
            ThemedToggle(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.tvNotFocusable(),
            )
        }
    }
}

@Composable
fun <T> SettingsDropdownRow(
    title: String,
    selectedValue: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    description: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedValue }?.second ?: selectedValue.toString()
    val triggerHaptic = rememberGlobalHapticFeedback()

    ThemedCard(
        modifier = Modifier.settingsRowClickable(
            onClick = {
                triggerHaptic()
                expanded = true
            },
        ),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = MutedText,
                        fontSize = 12.sp,
                    )
                }
            }
            Box {
                Text(
                    text = selectedLabel,
                    color = AccentPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                ThemedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(text = label, color = Color.White) },
                            onClick = {
                                triggerHaptic()
                                onSelect(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    displayValue: String = "${(value * 100).toInt()}%",
    description: String? = null
) {
    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            color = MutedText,
                            fontSize = 12.sp,
                        )
                    }
                }
                Text(
                    text = displayValue,
                    color = AccentPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            ThemedSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsClickRow(
    title: String,
    onClick: () -> Unit,
    description: String? = null
) {
    val triggerHaptic = rememberGlobalHapticFeedback()
    ThemedCard(
        modifier = Modifier.settingsRowClickable(
            onClick = {
                triggerHaptic()
                onClick()
            },
        ),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = MutedText,
                        fontSize = 12.sp,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
