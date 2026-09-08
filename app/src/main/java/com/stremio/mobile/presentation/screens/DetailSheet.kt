package com.stremio.mobile.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.stremio.mobile.core.theme.AccentGreen
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.CardFallback
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.data.model.MetaDetails
import com.stremio.mobile.presentation.components.LocalGlobalUiTheme
import com.stremio.mobile.presentation.components.LocalIsTv
import com.stremio.mobile.presentation.components.drawBackdropSafe
import com.stremio.mobile.presentation.components.rememberGlobalHapticFeedback
import com.stremio.mobile.presentation.components.tvClickable
import com.stremio.mobile.presentation.components.tvFocusIndicator
import com.stremio.mobile.presentation.components.tvFocusTrap
import com.stremio.mobile.presentation.components.TvRequestFocus

@Composable
fun DetailSheet(
    details: MetaDetails,
    inLibrary: Boolean,
    onBack: () -> Unit,
    onToggleLibrary: () -> Unit,
    onOpenStreams: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalIsTv.current) {
        TvDetailScreen(
            details = details,
            inLibrary = inLibrary,
            onBack = onBack,
            onToggleLibrary = onToggleLibrary,
            onOpenStreams = onOpenStreams,
            modifier = modifier,
        )
        return
    }

    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp.dp * 0.82f).coerceAtMost(720.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 420.dp, max = maxSheetHeight)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color(0xF2141422))
            .pointerInput(Unit) {
                var dragAccumulator = 0f
                var hasTriggered = false
                detectVerticalDragGestures(
                    onDragStart = {
                        dragAccumulator = 0f
                        hasTriggered = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (!hasTriggered) {
                            dragAccumulator += dragAmount
                            if (dragAccumulator < -40f) {
                                hasTriggered = true
                                onOpenStreams()
                            }
                        }
                    }
                )
            }
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .background(CardFallback),
        ) {
            AsyncImage(
                model = details.item.background ?: details.item.poster,
                contentDescription = details.item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x66000000), Color(0xFF10101D)),
                        ),
                    ),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .padding(14.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .tvClickable(shape = CircleShape, onClick = onBack)
                    .padding(10.dp),
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = details.item.name,
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(details.year, details.runtime, details.item.imdbRating?.let { "IMDb $it" })
                    .joinToString("  "),
                color = MutedText,
                fontSize = 13.sp,
            )
            if (details.isLoading) {
                CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(24.dp))
            } else if (details.error != null) {
                Text(text = details.error, color = Color(0xFFFFC66D), fontSize = 13.sp)
            } else {
                Text(
                    text = details.description ?: "No summary available.",
                    color = Color(0xFFE4E0EE),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = (details.genres + details.cast.take(3)).joinToString("  "),
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                DetailLiquidActionButton(
                    label = if (inLibrary) "In Library" else "Add",
                    imageVector = if (inLibrary) Icons.Outlined.Check else Icons.Outlined.Add,
                    onClick = onToggleLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    tint = AccentPurple,
                    surface = true,
                )
                DetailLiquidActionButton(
                    label = "Play",
                    imageVector = Icons.Outlined.PlayArrow,
                    onClick = onOpenStreams,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = !details.isLoading,
                    tint = AccentGreen,
                )
            }
        }
    }
}

