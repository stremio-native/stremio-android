package com.stremio.mobile.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.presentation.components.ThemedCard
import com.stremio.mobile.presentation.components.ThemedButton
import com.stremio.mobile.presentation.components.rememberGlobalHapticFeedback
import com.stremio.mobile.presentation.components.tvListItemSpacing

@Composable
fun GeneralSettingsScreen(
    isAuthenticated: Boolean,
    isTraktAuthenticated: Boolean,
    onAuthenticateTrakt: (Context) -> Unit,
    onLogoutTrakt: () -> Unit,
    onInstallTraktAddon: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    SettingsColumn {
        SettingsHeader(title = "General Settings", onBack = onBack)

        Text(
            text = "INTEGRATIONS",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        // Trakt.tv Integration Row
        ThemedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
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
                    Text(
                        text = "Trakt Integration",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (isTraktAuthenticated) "Authenticated" else "Not Authenticated",
                        color = if (isTraktAuthenticated) Color(0xFF4CAF50) else MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Sync what you watch on Stremio directly to your Trakt profile history and watchlist.",
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isTraktAuthenticated) {
                        ThemedButton(
                            text = "Log Out Trakt",
                            onClick = onLogoutTrakt,
                            containerColor = Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f)
                        )
                        ThemedButton(
                            text = "Install Addon",
                            onClick = onInstallTraktAddon,
                            containerColor = AccentPurple,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        ThemedButton(
                            text = "Authenticate Trakt",
                            onClick = { onAuthenticateTrakt(context) },
                            enabled = isAuthenticated,
                            containerColor = AccentPurple,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Text(
            text = "LINKS",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        var showAppPrivacyDialog by remember { mutableStateOf(false) }

        Column(
            verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(10.dp))
        ) {
            SettingsLinkRow(title = "Help / Support Center", url = "https://stremio.zendesk.com/hc/en-us")
            SettingsLinkRow(title = "Terms of Service", url = "https://www.stremio.com/tos")
            SettingsLinkRow(title = "Stremio Privacy Policy", url = "https://www.stremio.com/privacy")
            SettingsClickRow(title = "App Privacy Policy", onClick = { showAppPrivacyDialog = true })
        }

        if (showAppPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showAppPrivacyDialog = false })
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    url: String
) {
    val context = LocalContext.current
    val triggerHaptic = rememberGlobalHapticFeedback()
    ThemedCard(
        modifier = Modifier.settingsRowClickable(
            onClick = {
                triggerHaptic()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
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
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "App Privacy Policy",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "This open-source Stremio client values your privacy. Please read our practices below:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    PrivacyBulletPoint(
                        title = "1. Anonymous Analytics & Diagnostics",
                        description = "We collect anonymous usage telemetry via PostHog and crash reports via Firebase Crashlytics to diagnose errors and optimize app performance."
                    )
                    PrivacyBulletPoint(
                        title = "2. Absolute PII Sanitization",
                        description = "The client runs filters locally on your device to scrub any personally identifiable information (PII) like email addresses, passwords, or authentication tokens from logs and crash details before transmission."
                    )
                    PrivacyBulletPoint(
                        title = "3. Session Recording Masking",
                        description = "For session replay diagnostics, all typed text input boxes and image files are completely masked and pixelated on-device, ensuring no personal data ever leaves the device."
                    )
                    PrivacyBulletPoint(
                        title = "4. Opt-Out Controls",
                        description = "You can disable analytics collection at any time in Settings -> Android Settings -> Share Diagnostics & Analytics."
                    )
                    PrivacyBulletPoint(
                        title = "5. Upstream Stremio Telemetry",
                        description = "IMPORTANT: The backend Stremio core, default add-ons, and stream providers run independently of this application and may collect separate telemetry according to Stremio's general privacy policy."
                    )
                }
        },
        confirmButton = {
            ThemedButton(
                text = "Dismiss",
                onClick = onDismiss,
                containerColor = AccentPurple
            )
        },
        containerColor = GlassSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun PrivacyBulletPoint(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            color = MutedText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
