@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.stremio.mobile.presentation.components

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.GlassSurface
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.core.theme.ScreenGutter
import com.stremio.mobile.presentation.navigation.AppView
import kotlinx.coroutines.delay

val LocalIsTv = staticCompositionLocalOf { false }

/** Left gutter reserved for the TV navigation rail. */
val TvNavRailWidth = 72.dp

/** 10-foot overscan inset recommended by Android TV. */
val TvOverscan = 32.dp

/** Extra inset so focused rows can scale without clipping against the screen edge. */
val TvContentGutter = 28.dp

@Composable
fun contentGutter(): Dp = if (LocalIsTv.current) TvContentGutter else ScreenGutter

@Composable
fun tvListItemSpacing(phone: Dp = 10.dp): Dp =
    if (LocalIsTv.current) maxOf(phone + 6.dp, 16.dp) else phone

fun isTelevision(context: Context): Boolean {
    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    return uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

fun FocusRequester.requestTvFocusSafely(): Boolean {
    return runCatching { requestFocus() }.getOrDefault(false)
}

/**
 * Visible 10-foot focus treatment. The ring is drawn fully inside the node's bounds so parent
 * clips (cards, dialogs, pills) cannot hide it. Uses both [androidx.compose.ui.focus.FocusState.isFocused]
 * and [androidx.compose.ui.focus.FocusState.hasFocus] so the ring still appears when this
 * modifier sits outside a nested clickable/button focus target.
 */
fun Modifier.tvFocusIndicator(
    shape: Shape = RoundedCornerShape(14.dp),
    enabled: Boolean? = null,
    focusedScale: Float = 1.04f,
): Modifier = composed {
    val active = enabled ?: LocalIsTv.current
    if (!active) {
        this
    } else {
        var focused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (focused) focusedScale else 1f,
            animationSpec = tween(durationMillis = 120),
            label = "tvFocusScale",
        )
        this
            .zIndex(if (focused) 2f else 0f)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = true
                this.shape = shape
            }
            .drawWithContent {
                drawContent()
                if (focused) {
                    val stroke = 3.dp.toPx()
                    val glow = 5.dp.toPx()
                    val inset = glow / 2f + 1.dp.toPx()
                    val inner = Size(
                        (size.width - inset * 2).coerceAtLeast(0f),
                        (size.height - inset * 2).coerceAtLeast(0f),
                    )
                    val outline = shape.createOutline(inner, layoutDirection, this)
                    translate(inset, inset) {
                        drawOutline(
                            outline = outline,
                            color = Color.White.copy(alpha = 0.34f),
                            style = Stroke(width = glow),
                        )
                        drawOutline(
                            outline = outline,
                            color = Color.White,
                            style = Stroke(width = stroke),
                        )
                    }
                }
            }
    }
}

/** Focus ring first, then clickable — the order Compose needs for `isFocused` to fire. */
fun Modifier.tvClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    focusedScale: Float = 1.04f,
    onClick: () -> Unit,
): Modifier = composed {
    val isTv = LocalIsTv.current
    if (isTv) {
        this
            .tvFocusIndicator(shape, focusedScale = focusedScale)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
    } else {
        this.clickable(enabled = enabled, onClick = onClick)
    }
}

fun Modifier.tvFocusRestorer(): Modifier = composed {
    if (LocalIsTv.current) this.focusGroup().focusRestorer() else this
}

fun Modifier.tvContentFocus(requester: FocusRequester?): Modifier =
    if (requester != null) this.focusRequester(requester) else this

fun Modifier.tvNotFocusable(): Modifier = composed {
    if (LocalIsTv.current) focusProperties { canFocus = false } else this
}

fun Modifier.tvFocusVertical(up: FocusRequester? = null, down: FocusRequester? = null): Modifier {
    if (up == null && down == null) return this
    return focusProperties {
        if (up != null) this.up = up
        if (down != null) this.down = down
    }
}

/**
 * Lazy grids/lists often swallow D-pad Up at the first row instead of leaving the list.
 * Intercept Up while [enabled] and move focus to [target] (then [fallback]).
 */
fun Modifier.tvEscapeListUp(
    enabled: Boolean,
    target: FocusRequester,
    fallback: FocusRequester? = null,
): Modifier = composed {
    if (!LocalIsTv.current) {
        this
    } else {
        onPreviewKeyEvent { event ->
            val isUp = event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP ||
                event.key == Key.DirectionUp
            if (!enabled || event.type != KeyEventType.KeyDown || !isUp) {
                false
            } else {
                if (!target.requestTvFocusSafely()) {
                    fallback?.requestTvFocusSafely()
                }
                true
            }
        }
    }
}

