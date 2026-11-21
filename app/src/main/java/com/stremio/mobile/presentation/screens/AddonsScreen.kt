package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.stremio.core.types.addon.ResourceRequest
import com.stremio.mobile.data.model.AddonItem
import com.stremio.mobile.presentation.components.AddonRow
import com.stremio.mobile.presentation.components.EmptyState
import com.stremio.mobile.presentation.components.LoadingRow
import com.stremio.mobile.presentation.components.UrlInputRow
import com.stremio.mobile.presentation.state.AddonsUiState

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
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsHeader(title = "Addons", onBack = onBack)

        UrlInputRow(
            value = urlInput,
            onValueChange = { urlInput = it; urlError = false },
            placeholder = "Paste addon manifest URL...",
            buttonText = "Add",
            onSubmit = { if (onInstallByUrl(urlInput)) urlInput = "" else urlError = true },
            errorText = "Enter a valid http(s) addon manifest URL.".takeIf { urlError },
        )

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
