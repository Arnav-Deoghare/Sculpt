package com.arnav.sculpt.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arnav.sculpt.appContainer
import com.arnav.sculpt.data.PoseRepository
import com.arnav.sculpt.data.SettingsStore
import com.arnav.sculpt.data.db.PoseEntity
import kotlinx.coroutines.launch

private const val KEEP_ONE = "Keep at least one pose turned on."
private val RowHeight = 56.dp

@Composable
fun SettingsScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }

    val poses by container.poses.managedPoses.collectAsState(initial = emptyList())
    val enabledCount = poses.count { it.enabled }

    // Local copy of the order so rows can move live while dragging.
    var order by remember { mutableStateOf<List<PoseEntity>>(emptyList()) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(poses, dragId) { if (dragId == null) order = poses }
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }

    var handsFree by remember { mutableStateOf(SettingsStore.handsFree(context)) }
    var notice by remember { mutableStateOf<String?>(null) }

    // Add / rename dialog. editingId == null means "add".
    var dialogOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var draftError by remember { mutableStateOf<String?>(null) }

    fun openDialog(id: String?, startText: String) {
        editingId = id
        draft = startText
        draftError = null
        dialogOpen = true
    }

    fun swap(from: Int, to: Int) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        order = order.toMutableList().also {
            val item = it.removeAt(from)
            it.add(to, item)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            // ---- Appearance ----
            SectionLabel("Appearance")
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Segment("Dark", dark, Modifier.weight(1f)) { onDarkChange(true) }
                    Segment("Light", !dark, Modifier.weight(1f)) { onDarkChange(false) }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- Poses ----
            SectionLabel("Poses")
            Text(
                text = "Switch poses on or off. Drag ≡ to set the shooting order.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SectionCard {
                Column {
                    order.forEachIndexed { index, pose ->
                        if (index > 0) Hairline()
                        PoseRow(
                            pose = pose,
                            index = index,
                            rowPx = rowPx,
                            dragging = dragId == pose.id,
                            dragOffset = if (dragId == pose.id) dragOffset else 0f,
                            switchLocked = pose.enabled && enabledCount <= 1,
                            onToggle = { on ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val ok = container.poses.setEnabled(pose.id, on)
                                    notice = if (ok) null else KEEP_ONE
                                }
                            },
                            onRename = { openDialog(pose.id, pose.name) },
                            onRemove = {
                                scope.launch {
                                    val ok = container.poses.removeCustom(pose.id)
                                    if (!ok) {
                                        notice = KEEP_ONE
                                    } else {
                                        notice = null
                                        val result = snackbar.showSnackbar(
                                            message = "Removed \"${pose.name}\"",
                                            actionLabel = "Undo"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            container.poses.restoreCustom(pose.id)
                                        }
                                    }
                                }
                            },
                            onDragStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragId = pose.id
                                dragOffset = 0f
                            },
                            onDrag = { dy ->
                                dragOffset += dy
                                // Cross as many rows as the finger has passed, in one go.
                                var i = order.indexOfFirst { it.id == dragId }
                                if (i >= 0) {
                                    while (dragOffset > rowPx / 2 && i < order.lastIndex) {
                                        swap(i, i + 1)
                                        dragOffset -= rowPx
                                        i++
                                    }
                                    while (dragOffset < -rowPx / 2 && i > 0) {
                                        swap(i, i - 1)
                                        dragOffset += rowPx
                                        i--
                                    }
                                    // Keep the row inside the pose list: it can't leave the card.
                                    dragOffset = dragOffset.coerceIn(
                                        -i * rowPx,
                                        (order.lastIndex - i) * rowPx
                                    )
                                }
                            },
                            onDragEnd = {
                                val ids = order.map { it.id }
                                scope.launch {
                                    container.poses.reorder(ids)
                                    // Settle into the slot instead of snapping.
                                    animate(
                                        initialValue = dragOffset,
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
                                    ) { v, _ -> dragOffset = v }
                                    dragId = null
                                }
                            }
                        )
                    }
                    Hairline()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .clickable { openDialog(null, "") }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(36.dp)
                        )
                        Text(
                            text = "Add custom pose",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Hold a custom pose to rename or remove it. Removing keeps its photos in the gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            notice?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))

            // ---- Advanced ----
            SectionLabel("Advanced")
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hands-free capture", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Tap the shutter once and every remaining pose is shot 3 seconds apart. Tap again to stop.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = handsFree,
                        onCheckedChange = {
                            handsFree = it
                            SettingsStore.setHandsFree(context, it)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(if (editingId == null) "New custom pose" else "Rename pose") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it.take(PoseRepository.MAX_NAME_LENGTH)
                        draftError = null
                    },
                    label = { Text("Pose name") },
                    singleLine = true,
                    isError = draftError != null,
                    supportingText = draftError?.let { msg -> { Text(msg) } }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val id = editingId
                        val ok = if (id == null) {
                            container.poses.addCustom(draft) != null
                        } else {
                            container.poses.rename(id, draft)
                        }
                        if (ok) {
                            dialogOpen = false
                            notice = null
                        } else {
                            draftError = "Enter a name that isn't empty or already used."
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PoseRow(
    pose: PoseEntity,
    index: Int,
    rowPx: Float,
    dragging: Boolean,
    dragOffset: Float,
    switchLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val startNow by rememberUpdatedState(onDragStart)
    val dragNow by rememberUpdatedState(onDrag)
    val endNow by rememberUpdatedState(onDragEnd)

    // Rows nudged aside by a drag slide into their new slot instead of jumping.
    val slide = remember { Animatable(0f) }
    var lastIndex by remember { mutableIntStateOf(index) }
    LaunchedEffect(index) {
        if (index != lastIndex && !dragging) {
            val delta = (lastIndex - index) * rowPx
            lastIndex = index
            slide.snapTo(delta)
            slide.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 600f))
        } else {
            lastIndex = index
        }
    }

    // Picked-up row: grows slightly and gains a shadow.
    val lift by animateFloatAsState(if (dragging) 1f else 0f, label = "lift")
    val liftPx = with(LocalDensity.current) { 14.dp.toPx() }

    val rowModifier = Modifier
        .fillMaxWidth()
        .height(RowHeight)
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            translationY = dragOffset + slide.value
            val sc = 1f + 0.04f * lift
            scaleX = sc
            scaleY = sc
            shadowElevation = liftPx * lift
            shape = RoundedCornerShape(12.dp)
            clip = false
        }
        .background(
            if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh
            else androidx.compose.ui.graphics.Color.Transparent,
            RoundedCornerShape(12.dp)
        )
        .let {
            if (pose.builtIn) it else it.combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuOpen = true
                }
            )
        }
        .padding(horizontal = 16.dp)

    Box {
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "≡",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(36.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { startNow() },
                            onDragEnd = { endNow() },
                            onDragCancel = { endNow() },
                            onDrag = { change, amount ->
                                change.consume()
                                dragNow(amount.y)
                            }
                        )
                    }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(pose.name, style = MaterialTheme.typography.bodyLarge)
                if (!pose.builtIn) {
                    Text(
                        text = "custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = pose.enabled,
                onCheckedChange = onToggle,
                enabled = !switchLocked
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = { menuOpen = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                onClick = { menuOpen = false; onRemove() }
            )
        }
    }
}