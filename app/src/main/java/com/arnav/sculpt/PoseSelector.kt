package com.arnav.sculpt

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

private val ItemHeight = 48.dp
private val ListPadding = 6.dp
private val ListGap = 8.dp

/**
 * The pose pill on the camera screen.
 *
 *  - Tap: jump to the next pose.
 *  - Long-press, then drag up: a list of poses opens above the pill, the pose under your
 *    finger is magnified, and releasing picks it. Dragging back down onto the pill keeps the
 *    pose you started with.
 */
@Composable
fun PoseSelector(
    poses: List<Pose>,
    customName: String,
    selectedIndex: Int,
    counts: Map<String, Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var open by remember { mutableStateOf(false) }
    var hoverIndex by remember { mutableIntStateOf(selectedIndex) }
    var pillHeightPx by remember { mutableIntStateOf(0) }

    val itemHeightPx = with(density) { ItemHeight.toPx() }
    val listPaddingPx = with(density) { ListPadding.toPx() }
    val gapPx = with(density) { ListGap.toPx() }

    // The list opens ABOVE the pill. In the pill's own coordinates (top of pill = 0) the list
    // spans from listTopPx (negative) up to -gapPx.
    val listHeightPx = poses.size * itemHeightPx + 2 * listPaddingPx
    val listTopPx = -gapPx - listHeightPx

    // The gesture block below is created once, so it reads current values through these.
    val selectedNow by rememberUpdatedState(selectedIndex)
    val countNow by rememberUpdatedState(poses.size)
    val onSelectNow by rememberUpdatedState(onSelect)
    val listTopNow by rememberUpdatedState(listTopPx)
    val itemHeightNow by rememberUpdatedState(itemHeightPx)
    val listPadNow by rememberUpdatedState(listPaddingPx)

    val selectedLabel = poses.getOrNull(selectedIndex)?.label(customName) ?: ""

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onSizeChanged { pillHeightPx = it.height }
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
                                hoverIndex = startIndex
                                open = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    val y = change.position.y
                                    val listY = y - listTopNow - listPadNow
                                    // Finger back down on the pill (below the list): keep the start pose.
                                    val index = if (listY >= countNow * itemHeightNow) {
                                        startIndex
                                    } else {
                                        floor(listY / itemHeightNow).toInt().coerceIn(0, countNow - 1)
                                    }
                                    if (index != hoverIndex) {
                                        hoverIndex = index
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    if (!change.pressed) {
                                        if (hoverIndex != startIndex) onSelectNow(hoverIndex)
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
                AnimatedContent(targetState = selectedLabel, label = "poseLabel") { label ->
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Text(
                    text = "  ↕",
                    color = Color(0xFFBBBBBB),
                    fontSize = 13.sp
                )
            }
        }

        if (open) {
            Popup(
                alignment = Alignment.BottomCenter,
                // Lift the popup's bottom edge from the pill's bottom to just above the pill.
                offset = IntOffset(0, -(pillHeightPx + gapPx).roundToInt()),
                properties = PopupProperties(focusable = false)
            ) {
                Column(
                    modifier = Modifier
                        .width(190.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xE6121212))
                        .padding(vertical = ListPadding)
                ) {
                    poses.forEachIndexed { i, pose ->
                        PoseRow(
                            label = pose.label(customName),
                            count = counts[pose.id] ?: 0,
                            hovered = i == hoverIndex,
                            current = i == selectedIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PoseRow(label: String, count: Int, hovered: Boolean, current: Boolean) {
    val scale by animateFloatAsState(if (hovered) 1.08f else 0.96f, label = "rowScale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ItemHeight)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(if (hovered) Color.White else Color.Transparent),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (current) "●  $label" else label,
                color = if (hovered) Color.Black else Color.White,
                fontWeight = if (hovered) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = count.toString(),
                color = if (hovered) Color(0xFF555555) else Color(0xFF999999),
                fontSize = 12.sp
            )
        }
    }
}
