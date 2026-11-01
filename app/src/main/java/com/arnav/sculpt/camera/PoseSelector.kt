package com.arnav.sculpt.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arnav.sculpt.data.db.PoseEntity
import com.arnav.sculpt.ui.WheelOption
import com.arnav.sculpt.ui.WheelPill

/** The pose pill on the camera screen: a [WheelPill] whose options are the enabled poses. */
@Composable
fun PoseSelector(
    poses: List<PoseEntity>,
    selectedIndex: Int,
    counts: Map<String, Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(poses, counts) {
        poses.map { pose ->
            val count = counts[pose.id] ?: 0
            WheelOption(
                label = pose.name,
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
