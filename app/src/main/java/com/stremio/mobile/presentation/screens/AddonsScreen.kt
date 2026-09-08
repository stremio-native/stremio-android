package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.stremio.core.types.addon.ResourceRequest
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.data.model.AddonItem
import com.stremio.mobile.presentation.components.AddonRow
import com.stremio.mobile.presentation.components.EmptyState
import com.stremio.mobile.presentation.components.LoadingRow
import com.stremio.mobile.presentation.components.ThemedButton
import com.stremio.mobile.presentation.components.tvListItemSpacing
import com.stremio.mobile.presentation.state.AddonsUiState
import androidx.compose.ui.graphics.Color

@Composable
fun AddonsScreen(
    state: AddonsUiState,
    backdrop: LayerBackdrop?,
    onBack: () -> Unit,
    onSelectFilter: (ResourceRequest) -> Unit,
    onOpenDetails: (String) -> Unit,
    onInstall: (AddonItem) -> Unit,
    onUninstall: (AddonItem) -> Unit,
    onInstallByUrl: (String) -> Boolean,
) {
    SettingsColumn {
        AddonsHeaderAndControls(
            state = state,
            backdrop = backdrop,
            onBack = onBack,
            onSelectFilter = onSelectFilter,
            onInstallByUrl = onInstallByUrl,
        )

        when {
            state.isLoading -> LoadingRow()
            state.error != null -> EmptyState(state.error)
            state.items.isEmpty() -> EmptyState(
                if (state.isBrowsingRemote) "No addons found in this catalog." else "No addons installed."
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(10.dp))) {
                state.items.forEach { addon ->
                    AddonRow(
                        addon = addon,
                        onClick = { onOpenDetails(addon.transportUrl) },
                        onInstall = { onInstall(addon) },
                        onUninstall = { onUninstall(addon) },
                    )
                }
            }
        }
    }
}

@Composable
fun AddonsHeaderAndControls(
    state: AddonsUiState,
    backdrop: LayerBackdrop?,
    onBack: () -> Unit,
    onSelectFilter: (ResourceRequest) -> Unit,
    onInstallByUrl: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var urlInput by rememberSaveable { mutableStateOf("") }
    var urlError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(16.dp)),
    ) {
        SettingsHeader(title = "Addons", onBack = onBack)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it; urlError = false },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Paste addon manifest URL...",
                            color = MutedText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassSurface,
                        unfocusedContainerColor = GlassSurface,
                        focusedBorderColor = Color(0x33FFFFFF),
                        unfocusedBorderColor = Color(0x19FFFFFF),
                        cursorColor = AccentPurple,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                )
                ThemedButton(
                    text = "Add",
                    onClick = {
                        if (onInstallByUrl(urlInput)) urlInput = "" else urlError = true
                    },
                    containerColor = AccentPurple,
                )
            }
            if (urlError) {
                Text(
                    text = "Enter a valid http(s) addon manifest URL.",
                    color = Color(0xFFE57373),
                    fontSize = 12.sp,
                )
            }
        }

        if (state.selectableCatalogs.isNotEmpty() || state.selectableTypes.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.selectableCatalogs.isNotEmpty()) {
                    val selectedIndex = state.selectableCatalogs.indexOfFirst { it.selected }
                    DropdownFilter(
                        label = state.selectableCatalogs.getOrNull(selectedIndex)?.label ?: "Source",
                        options = state.selectableCatalogs.map { it.label },
                        selectedIndex = selectedIndex,
                        onSelectIndex = { index -> onSelectFilter(state.selectableCatalogs[index].request) },
                        backdrop = backdrop,
                    )
                }
                if (state.selectableTypes.isNotEmpty()) {
                    val selectedIndex = state.selectableTypes.indexOfFirst { it.selected }
                    DropdownFilter(
                        label = state.selectableTypes.getOrNull(selectedIndex)?.label ?: "Type",
                        options = state.selectableTypes.map { it.label },
                        selectedIndex = selectedIndex,
                        onSelectIndex = { index -> onSelectFilter(state.selectableTypes[index].request) },
                        backdrop = backdrop,
                    )
                }
            }
        }
    }
}
