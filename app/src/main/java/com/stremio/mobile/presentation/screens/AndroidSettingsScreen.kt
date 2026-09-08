package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.MutedText

@Composable
fun AndroidSettingsScreen(
    isAutoStartOnBoot: Boolean,
    onSetAutoStartOnBoot: (Boolean) -> Unit,
    isServerInForeground: Boolean,
    onSetServerInForeground: (Boolean) -> Unit,
    isMobileDataWarning: Boolean,
    onSetMobileDataWarning: (Boolean) -> Unit,
    isKeepScreenOn: Boolean,
    onSetKeepScreenOn: (Boolean) -> Unit,
    isAnalyticsEnabled: Boolean,
    onSetAnalyticsEnabled: (Boolean) -> Unit,
    onShowAnalyticsDisclosure: () -> Unit,
    onBack: () -> Unit
) {
    SettingsColumn {
        SettingsHeader(title = "Android Settings", onBack = onBack)

        Text(
            text = "SYSTEM & INTEGRATION",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        SettingsToggleRow(
            title = "Auto-Start Server on Boot",
            checked = isAutoStartOnBoot,
            onCheckedChange = onSetAutoStartOnBoot,
            description = "Start the local torrent streaming server when your phone boots"
        )

        SettingsToggleRow(
            title = "Server Foreground Service",
            checked = isServerInForeground,
            onCheckedChange = onSetServerInForeground,
            description = "Run server as a foreground service with notification (prevents Android from killing it)"
        )

        SettingsToggleRow(
            title = "Keep Screen On",
            checked = isKeepScreenOn,
            onCheckedChange = onSetKeepScreenOn,
            description = "Prevent the screen from turning off during video playback"
        )

        Text(
            text = "NETWORK & DATA USAGE",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        SettingsToggleRow(
            title = "Mobile Data warning",
            checked = isMobileDataWarning,
            onCheckedChange = onSetMobileDataWarning,
            description = "Warn before streaming movies or TV shows over cellular connections"
        )

        Text(
            text = "PRIVACY & DIAGNOSTICS",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        SettingsToggleRow(
            title = "Share Diagnostics & Analytics",
            checked = isAnalyticsEnabled,
            onCheckedChange = onSetAnalyticsEnabled,
            description = "Help improve the app by sharing anonymous crash reports and usage statistics"
        )

        SettingsClickRow(
            title = "Privacy & Analytics Disclosure",
            onClick = onShowAnalyticsDisclosure,
            description = "Read disclosure about how anonymous crash reports and usage statistics are processed"
        )
    }
}
