package com.arnav.sculpt.gallery

import com.arnav.sculpt.appContainer
import com.arnav.sculpt.data.db.PhotoEntity
import com.arnav.sculpt.util.rememberDecodedImage
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

internal val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
internal val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
internal val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

internal fun zoned(millis: Long): ZonedDateTime =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())

private fun photoWord(count: Int) = if (count == 1) "photo" else "photos"

/** A photo row paired with its image file. */
internal data class GalleryPhoto(val entity: PhotoEntity, val file: File) {
    val takenAt: Long get() = entity.takenAt
    val poseId: String? get() = entity.poseId
}

/** Name of the photo's pose (including removed custom poses), or null if it has none. */
internal fun poseLabel(photo: GalleryPhoto, names: Map<String, String>): String? =
    photo.poseId?.let { names[it] }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen() {
    val container = LocalContext.current.appContainer
    val entities by container.photos.photos.collectAsState(initial = emptyList())
    val names by container.poses.namesById.collectAsState(initial = emptyMap())
    val photos = remember(entities) {
        entities.map { GalleryPhoto(it, container.photos.file(it)) }
    }
    var viewing by remember { mutableStateOf<GalleryPhoto?>(null) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var comparing by remember { mutableStateOf<Pair<GalleryPhoto, GalleryPhoto>?>(null) }
    val selectionMode = selection.isNotEmpty()

    // Photos are sorted newest first, so groupBy keeps days newest first too.
    val groups = remember(photos) {
        photos.groupBy { zoned(it.takenAt).toLocalDate() }.toList()
    }

    BackHandler(enabled = selectionMode) { selection = emptySet() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selection.size} selected",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { selection = emptySet() }) { Text("Cancel") }
                    val picked = photos.filter { it.file.name in selection }
                    if (picked.size == 2 && picked[0].poseId == picked[1].poseId) {
                        TextButton(onClick = {
                            val (older, newer) = photos
                                .filter { it.file.name in selection }
                                .sortedBy { it.takenAt }
                            selection = emptySet()
                            comparing = older to newer
                        }) { Text("Compare") }
                    }
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )
                Text(
                    text = "${photos.size} ${photoWord(photos.size)}  ·  long-press to select",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                val comparable = comparableGroups(photos)
                if (comparable.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Compare",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    val newestPose = comparable.first().second
                                    comparing = newestPose.last() to newestPose.first()
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No photos yet. Take one from the Camera tab.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
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
                        items(dayPhotos, key = { it.file.name }) { photo ->
                            GalleryThumb(
                                photo = photo,
                                poseLabel = poseLabel(photo, names),
                                selected = photo.file.name in selection,
                                onClick = {
                                    if (selectionMode) {
                                        val name = photo.file.name
                                        selection = if (name in selection) selection - name else selection + name
                                    } else {
                                        viewing = photo
                                    }
                                },
                                onLongClick = {
                                    selection = selection + photo.file.name
                                }
                            )
                        }
                    }
                }
            }
        }

        comparing?.let { (before, after) ->
            CompareView(
                photos = photos,
                names = names,
                initialBefore = before,
                initialAfter = after,
                onClose = { comparing = null }
            )
        }

        viewing?.let { photo ->
            PhotoViewer(
                photo = photo,
                poseLabel = poseLabel(photo, names),
                onClose = { viewing = null },
                onDelete = {
                    container.appScope.launch { container.photos.delete(listOf(photo.entity)) }
                    viewing = null
                }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selection.size} ${photoWord(selection.size)}?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val doomed = photos.filter { it.file.name in selection }.map { it.entity }
                    container.appScope.launch { container.photos.delete(doomed) }
                    selection = emptySet()
                    confirmDelete = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
internal fun DayHeader(date: LocalDate, count: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) {
        Text(
            text = date.format(dayFormat),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${date.format(dateFormat)} · $count ${photoWord(count)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GalleryThumb(
    photo: GalleryPhoto,
    poseLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bitmap = rememberDecodedImage(photo.file, 320)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.DarkGray)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (poseLabel != null) {
            Text(
                text = poseLabel,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color(0x99000000))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        Text(
            text = zoned(photo.takenAt).format(timeFormat),
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color(0x99000000))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        if (selected) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PhotoViewer(
    photo: GalleryPhoto,
    poseLabel: String?,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    BackHandler(onBack = onClose)
    var confirm by remember { mutableStateOf(false) }

    val bitmap = rememberDecodedImage(photo.file, 2048)
    val time = zoned(photo.takenAt)
    val details = buildString {
        append(time.format(dateFormat))
        append(" · ")
        append(time.format(timeFormat))
        if (poseLabel != null) {
            append(" · ")
            append(poseLabel)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = time.format(dayFormat),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFDDDDDD)
                )
            }
            TextButton(onClick = { confirm = true }) {
                Text("Delete", color = Color(0xFFFF8A80))
            }
            TextButton(onClick = onClose) {
                Text("Close", color = Color.White)
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete this photo?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel") }
            }
        )
    }
}
