package com.stremio.mobile.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.*
import com.stremio.mobile.data.model.CatalogItem
import com.stremio.mobile.data.model.CatalogShelf
import com.stremio.mobile.presentation.components.PosterShelf
import com.stremio.mobile.presentation.components.PosterTile
import com.stremio.mobile.presentation.components.ShelfMode
import com.stremio.mobile.presentation.components.LocalGlobalUiTheme
import com.stremio.mobile.presentation.components.LocalIsTv
import com.stremio.mobile.presentation.components.ThemedIconButton
import com.stremio.mobile.presentation.components.TvBackButton
import com.stremio.mobile.presentation.components.TvOverscan
import com.stremio.mobile.presentation.components.contentGutter
import com.stremio.mobile.presentation.components.tvListItemSpacing
import com.stremio.mobile.presentation.components.tvFocusIndicator

/**
 * Full-screen search results, shown whenever the query is non-blank. Reuses the Discover-style
 * 3-column [PosterTile] grid and is powered by stremio-core's search across installed catalogs.
 */
@Composable
fun SearchResultsScreen(
    query: String,
    results: CatalogShelf,
    shelves: List<CatalogShelf>,
    onQueryChange: (String) -> Unit,
    onOpenDetails: (CatalogItem) -> Unit,
    onOpenDiscoverCatalog: (com.stremio.core.types.addon.ResourceRequest, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var localQuery by remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        )
    }
    LaunchedEffect(query) {
        if (query != localQuery.text) {
            localQuery = TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val globalTheme = LocalGlobalUiTheme.current
    val fieldContainerColor = if (globalTheme.style == "modern") {
        Color.White.copy(alpha = (globalTheme.glassAlpha * 0.42f + 0.10f).coerceIn(0.10f, 0.36f))
    } else {
        GlassSurface
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StremioBackgroundBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                })
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (LocalIsTv.current) TvOverscan else 11.dp, top = 6.dp, end = contentGutter(), bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (LocalIsTv.current) {
                TvBackButton(onClick = onBack)
            } else {
                ThemedIconButton(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    containerColor = GlassSurface,
                )
            }
            OutlinedTextField(
                value = localQuery,
                onValueChange = { newValue ->
                    localQuery = newValue
                    onQueryChange(newValue.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .focusRequester(focusRequester)
                    .tvFocusIndicator(RoundedCornerShape(18.dp)),
                placeholder = {
                    Text(
                        text = "Search movies, series, anime…",
                        color = MutedText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldContainerColor,
                    unfocusedContainerColor = fieldContainerColor,
                    focusedBorderColor = Color(0x33FFFFFF),
                    unfocusedBorderColor = Color(0x19FFFFFF),
                    cursorColor = AccentPurple,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
        }

        when {
            query.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Start typing to search movies, series, and anime.",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = contentGutter()),
                    )
                }
            }

            results.isLoading && results.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(34.dp))
                }
            }

            results.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = results.error ?: "No results for \"$query\".",
                        color = MutedText,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = contentGutter()),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = BottomBarSpace + navBottom,
                    ),
                    verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(27.dp)),
                ) {
                    items(shelves.size) { index ->
                        val shelf = shelves[index]
                        PosterShelf(
                            shelf = shelf,
                            mode = if (shelf.type == "series") ShelfMode.Series else ShelfMode.Movie,
                            onItemClick = onOpenDetails,
                            onSeeAllClick = {
                                shelf.seeAllRequest?.let { req ->
                                    onOpenDiscoverCatalog(req, shelf.title)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items_grid(
    items: List<CatalogItem>,
    onOpenDetails: (CatalogItem) -> Unit,
) {
    val rows = items.chunked(3)
    items(rows.size) { rowIndex ->
        val rowItems = rows[rowIndex]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            rowItems.forEach { item ->
                Box(modifier = Modifier.weight(1f)) {
                    PosterTile(
                        item = item,
                        mode = if (item.type == "series") ShelfMode.Series else ShelfMode.Movie,
                        onClick = { onOpenDetails(item) },
                    )
                }
            }
            repeat(3 - rowItems.size) {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}