/**
 * Horizontal sliders treat D-pad Down as "decrease". On TV, Up/Down must move focus
 * to the next control instead of seeking.
 */
fun Modifier.tvSliderFocusNavigation(
    up: FocusRequester? = null,
    down: FocusRequester? = null,
): Modifier = composed {
    if (!LocalIsTv.current) {
        this
    } else {
        val focusManager = LocalFocusManager.current
        onPreviewKeyEvent { event ->
            val isUp = event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP ||
                event.key == Key.DirectionUp
            val isDown = event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN ||
                event.key == Key.DirectionDown
            if (!isUp && !isDown) {
                false
            } else if (event.type == KeyEventType.KeyDown) {
                if (isDown) {
                    if (down != null) down.requestTvFocusSafely() else focusManager.moveFocus(FocusDirection.Down)
                } else {
                    if (up != null) up.requestTvFocusSafely() else focusManager.moveFocus(FocusDirection.Up)
                }
                true
            } else {
                // Swallow KeyUp so Material/Compose sliders never treat vertical D-pad as a value change.
                event.type == KeyEventType.KeyUp
            }
        }
    }
}

/** Keep D-pad navigation inside this overlay; background UI cannot take focus. */
fun Modifier.tvFocusTrap(): Modifier = composed {
    if (!LocalIsTv.current) {
        this
    } else {
        this
            .focusGroup()
            .focusProperties {
                exit = { FocusRequester.Cancel }
            }
    }
}

@Composable
fun TvFocusDialog(
    onDismiss: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
    dismissOnBackPress: Boolean = true,
    trapFocus: Boolean = true,
    scrim: Color = Color(0x99000000),
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = dismissOnBackPress,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
                .then(if (trapFocus) Modifier.tvFocusTrap() else Modifier),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

@Composable
fun TvRequestFocus(
    requester: FocusRequester,
    key: Any? = Unit,
    enabled: Boolean = true,
) {
    val isTv = LocalIsTv.current
    LaunchedEffect(key, isTv) {
        if (!isTv || !enabled) return@LaunchedEffect
        withFrameNanos { }
        delay(32)
        requester.requestTvFocusSafely()
    }
}

fun View.disableTvFocus() {
    isFocusable = false
    isFocusableInTouchMode = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    if (this is ViewGroup) {
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        for (index in 0 until childCount) {
            getChildAt(index)?.disableTvFocus()
        }
    }
}

@Composable
fun TvBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(GlassSurface)
            .tvClickable(shape = CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
fun TvNavigationRail(
    selectedView: AppView,
    onSelect: (AppView) -> Unit,
    onOpenSearch: () -> Unit,
    contentFocusRequester: FocusRequester,
    onMoveFocusToContent: () -> Unit = { contentFocusRequester.requestTvFocusSafely() },
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    var railHasFocus by remember { mutableStateOf(false) }
    val expanded = railHasFocus

    Column(
        modifier = modifier
            .width(if (expanded) 188.dp else TvNavRailWidth)
            .fillMaxHeight()
            .onFocusChanged { railHasFocus = it.hasFocus }
            .background(Color(0xF210101D))
            .padding(
                horizontal = if (expanded) 10.dp else 8.dp,
                vertical = 28.dp,
            )
            .focusGroup()
            .focusProperties { right = contentFocusRequester }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    contentFocusRequester.requestTvFocusSafely()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppView.entries.forEach { view ->
            TvRailItem(
                icon = {
                    Icon(
                        imageVector = view.icon,
                        contentDescription = view.label,
                        tint = if (view == selectedView) Color.White else MutedText,
                        modifier = Modifier.size(26.dp),
                    )
                },
                label = view.label,
                selected = view == selectedView,
                expanded = expanded,
                focusRequester = if (view == selectedView) focusRequester else null,
                onClick = {
                    onSelect(view)
                    onMoveFocusToContent()
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TvRailItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MutedText,
                    modifier = Modifier.size(26.dp),
                )
            },
            label = "Search",
            selected = false,
            expanded = expanded,
            onClick = onOpenSearch,
        )
    }
}

@Composable
private fun TvRailItem(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .then(if (expanded) Modifier.fillMaxWidth().height(64.dp) else Modifier.size(56.dp))
            .background(
                if (selected) AccentPurple.copy(alpha = 0.42f) else Color.Transparent,
                shape,
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvClickable(shape = shape, onClick = onClick)
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(12.dp) else Arrangement.Center,
    ) {
        icon()
        if (expanded) {
            Text(
                text = label,
                color = if (selected) Color.White else MutedText,
                fontSize = 15.sp,
                maxLines = 1,
            )
        }
    }
}
