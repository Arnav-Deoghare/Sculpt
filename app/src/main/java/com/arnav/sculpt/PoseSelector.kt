package com.arnav.sculpt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The pose pill on the camera screen: a [WheelPill] whose options are the enabled poses. */
@Composable
fun PoseSelector(
    poses: List<Pose>,
    customName: String,
    selectedIndex: Int,
    counts: Map<String, Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(poses, customName, counts) {
        poses.map { pose ->
            val count = counts[pose.id] ?: 0
            WheelOption(
                label = pose.label(customName),
                detail = if (count == 1) "1 photo" else "$count photos"
            )
        }
    }

    WheelPill(
        options = options,
        selectedIndex = selectedIndex,
        pillText = options.getOrNull(selectedIndex)?.label ?: "",
        openAbove = true,
        onSelect = onSelect,
        modifier = modifier,
        chipWidth = 88.dp
    )
}
