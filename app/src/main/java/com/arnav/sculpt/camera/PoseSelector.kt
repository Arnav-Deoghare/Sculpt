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
    doneToday: Set<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(poses, counts, doneToday) {
        poses.map { pose ->
            val count = counts[pose.id] ?: 0
            val done = pose.id in doneToday
            WheelOption(
                label = if (done) "✓ ${pose.name}" else pose.name,
                detail = if (done) "done today" else if (count == 1) "1 photo" else "$count photos"
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
