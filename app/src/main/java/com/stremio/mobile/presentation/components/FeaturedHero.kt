package com.stremio.mobile.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.stremio.mobile.data.model.CatalogItem
import com.stremio.mobile.core.theme.CardFallback

@Composable
fun FeaturedHero(
    item: CatalogItem?,
    onClick: (CatalogItem) -> Unit,
) {
    FeaturedHeroCard(item = item, onClick = onClick)
}

@Composable
fun FeaturedHeroPager(
    items: List<CatalogItem>,
    onClick: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester? = null,
) {
    if (items.isEmpty()) return
    val isTv = LocalIsTv.current
    if (items.size == 1 || isTv) {
        FeaturedHeroCard(
            item = items.first(),
            onClick = onClick,
            modifier = modifier.tvContentFocus(initialFocusRequester),
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(key1 = items) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            FeaturedHeroCard(
                item = items[page],
                onClick = onClick,
            )
        }

        Row(
            Modifier
                .height(20.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val width = animateDpAsState(targetValue = if (isSelected) 12.dp else 6.dp, label = "width")
                val alpha = androidx.compose.animation.core.animateFloatAsState(targetValue = if (isSelected) 1f else 0.4f, label = "alpha")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width.value)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = alpha.value))
                )
            }
        }
    }
}

@Composable
fun FeaturedHeroCard(
    item: CatalogItem?,
    onClick: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item == null) {
        return
    }

    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (LocalIsTv.current) 320.dp else 230.dp)
            .padding(start = contentGutter(), end = contentGutter())
            .tvFocusIndicator(shape)
            .clip(shape)
            .background(CardFallback)
            .clickable { onClick(item) },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.background ?: item.poster)
                .size(800, 460)
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
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xEE050515),
                            0.52f to Color(0x88050515),
                            1.0f to Color(0x22050515),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.68f to Color.Transparent,
                            1.0f to Color(0xFF050515),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassPill(text = "Featured now")
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(item.releaseInfo, item.imdbRating?.let { "IMDb $it" })
                    .joinToString("  "),
                color = Color(0xFFE7E1FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
