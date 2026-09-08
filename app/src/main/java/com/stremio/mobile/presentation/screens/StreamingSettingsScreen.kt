package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.presentation.components.ThemedButton
import com.stremio.mobile.presentation.components.ThemedCard
import com.stremio.mobile.server.StreamingServerState
import com.stremio.mobile.server.formatServerErrorMessage

@Composable
fun StreamingSettingsScreen(
    serverState: StreamingServerState,
    serverSettings: com.stremio.core.models.StreamingServer.Settings?,
    isSeedingEnabled: Boolean,
    minSeedsThreshold: Int,
    minDownloadSpeedBps: Long,
    preferredQuality: String,
    isAutoSwitchOnDeadStream: Boolean,
    onUpdateServerSettings: (com.stremio.core.models.StreamingServer.Settings) -> Unit,
    onSetSeedingEnabled: (Boolean) -> Unit,
    onSetMinSeedsThreshold: (Int) -> Unit,
    onSetMinDownloadSpeedBps: (Long) -> Unit,
    onSetPreferredQuality: (String) -> Unit,
    onSetAutoSwitchOnDeadStream: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    SettingsColumn {
        SettingsHeader(title = "Streaming Server", onBack = onBack)

        // Status Card
        ThemedCard(
            modifier = Modifier
                .fillMaxWidth(),
            cornerRadius = 20.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Server Status",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val dotColor = when (serverState) {
                        is StreamingServerState.Ready -> Color(0xFF4CAF50)
                        is StreamingServerState.Starting -> Color(0xFFFFC107)
                        is StreamingServerState.Stopped -> Color(0xFF9E9E9E)
                        is StreamingServerState.Failed -> Color(0xFFF44336)
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    val statusText = when (serverState) {
                        is StreamingServerState.Ready -> "Running"
                        is StreamingServerState.Starting -> "Starting..."
                        is StreamingServerState.Stopped -> "Stopped"
                        is StreamingServerState.Failed -> "Failed: ${formatServerErrorMessage(serverState.message)}"
                    }
                    Text(
                        text = "Status: $statusText",
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                }

                if (serverState is StreamingServerState.Ready) {
                    Text(
                        text = "URL: ${serverState.baseUrl}",
                        color = MutedText,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        if (serverSettings != null) {
            Text(
                text = "SERVER OPTIONS",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            val cacheSizes = listOf(
                0.0 to "Disabled",
                2147483648.0 to "2 GB",
                5368709120.0 to "5 GB",
                10737418240.0 to "10 GB",
                21474836480.0 to "20 GB"
            )

            val torrentProfiles = listOf(
                200L to "Default (Balanced)",
                80L to "Soft (Low Connections)",
                500L to "Fast (High Connections)",
                800L to "Ultra Fast (Unrestricted)"
            )

            val transcodeProfiles = listOf(
                "disabled" to "No Transcoding",
                "default" to "Default (Balanced)",
                "fast" to "Fast (Lower Quality)",
                "slow" to "High Quality (High CPU)"
            )

            SettingsDropdownRow(
                title = "Server Cache Size",
                selectedValue = serverSettings.cacheSize ?: 10737418240.0,
                options = cacheSizes,
                onSelect = { onUpdateServerSettings(serverSettings.copy(cacheSize = it)) },
                description = "Disk cache allocated for torrent file buffers"
            )

            SettingsDropdownRow(
                title = "Torrent Connections Limit",
                selectedValue = serverSettings.btMaxConnections,
                options = torrentProfiles,
                onSelect = { onUpdateServerSettings(serverSettings.copy(btMaxConnections = it)) },
                description = "Maximum peers that can connect simultaneously"
            )

            SettingsDropdownRow(
                title = "Transcoding Profile",
                selectedValue = serverSettings.transcodeProfile ?: "default",
                options = transcodeProfiles,
                onSelect = { onUpdateServerSettings(serverSettings.copy(transcodeProfile = it)) },
                description = "Adjust CPU usage for mobile video stream transcoding"
            )

            SettingsToggleRow(
                title = "Proxy Video Streams",
                checked = serverSettings.proxyStreamsEnabled,
                onCheckedChange = { onUpdateServerSettings(serverSettings.copy(proxyStreamsEnabled = it)) },
                description = "Force video player stream traffic through server proxy"
            )

            SettingsToggleRow(
                title = "Seeding Enabled",
                checked = isSeedingEnabled,
                onCheckedChange = onSetSeedingEnabled,
                description = "Continue seeding torrents in background after download finishes"
            )
        }

        Text(
            text = "STREAM HEALTH",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        val minSeedsOptions = listOf(0 to "Any", 1 to "1", 2 to "2", 3 to "3", 5 to "5")
        val minSpeedOptions = listOf(
            0L to "Disabled",
            25_000L to "25 KB/s",
            50_000L to "50 KB/s",
            100_000L to "100 KB/s",
            200_000L to "200 KB/s",
        )
        val qualityOptions = listOf(
            "Any" to "Any",
            "2160p" to "4K",
            "1080p" to "1080p",
            "720p" to "720p",
            "480p" to "480p",
        )

        SettingsDropdownRow(
            title = "Minimum Seeds",
            selectedValue = minSeedsThreshold,
            options = minSeedsOptions,
            onSelect = onSetMinSeedsThreshold,
            description = "Flag a playing stream as dead when it has fewer peers than this"
        )

        SettingsDropdownRow(
            title = "Minimum Download Speed",
            selectedValue = minDownloadSpeedBps,
            options = minSpeedOptions,
            onSelect = onSetMinDownloadSpeedBps,
            description = "Flag a playing stream as too slow below this throughput"
        )

        SettingsDropdownRow(
            title = "Preferred Video Quality",
            selectedValue = preferredQuality,
            options = qualityOptions,
            onSelect = onSetPreferredQuality,
            description = "Used to break ties when sorting streams or picking a fallback"
        )

        SettingsToggleRow(
            title = "Auto-Switch on Dead Stream",
            checked = isAutoSwitchOnDeadStream,
            onCheckedChange = onSetAutoSwitchOnDeadStream,
            description = "Automatically play the next best stream instead of asking"
        )
    }
}
