package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.core.theme.StremioBackgroundBrush
import com.stremio.mobile.data.model.EpisodeOption
import com.stremio.mobile.data.model.StreamOption
import com.stremio.mobile.data.model.StreamSortCriterion
import com.stremio.mobile.data.model.parseSeedCount
import com.stremio.mobile.data.model.parseSizeBytes
import com.stremio.mobile.data.model.qualityScore
import com.stremio.mobile.presentation.components.ThemedCard
import com.stremio.mobile.presentation.components.ThemedChip
import com.stremio.mobile.presentation.components.ThemedIconButton
import com.stremio.mobile.presentation.components.TvBackButton
import com.stremio.mobile.presentation.components.LocalIsTv
import com.stremio.mobile.presentation.components.TvOverscan
import com.stremio.mobile.presentation.components.TvRequestFocus
import com.stremio.mobile.presentation.components.tvClickable
import com.stremio.mobile.presentation.components.tvContentFocus
import com.stremio.mobile.presentation.components.tvEscapeListUp
import com.stremio.mobile.presentation.components.tvFocusVertical
import com.stremio.mobile.presentation.components.tvListItemSpacing
import com.stremio.mobile.presentation.state.StreamsUiState

@Composable
fun StreamsSheet(
    state: StreamsUiState,
    preferredQuality: String,
    onBack: () -> Unit,
    onSelect: (StreamOption) -> Unit,
    onSelectEpisode: (EpisodeOption) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onSelectProvider: (String?) -> Unit,
    onSelectSortCriterion: (StreamSortCriterion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val chromeFocus = remember { FocusRequester() }
    TvRequestFocus(listFocus, key = Triple(state.showingEpisodes, state.selectedSeason, state.streams.firstOrNull()?.key))
    val isTv = LocalIsTv.current
    val hasSeasonChrome = state.showingEpisodes && state.seasons.size > 1
    val hasStreamChrome = !state.showingEpisodes && (
        state.streams.map { it.addonTitle }.distinct().size > 1 ||
            state.sortCriterion != StreamSortCriterion.DEFAULT
        )
    val hasChrome = hasSeasonChrome || hasStreamChrome
    val upFromList = if (hasChrome) chromeFocus else backFocus
    val downFromBack = if (hasChrome) chromeFocus else listFocus
    var firstRowFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StremioBackgroundBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                horizontal = if (isTv) TvOverscan else 18.dp,
                vertical = if (isTv) 20.dp else 16.dp,
            )
            .tvEscapeListUp(
                enabled = firstRowFocused,
                target = upFromList,
                fallback = backFocus,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isTv) {
                TvBackButton(
                    onClick = onBack,
                    modifier = Modifier
                        .focusRequester(backFocus)
                        .focusProperties {
                            down = downFromBack
                            up = FocusRequester.Cancel
                        },
                )
            } else {
                ThemedIconButton(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    containerColor = GlassSurface,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.showingEpisodes) "Episodes" else "Streams",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                state.forItem?.let {
                    Text(
                        text = buildString {
                            append(it.name)
                            state.selectedEpisodeLabel?.let { label ->
                                append(" · $label")
                            }
                        },
                        color = MutedText,
                        fontSize = 14.sp,
                        maxLines = if (isTv) Int.MAX_VALUE else 1,
                        overflow = if (isTv) TextOverflow.Clip else TextOverflow.Ellipsis,
                    )
                }
                state.releaseDateLabel?.takeIf { it.isNotBlank() }?.let { releaseDate ->
                    Text(
                        text = "Released $releaseDate",
                        color = MutedText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (state.isResolving) {
                CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when {
            state.error != null && state.streams.isEmpty() && state.episodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error,
                        color = Color(0xFFFFC66D),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            state.isLoading && state.streams.isEmpty() && state.episodes.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (state.isSeries) "Loading episodes…" else "Finding streams across your addons…",
                        color = MutedText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            state.showingEpisodes -> {
                val filteredEpisodes = state.episodes.filter {
                    state.selectedSeason == null || it.season == state.selectedSeason
                }
                if (state.seasons.size > 1) {
                    SeasonChipRow(
                        seasons = state.seasons,
                        selectedSeason = state.selectedSeason,
                        chromeFocus = chromeFocus,
                        backFocus = backFocus,
                        listFocus = listFocus,
                        onSelectSeason = onSelectSeason,
                    )
                }
                val phoneListState = rememberLazyListState()
                val targetIndex = remember(filteredEpisodes) {
                    val currentIndex = filteredEpisodes.indexOfFirst { it.isCurrent }
                    if (currentIndex != -1) {
                        currentIndex
                    } else {
                        filteredEpisodes.indexOfLast { it.watched }
                    }
                }
                if (!isTv) {
                    LaunchedEffect(targetIndex) {
                        if (targetIndex != -1) {
                            phoneListState.scrollToItem(targetIndex)
                        } else {
                            phoneListState.scrollToItem(0)
                        }
                    }
                }
                val focusIndex = if (isTv) {
                    0
                } else {
                    filteredEpisodes.indexOfFirst { it.isCurrent }.let { if (it >= 0) it else 0 }
                }
                TvAwareItemList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    itemCount = filteredEpisodes.size,
                    itemSpacing = tvListItemSpacing(8.dp),
                    lazyState = phoneListState,
                    keyForIndex = { filteredEpisodes[it].videoId },
                ) { index ->
                    val episode = filteredEpisodes[index]
                    EpisodeRow(
                        episode = episode,
                        onClick = { onSelectEpisode(episode) },
                        modifier = Modifier.listEdgeFocus(
                            index = index,
                            focusIndex = focusIndex,
                            listFocus = listFocus,
                            upFromList = upFromList,
                            backFocus = backFocus,
                            onFirstRowFocusChange = { firstRowFocused = it },
                        ),
                    )
                }
            }
            else -> {
                // Loading streams for a selected episode
                if (state.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Finding streams across your addons…",
                            color = MutedText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val providers = remember(state.streams) { state.streams.map { it.addonTitle }.distinct() }
                    val visibleStreams = remember(state.streams, state.selectedProvider, state.sortCriterion, preferredQuality) {
                        state.streams
                            .filter { state.selectedProvider == null || it.addonTitle == state.selectedProvider }
                            .let { filtered ->
                                when (state.sortCriterion) {
                                    StreamSortCriterion.DEFAULT -> filtered
                                    StreamSortCriterion.SEEDS -> filtered.sortedByDescending { parseSeedCount(it.seeds) }
                                    StreamSortCriterion.SIZE -> filtered.sortedByDescending { parseSizeBytes(it.size) }
                                    StreamSortCriterion.QUALITY -> filtered.sortedByDescending { qualityScore(it.quality, preferredQuality) }
                                }
                            }
                    }

                    if (providers.size > 1 || state.sortCriterion != StreamSortCriterion.DEFAULT) {
                        StreamChromeRow(
                            providers = providers,
                            selectedProvider = state.selectedProvider,
                            sortCriterion = state.sortCriterion,
                            chromeFocus = chromeFocus,
                            backFocus = backFocus,
                            listFocus = listFocus,
                            onSelectProvider = onSelectProvider,
                            onSelectSortCriterion = onSelectSortCriterion,
                        )
                    }

                    TvAwareItemList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        itemCount = visibleStreams.size,
                        itemSpacing = tvListItemSpacing(12.dp),
                        keyForIndex = { visibleStreams[it].key },
                    ) { index ->
                        val option = visibleStreams[index]
                        StreamRow(
                            option = option,
                            enabled = !state.isResolving,
                            onSelect = { onSelect(option) },
                            modifier = Modifier.listEdgeFocus(
                                index = index,
                                focusIndex = 0,
                                listFocus = listFocus,
                                upFromList = upFromList,
                                backFocus = backFocus,
                                onFirstRowFocusChange = { firstRowFocused = it },
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.listEdgeFocus(
    index: Int,
    focusIndex: Int,
    listFocus: FocusRequester,
    upFromList: FocusRequester,
    backFocus: FocusRequester,
    onFirstRowFocusChange: (Boolean) -> Unit,
): Modifier = this
    .then(
        if (index == 0) {
            Modifier.onFocusChanged { focus ->
                onFirstRowFocusChange(focus.isFocused || focus.hasFocus)
            }
        } else {
            Modifier
        },
    )
    .then(if (index == focusIndex) Modifier.tvContentFocus(listFocus) else Modifier)
    .then(if (index == 0) Modifier.tvFocusVertical(up = upFromList) else Modifier)
    .tvEscapeListUp(
        enabled = index == 0,
        target = upFromList,
        fallback = backFocus,
    )

@Composable
private fun TvAwareItemList(
    itemCount: Int,
    itemSpacing: Dp,
    modifier: Modifier = Modifier,
    lazyState: LazyListState = rememberLazyListState(),
    keyForIndex: (Int) -> Any,
    item: @Composable (index: Int) -> Unit,
) {
    val isTv = LocalIsTv.current
    if (isTv) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            repeat(itemCount) { index -> item(index) }
            Spacer(modifier = Modifier.height(24.dp))
        }
    } else {
        LazyColumn(
            state = lazyState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = modifier,
        ) {
            items(
                count = itemCount,
                key = keyForIndex,
            ) { index ->
                item(index)
            }
        }
    }
}

@Composable
private fun SeasonChipRow(
    seasons: List<Int>,
    selectedSeason: Int?,
    chromeFocus: FocusRequester,
    backFocus: FocusRequester,
    listFocus: FocusRequester,
    onSelectSeason: (Int) -> Unit,
) {
    val isTv = LocalIsTv.current
    val chip: @Composable (Int) -> Unit = { season ->
        val selected = season == selectedSeason
        ThemedChip(
            modifier = Modifier
                .then(if (season == seasons.first()) Modifier.focusRequester(chromeFocus) else Modifier)
                .tvFocusVertical(up = backFocus, down = listFocus),
            selected = selected,
            onClick = { onSelectSeason(season) },
        ) {
            Text(
                text = "Season $season",
                color = if (selected) Color.White else MutedText,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
    if (isTv) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            seasons.forEach { chip(it) }
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(seasons) { season -> chip(season) }
        }
    }
}

@Composable
private fun StreamChromeRow(
    providers: List<String>,
    selectedProvider: String?,
    sortCriterion: StreamSortCriterion,
    chromeFocus: FocusRequester,
    backFocus: FocusRequester,
    listFocus: FocusRequester,
    onSelectProvider: (String?) -> Unit,
    onSelectSortCriterion: (StreamSortCriterion) -> Unit,
) {
    val isTv = LocalIsTv.current
    val providerChip: @Composable (String?, Boolean) -> Unit = { provider, attachChrome ->
        FilterChip(
            label = provider ?: "All",
            selected = if (provider == null) selectedProvider == null else selectedProvider == provider,
            onClick = { onSelectProvider(provider) },
            modifier = Modifier
                .then(if (attachChrome) Modifier.focusRequester(chromeFocus) else Modifier)
                .tvFocusVertical(up = backFocus, down = listFocus),
        )
    }
    val sortChips: @Composable () -> Unit = {
        StreamSortCriterion.entries.forEachIndexed { index, criterion ->
            FilterChip(
                label = "Sort: ${criterion.label}",
                selected = sortCriterion == criterion,
                onClick = { onSelectSortCriterion(criterion) },
                modifier = Modifier
                    .then(
                        if (providers.size <= 1 && index == 0) {
                            Modifier.focusRequester(chromeFocus)
                        } else {
                            Modifier
                        },
                    )
                    .tvFocusVertical(up = backFocus, down = listFocus),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isTv) {
            if (providers.size > 1) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    providerChip(null, true)
                    providers.forEach { providerChip(it, false) }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sortChips()
            }
        } else {
            if (providers.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { providerChip(null, true) }
                    items(providers) { provider -> providerChip(provider, false) }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(StreamSortCriterion.entries.toList()) { index, criterion ->
                    FilterChip(
                        label = "Sort: ${criterion.label}",
                        selected = sortCriterion == criterion,
                        onClick = { onSelectSortCriterion(criterion) },
                        modifier = Modifier
                            .then(
                                if (providers.size <= 1 && index == 0) {
                                    Modifier.focusRequester(chromeFocus)
                                } else {
                                    Modifier
                                },
                            )
                            .tvFocusVertical(up = backFocus, down = listFocus),
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemedCard(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(14.dp), onClick = onClick),
        cornerRadius = 14.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (episode.isCurrent) Color(0x332A2042) else Color.Transparent)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF202033)),
        ) {
            if (!episode.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = episode.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "E${episode.episode}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (episode.isCurrent) AccentPurple else Color(0xB3000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "E${episode.episode}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "E${episode.episode}. ${episode.title}",
                color = Color.White,
                fontSize = if (LocalIsTv.current) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (LocalIsTv.current) Int.MAX_VALUE else 1,
                overflow = if (LocalIsTv.current) TextOverflow.Clip else TextOverflow.Ellipsis,
            )
            episode.releaseDate?.takeIf { it.isNotBlank() }?.let { releaseDate ->
                Text(
                    text = releaseDate,
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
            if (episode.watched) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentPurple)
                )
            }
        }
    }
}

@Composable
private fun StreamRow(
    option: StreamOption,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTv.current
    ThemedCard(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(enabled = enabled, shape = RoundedCornerShape(16.dp), onClick = onSelect),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTv) 18.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTv) 48.dp else 40.dp)
                    .clip(CircleShape)
                    .background(AccentPurple),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isTv) 26.dp else 22.dp),
                )
            }
            Spacer(modifier = Modifier.width(if (isTv) 16.dp else 12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isTv) 8.dp else 4.dp),
            ) {
                Text(
                    text = option.name,
                    color = Color.White,
                    fontSize = if (isTv) 17.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (isTv) Int.MAX_VALUE else 1,
                    overflow = if (isTv) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    option.quality?.let { qual ->
                        StreamMetaBadge(
                            text = qual.uppercase(),
                            background = Color(0xFF3B3B4F),
                            color = Color.White,
                        )
                    }
                    if (option.addonTitle.isNotBlank()) {
                        StreamMetaBadge(
                            text = option.addonTitle,
                            background = AccentPurple.copy(alpha = 0.12f),
                            color = AccentPurple,
                            border = true,
                        )
                    }
                    option.size?.let { sz ->
                        StreamMetaChip(
                            icon = Icons.Outlined.Storage,
                            text = sz,
                            tint = MutedText,
                        )
                    }
                    option.seeds?.let { s ->
                        StreamMetaChip(
                            icon = Icons.Outlined.Person,
                            text = s,
                            tint = Color(0xFF81C784),
                            iconTint = Color(0xFF4CAF50),
                        )
                    }
                    option.origin?.let { o ->
                        StreamMetaChip(
                            icon = Icons.Outlined.Cloud,
                            text = o,
                            tint = MutedText,
                        )
                    }
                }

                val cleanDesc = option.cleanDescription ?: option.description ?: option.addonTitle
                if (cleanDesc.isNotBlank()) {
                    Text(
                        text = cleanDesc,
                        color = MutedText,
                        fontSize = if (isTv) 14.sp else 12.sp,
                        maxLines = if (isTv) Int.MAX_VALUE else 3,
                        overflow = if (isTv) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamMetaBadge(
    text: String,
    background: Color,
    color: Color,
    border: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .then(
                if (border) Modifier.border(1.dp, AccentPurple.copy(alpha = 0.24f), RoundedCornerShape(4.dp))
                else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun StreamMetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color,
    iconTint: Color = tint,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemedChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MutedText,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
