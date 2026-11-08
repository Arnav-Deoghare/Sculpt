package com.arnav.sculpt.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnav.sculpt.util.rememberDecodedImage
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val MAX_ZOOM = 5f
private const val NO_POSE = ""

internal fun poseName(key: String, names: Map<String, String>): String =
    if (key == NO_POSE) "No pose" else names[key] ?: "Pose"

/**
 * Poses that have at least two photos, newest pose first. Each list is newest photo first, so
 * `last()` is the oldest photo and `first()` the newest.
 */
internal fun comparableGroups(photos: List<GalleryPhoto>): List<Pair<String, List<GalleryPhoto>>> =
    photos.groupBy { it.poseId ?: NO_POSE }
        .filterValues { it.size >= 2 }
        .toList()
        .sortedByDescending { (_, list) -> list.first().takenAt }

/** "3 weeks", "5 months" and so on, for the gap between two photos. */
private fun gapText(a: GalleryPhoto, b: GalleryPhoto): String {
    val days = abs(ChronoUnit.DAYS.between(zoned(a.takenAt).toLocalDate(), zoned(b.takenAt).toLocalDate()))
    fun plural(n: Long, unit: String) = "$n $unit" + if (n == 1L) "" else "s"
    return when {
        days == 0L -> "Same day"
        days < 14 -> plural(days, "day")
        days < 60 -> plural(days / 7, "week")
        days < 730 -> plural(days / 30, "month")
        else -> plural(days / 365, "year")
    }
}

/**
 * Compares two photos, either next to each other or faded over one another. The dates under the
 * photos open a picker to swap either one.
 */
@Composable
internal fun CompareView(
    photos: List<GalleryPhoto>,
    names: Map<String, String>,
    initialBefore: GalleryPhoto,
    initialAfter: GalleryPhoto,
    onClose: () -> Unit
) {
    var poseKey by remember { mutableStateOf(initialBefore.poseId ?: NO_POSE) }
    var before by remember { mutableStateOf(initialBefore) }
    var after by remember { mutableStateOf(initialAfter) }
    val groups = remember(photos) { comparableGroups(photos) }
    val samePose = remember(photos, poseKey) { photos.filter { (it.poseId ?: NO_POSE) == poseKey } }
    var fade by remember { mutableStateOf(false) }
    var mix by remember { mutableFloatStateOf(0.5f) }
    var picking by remember { mutableStateOf<Int?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    BackHandler(enabled = picking == null, onBack = onClose)
    LaunchedEffect(before.file.name, after.file.name) {
        zoom = 1f
        pan = Offset.Zero
    }

    val beforeBitmap = rememberPhoto(before)
    val afterBitmap = rememberPhoto(after)
    val transform = Zoom(zoom, pan) { z, p -> zoom = z; pan = p }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compare",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose) { Text("Done") }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups, key = { it.first }) { (key, list) ->
                    Pill(
                        text = "${poseName(key, names)} · ${list.size}",
                        selected = key == poseKey,
                        onClick = {
                            poseKey = key
                            before = list.last()
                            after = list.first()
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Pill("Side by side", selected = !fade, onClick = { fade = false })
                Pill("Fade", selected = fade, onClick = { fade = true })
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                if (fade) {
                    FadeView(beforeBitmap, afterBitmap, mix, transform)
                } else {
                    SideBySideView(beforeBitmap, afterBitmap, transform)
                }
                if (zoom > 1f) {
                    Text(
                        text = "Reset zoom",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCC000000))
                            .clickable { zoom = 1f; pan = Offset.Zero }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (fade) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Before", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = mix,
                        onValueChange = { mix = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("After", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateCard("Before", before, names, onClick = { picking = 0 }, modifier = Modifier.weight(1f))
                Text(
                    text = gapText(before, after),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DateCard("After", after, names, onClick = { picking = 1 }, modifier = Modifier.weight(1f))
            }
        }

        picking?.let { slot ->
            PhotoPicker(
                title = poseName(poseKey, names),
                photos = samePose,
                names = names,
                current = if (slot == 0) before else after,
                onPick = {
                    if (slot == 0) before = it else after = it
                    picking = null
                },
                onCancel = { picking = null }
            )
        }
    }
}

@Composable
private fun rememberPhoto(photo: GalleryPhoto): ImageBitmap? = rememberDecodedImage(photo.file, 1600)

/** Shared pinch-zoom and pan, applied to both photos so they stay aligned. */
private class Zoom(val scale: Float, val pan: Offset, val set: (Float, Offset) -> Unit)

private fun Modifier.zoomGestures(zoom: Zoom, viewSize: IntSize): Modifier =
    this
        .pointerInput(viewSize) {
            detectTransformGestures { _, panChange, zoomChange, _ ->
                val scale = (zoom.scale * zoomChange).coerceIn(1f, MAX_ZOOM)
                val maxX = viewSize.width * (scale - 1f) / 2f
                val maxY = viewSize.height * (scale - 1f) / 2f
                val next = if (scale <= 1f) Offset.Zero else Offset(
                    (zoom.pan.x + panChange.x).coerceIn(-maxX, maxX),
                    (zoom.pan.y + panChange.y).coerceIn(-maxY, maxY)
                )
                zoom.set(scale, next)
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { zoom.set(1f, Offset.Zero) })
        }

private fun Modifier.zoomed(zoom: Zoom): Modifier = graphicsLayer {
    scaleX = zoom.scale
    scaleY = zoom.scale
    translationX = zoom.pan.x
    translationY = zoom.pan.y
}

@Composable
private fun SideBySideView(before: ImageBitmap?, after: ImageBitmap?, zoom: Zoom) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .zoomGestures(zoom, IntSize(size.width / 2, size.height)),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(before, after).forEachIndexed { slot, bitmap ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .clipToBounds()
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = if (slot == 0) "Before photo" else "After photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().zoomed(zoom)
                    )
                }
                CornerTag(if (slot == 0) "Before" else "After", Modifier.align(Alignment.TopStart).padding(8.dp))
            }
        }
    }
}

