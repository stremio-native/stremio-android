package com.stremio.mobile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.CardFallback
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.data.model.CatalogItem
import com.stremio.mobile.data.model.CatalogShelf

enum class ShelfMode {
    Continue,
    Movie,
    Series,
}

@Composable
fun PosterShelf(
    shelf: CatalogShelf,
    mode: ShelfMode,
    onItemClick: (CatalogItem) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    initialFocusRequester: FocusRequester? = null,
) {
    val isTv = LocalIsTv.current
    Column(verticalArrangement = Arrangement.spacedBy(tvListItemSpacing(20.dp))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = contentGutter(), end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shelf.title,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = if (isTv) 20.sp else 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isTv && onSeeAllClick != null && shelf.seeAllRequest != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.tvClickable(shape = RoundedCornerShape(10.dp), onClick = onSeeAllClick)
                ) {
                    Text(
                        text = "SEE ALL",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier
                            .padding(start = 5.dp, end = 0.dp)
                            .size(18.dp),
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.tvFocusRestorer(),
            contentPadding = PaddingValues(
                start = contentGutter(),
                end = contentGutter(),
                top = if (isTv) 10.dp else 0.dp,
                bottom = if (isTv) 14.dp else 0.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (isTv) tvListItemSpacing(20.dp) else 16.dp),
        ) {
            when {
                shelf.isLoading -> {
                    items(5, contentType = { "skeleton" }) {
                        PosterSkeleton()
                    }
                }

                shelf.items.isEmpty() -> {
                    item(contentType = "empty") {
                        Text(
                            text = shelf.error ?: "No items available",
                            color = MutedText,
                            modifier = Modifier.padding(start = 18.dp),
                        )
                    }
                }

                else -> {
                    itemsIndexed(shelf.items, key = { _, item -> "${item.type}-${item.id}" }, contentType = { _, _ -> "poster" }) { index, item ->
                        PosterTile(
                            item = item,
                            mode = mode,
                            onClick = { onItemClick(item) },
                            modifier = if (index == 0) Modifier.tvContentFocus(initialFocusRequester) else Modifier,
                        )
                    }
                    if (isTv && onSeeAllClick != null && shelf.seeAllRequest != null) {
                        item(contentType = "see-all") {
                            SeeAllPoster(onClick = onSeeAllClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeeAllPoster(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .aspectRatio(0.66f)
            .tvFocusIndicator(shape)
            .clip(shape)
            .background(Color(0xFF1B1B2C))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = "See all",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PosterTile(
    item: CatalogItem,
    mode: ShelfMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTv.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .width(if (isTv) 150.dp else 112.dp)
            .aspectRatio(0.66f)
            .tvFocusIndicator(shape)
            .clip(shape)
            .background(CardFallback)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.poster)
                .size(224, 340)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1.0f to Color(0xAA000000),
                        ),
                    ),
                ),
        )

        when (mode) {
            ShelfMode.Continue -> {
                val progress = item.progress ?: progressFor(item.id)
                val isCompleted = item.watched

                if (isCompleted) {
                    CircleBadge(
                        imageVector = Icons.Outlined.Check,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    )
                }

                ProgressBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 3.dp, vertical = 4.dp),
                    progress = progress,
                )
            }

            ShelfMode.Movie -> {
                if (item.inCinema) {
                    CinemaBadge(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                    )
                }
            }

            ShelfMode.Series -> Unit
        }

        if (mode == ShelfMode.Continue && item.type == "series" && item.remainingEpisodes != null && item.remainingEpisodes > 0) {
            AddBadge(
                text = "+${item.remainingEpisodes}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 10.dp),
            )
        }
    }
}

@Composable
private fun PosterSkeleton() {
    Box(
        modifier = Modifier
            .width(if (LocalIsTv.current) 150.dp else 112.dp)
            .aspectRatio(0.66f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF222231), Color(0xFF11111C)),
                ),
            ),
    )
}

@Composable
private fun CircleBadge(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(17.dp)
            .clip(CircleShape)
            .background(AccentPurple),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun TextBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AccentPurple)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AddBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
    ) {
        // Bottom stacked card
        Box(
            modifier = Modifier
                .offset(x = (-2).dp, y = 2.dp)
                .height(17.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFC0B5FA)) // slightly darker/saturated lavender card for stack effect
                .padding(horizontal = 5.dp)
        ) {
            Text(
                text = text,
                color = Color.Transparent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Top foreground card
        Row(
            modifier = Modifier
                .height(17.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xDDEFEAFF))
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = AccentPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CinemaBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xCC202631))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = Color(0xFFC9C8D8),
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = "IN CINEMA",
            color = Color(0xFFE8E6F0),
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8E8EE)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .background(AccentPurple),
        )
    }
}

private fun progressFor(id: String): Float {
    val bucket = kotlin.math.abs(id.hashCode() % 46)
    return (bucket + 28) / 100f
}
