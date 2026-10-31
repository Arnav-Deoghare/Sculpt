package com.arnav.sculpt.gallery

import com.arnav.sculpt.data.PhotoItem
import com.arnav.sculpt.data.PhotoStore
import com.arnav.sculpt.data.Pose
import com.arnav.sculpt.data.SettingsStore
import com.arnav.sculpt.util.ImageUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

private fun zoned(millis: Long): ZonedDateTime =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())

private fun photoWord(count: Int) = if (count == 1) "photo" else "photos"

private fun poseLabel(photo: PhotoItem, customName: String): String? =
    Pose.fromId(photo.poseId)?.label(customName)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    val customName = remember { SettingsStore.customPoseName(context) }
    var photos by remember { mutableStateOf(PhotoStore.all(context)) }
    var viewing by remember { mutableStateOf<PhotoItem?>(null) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
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
                                poseLabel = poseLabel(photo, customName),
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

        viewing?.let { photo ->
            PhotoViewer(
                photo = photo,
                poseLabel = poseLabel(photo, customName),
                onClose = { viewing = null },
                onDelete = {
                    PhotoStore.delete(listOf(photo.file))
                    photos = PhotoStore.all(context)
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
                    PhotoStore.delete(photos.filter { it.file.name in selection }.map { it.file })
                    photos = PhotoStore.all(context)
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
private fun DayHeader(date: LocalDate, count: Int) {
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
private fun GalleryThumb(
    photo: PhotoItem,
    poseLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo.file) {
        value = withContext(Dispatchers.IO) {
            ImageUtils.decode(photo.file, 320)?.asImageBitmap()
        }
    }

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
    photo: PhotoItem,
    poseLabel: String?,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    BackHandler(onBack = onClose)
    var confirm by remember { mutableStateOf(false) }

    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo.file) {
        value = withContext(Dispatchers.IO) {
            ImageUtils.decode(photo.file, 2048)?.asImageBitmap()
        }
    }
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