@Composable
private fun DetailLiquidActionButton(
    label: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: LayerBackdrop? = null,
    surface: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val triggerHaptic = rememberGlobalHapticFeedback()
    val theme = LocalGlobalUiTheme.current
    val tuning = theme.liquidGlassTuning.clamped()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 430f),
        label = "detailLiquidButtonScale",
    )
    val shape = RoundedCornerShape(999.dp)
    val contentColor = if (surface) {
        Color(0xFF101018).copy(alpha = if (enabled) 0.92f else 0.42f)
    } else {
        Color.White.copy(alpha = if (enabled) 0.96f else 0.42f)
    }
    val realGlass = enabled && backdrop != null && theme.style == "modern" && theme.glassEffectsMode != "static"

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .scale(scale)
            .then(
                if (realGlass) {
                    Modifier.drawBackdropSafe(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(
                                refractionHeight = 16f.dp.toPx(),
                                refractionAmount = 32f.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = tuning.chromaticAberration,
                            )
                        },
                        highlight = {
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.4f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.4f,
                                alpha = 0.72f,
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 18.dp,
                                offset = androidx.compose.ui.unit.DpOffset(0.dp, 4.dp),
                                color = Color.Black.copy(alpha = 0.18f),
                            )
                        },
                        onDrawSurface = {
                            if (surface) {
                                drawRoundRect(Color.White.copy(alpha = 0.62f))
                            } else {
                                drawRoundRect(tint, blendMode = BlendMode.Hue)
                                drawRoundRect(tint.copy(alpha = 0.72f))
                            }
                        },
                    )
                } else {
                    Modifier
                        .clip(shape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (surface) {
                                    listOf(
                                        Color.White.copy(alpha = if (enabled) 0.78f else 0.14f),
                                        Color.White.copy(alpha = if (enabled) 0.46f else 0.10f),
                                    )
                                } else {
                                    listOf(
                                        tint.copy(alpha = if (enabled) 0.78f else 0.12f),
                                        tint.copy(alpha = if (enabled) 0.48f else 0.10f),
                                    )
                                },
                            ),
                        )
                }
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (enabled) 0.58f else 0.16f),
                        Color.White.copy(alpha = if (enabled) 0.22f else 0.08f),
                    ),
                ),
                shape = shape,
            )
            .tvFocusIndicator(shape, focusedScale = 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) {
                triggerHaptic()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (enabled) 0.28f else 0.04f),
                            Color.Transparent,
                            Color.White.copy(alpha = if (surface) 0.08f else 0.04f),
                        ),
                    ),
                ),
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TvDetailScreen(
    details: MetaDetails,
    inLibrary: Boolean,
    onBack: () -> Unit,
    onToggleLibrary: () -> Unit,
    onOpenStreams: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playFocus = remember { FocusRequester() }
    TvRequestFocus(playFocus, key = details.item.id)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 420.dp, max = 560.dp)
            .tvFocusTrap(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF2141422)),
        ) {
            AsyncImage(
                model = details.item.background ?: details.item.poster,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xF2141422), Color(0xCC141422), Color(0x66141422)),
                        ),
                    ),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(36.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = details.item.poster,
                contentDescription = details.item.name,
                modifier = Modifier
                    .width(180.dp)
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardFallback),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = details.item.name,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(details.year, details.runtime, details.item.imdbRating?.let { "IMDb $it" })
                        .joinToString("  "),
                    color = MutedText,
                    fontSize = 16.sp,
                )
                Text(
                    text = details.description ?: "No summary available.",
                    color = Color(0xFFE4E0EE),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 600.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 12.dp, end = 8.dp, bottom = 8.dp),
                ) {
                    DetailLiquidActionButton(
                        label = "Play",
                        imageVector = Icons.Outlined.PlayArrow,
                        onClick = onOpenStreams,
                        modifier = Modifier
                            .width(200.dp)
                            .height(56.dp),
                        enabled = !details.isLoading,
                        tint = AccentGreen,
                        focusRequester = playFocus,
                    )
                    DetailLiquidActionButton(
                        label = if (inLibrary) "In Library" else "Add to Library",
                        imageVector = if (inLibrary) Icons.Outlined.Check else Icons.Outlined.Add,
                        onClick = onToggleLibrary,
                        modifier = Modifier
                            .width(220.dp)
                            .height(56.dp),
                        tint = AccentPurple,
                        surface = true,
                    )
                    DetailLiquidActionButton(
                        label = "Back",
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = onBack,
                        modifier = Modifier
                            .width(140.dp)
                            .height(56.dp),
                        tint = Color.White,
                        surface = true,
                    )
                }
            }
        }
    }
}
