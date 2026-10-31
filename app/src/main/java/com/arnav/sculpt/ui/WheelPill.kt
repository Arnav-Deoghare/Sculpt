package com.arnav.sculpt.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

/** One choice in a [WheelPill]. [detail] is an optional smaller second line. */
data class WheelOption(val label: String, val detail: String? = null)

private val ChipHeight = 54.dp
private val ChipSpacing = 8.dp
private val StripGap = 10.dp

/**
 * A pill with a long-press "wheel" of options.
 *
 *  - Tap: jump to the next option (wraps around).
 *  - Long-press, then drag left or right: a strip of chips opens [openAbove] or below the pill
 *    and slides under your finger like a wheel. The chip that settles over the pill is picked
 *    when you let go. Dragging back to where you started keeps the old option.
 */
@Composable
fun WheelPill(
    options: List<WheelOption>,
    selectedIndex: Int,
    pillText: String,
    openAbove: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chipWidth: Dp = 88.dp
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    var open by remember { mutableStateOf(false) }
    // Fractional index of the chip sitting over the pill. Follows the finger smoothly.
    var hover by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var pillCenterX by remember { mutableFloatStateOf(0f) }

    val chipPitchPx = with(density) { (chipWidth + ChipSpacing).toPx() }
    val chipWidthPx = with(density) { chipWidth.toPx() }
    val gapPx = with(density) { StripGap.toPx() }

    // Finger travel per option. Small enough that every option is reachable from any starting
    // option without leaving the screen.
    val stepPx = with(density) {
        val room = screenWidthDp.dp.toPx() / 2f - 8.dp.toPx()
        minOf(44.dp.toPx(), room / maxOf(1, options.size - 1))
    }

    // The gesture block below is created once, so it reads current values through these.
    val selectedNow by rememberUpdatedState(selectedIndex)
    val countNow by rememberUpdatedState(options.size)
    val onSelectNow by rememberUpdatedState(onSelect)
    val stepNow by rememberUpdatedState(stepPx)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    pillCenterX = it.positionInWindow().x + it.size.width / 2f
                }
                .clip(CircleShape)
                .background(Color(0x99000000))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()

                        // true = released quickly (tap), false = cancelled, null = held long enough.
                        val quickUp: Boolean? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation() != null
                        }

                        when (quickUp) {
                            true -> {
                                if (countNow > 1) onSelectNow((selectedNow + 1) % countNow)
                            }
                            false -> Unit
                            null -> {
                                val startIndex = selectedNow
                                val startX = down.position.x
                                var snapped = startIndex
                                hover = startIndex.toFloat()
                                open = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    val h = (startIndex + (change.position.x - startX) / stepNow)
                                        .coerceIn(0f, (countNow - 1).toFloat())
                                    hover = h

                                    val idx = h.roundToInt()
                                    if (idx != snapped) {
                                        snapped = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    if (!change.pressed) {
                                        if (snapped != startIndex) onSelectNow(snapped)
                                        break
                                    }
                                    change.consume()
                                }
                                open = false
                            }
                        }
                    }
                }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(targetState = pillText, label = "pillText") { text ->
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Text(
                    text = "  ↔",
                    color = Color(0xFFBBBBBB),
                    fontSize = 13.sp
                )
            }
        }

        if (open) {
            Popup(
                popupPositionProvider = remember(gapPx, openAbove) {
                    StripPositionProvider(gapPx.roundToInt(), openAbove)
                },
                properties = PopupProperties(
                    focusable = false,
                    usePlatformDefaultWidth = false,
                    clippingEnabled = false
                )
            ) {
                // Full-width, transparent strip; chips are placed relative to the pill's centre.
                Box(
                    modifier = Modifier
                        .width(screenWidthDp.dp)
                        .height(ChipHeight)
                ) {
                    options.forEachIndexed { i, option ->
                        WheelChip(
                            option = option,
                            width = chipWidth,
                            distance = (i - hover).absoluteValue,
                            current = i == selectedIndex,
                            modifier = Modifier.offset {
                                IntOffset(
                                    (pillCenterX + (i - hover) * chipPitchPx - chipWidthPx / 2f).roundToInt(),
                                    0
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Places the strip at the left edge of the window, just above or just below the pill. */
private class StripPositionProvider(
    private val gapPx: Int,
    private val above: Boolean
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val y = if (above) {
            anchorBounds.top - popupContentSize.height - gapPx
        } else {
            anchorBounds.bottom + gapPx
        }
        return IntOffset(0, y)
    }
}

@Composable
private fun WheelChip(
    option: WheelOption,
    width: Dp,
    distance: Float,
    current: Boolean,
    modifier: Modifier = Modifier
) {
    val near = distance.coerceIn(0f, 1f)
    val hovered = distance < 0.5f

    Column(
        modifier = modifier
            .width(width)
            .height(ChipHeight)
            .graphicsLayer {
                val s = lerp(1.12f, 0.86f, near)
                scaleX = s
                scaleY = s
                alpha = lerp(1f, 0.5f, near)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(if (hovered) Color.White else Color(0xE6121212)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (current) "● ${option.label}" else option.label,
            color = if (hovered) Color.Black else Color.White,
            fontWeight = if (hovered) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1
        )
        if (option.detail != null) {
            Text(
                text = option.detail,
                color = if (hovered) Color(0xFF555555) else Color(0xFF999999),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