/** Both photos stacked in one frame; [mix] 0 shows only Before, 1 shows only After. */
@Composable
private fun FadeView(before: ImageBitmap?, after: ImageBitmap?, mix: Float, zoom: Zoom) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
            .zoomGestures(zoom, size)
            .clipToBounds()
    ) {
        if (before != null) {
            Image(
                bitmap = before,
                contentDescription = "Before photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().zoomed(zoom)
            )
        }
        if (after != null) {
            Image(
                bitmap = after,
                contentDescription = "After photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().alpha(mix).zoomed(zoom)
            )
        }
    }
}

@Composable
private fun CornerTag(text: String, modifier: Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x99000000))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DateCard(
    label: String,
    photo: GalleryPhoto,
    names: Map<String, String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val pose = poseLabel(photo, names)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = zoned(photo.takenAt).format(dateFormat),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (pose != null) {
            Text(
                text = pose,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A full-screen grid of every photo; tapping one picks it. */
@Composable
private fun PhotoPicker(
    title: String,
    photos: List<GalleryPhoto>,
    names: Map<String, String>,
    current: GalleryPhoto,
    onPick: (GalleryPhoto) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    val groups = remember(photos) { photos.groupBy { zoned(it.takenAt).toLocalDate() }.toList() }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            groups.forEach { (date, dayPhotos) ->
                item(key = "header-$date", span = { GridItemSpan(maxLineSpan) }) {
                    DayHeader(date, dayPhotos.size)
                }
                gridItems(dayPhotos, key = { it.file.name }) { photo ->
                    GalleryThumb(
                        photo = photo,
                        poseLabel = poseLabel(photo, names),
                        selected = photo.file.name == current.file.name,
                        onClick = { onPick(photo) },
                        onLongClick = { onPick(photo) }
                    )
                }
            }
        }
    }
}
