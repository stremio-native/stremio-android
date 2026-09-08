package com.stremio.mobile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.core.theme.SearchBackground
import com.stremio.mobile.data.model.AddonItem

/** A single addon in the management list: tap opens details, the trailing button installs/uninstalls. */
@Composable
fun AddonRow(
    addon: AddonItem,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val triggerHaptic = rememberGlobalHapticFeedback()
    ThemedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp, max = 150.dp)
            .tvClickable(shape = RoundedCornerShape(16.dp), focusedScale = 1.025f) {
                triggerHaptic()
                onClick()
            },
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SearchBackground),
                contentAlignment = Alignment.Center,
            ) {
                if (addon.logo != null) {
                    AsyncImage(
                        model = addon.logo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = addon.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (addon.official || addon.protected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (addon.official) AddonBadge("Official")
                        if (addon.protected) AddonBadge("Protected")
                    }
                }
                Text(
                    text = addon.description ?: addon.version?.let { "v$it" } ?: "No description available",
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (addon.types.isNotEmpty()) {
                    Text(
                        text = addon.types.joinToString(" / "),
                        color = AccentPurple,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AddonActionButton(
                addon = addon,
                onInstall = onInstall,
                onUninstall = onUninstall,
            )
        }
    }
}

@Composable
private fun AddonActionButton(
    addon: AddonItem,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
) {
    if (addon.installed) {
        ThemedButton(
            text = if (addon.uninstallable) "Uninstall" else "Installed",
            onClick = onUninstall,
            enabled = addon.uninstallable,
            containerColor = Color(0x26FFFFFF),
            modifier = Modifier
                .widthIn(min = 108.dp, max = 132.dp)
                .height(52.dp),
        )
    } else {
        ThemedButton(
            text = "Install",
            onClick = onInstall,
            enabled = addon.installable,
            containerColor = AccentPurple,
            modifier = Modifier
                .widthIn(min = 108.dp, max = 132.dp)
                .height(52.dp),
        )
    }
}

@Composable
private fun AddonBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x2EFFFFFF))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
