package com.arnav.sculpt.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arnav.sculpt.appContainer
import com.arnav.sculpt.data.PoseRepository
import com.arnav.sculpt.data.db.PoseEntity
import kotlinx.coroutines.launch

private const val KEEP_ONE = "Keep at least one pose turned on."

@Composable
fun SettingsScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()

    val poses by container.poses.managedPoses.collectAsState(initial = emptyList())
    val enabled = poses.filter { it.enabled }
    val customs = poses.filter { !it.builtIn }

    var menuOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    // Add / rename dialog. editingId == null means "add".
    var dialogOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var draftError by remember { mutableStateOf<String?>(null) }

    var removing by remember { mutableStateOf<PoseEntity?>(null) }

    fun openDialog(id: String?, startText: String) {
        editingId = id
        draft = startText
        draftError = null
        dialogOpen = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        // ---- Appearance ----
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = dark,
                onClick = { onDarkChange(true) },
                label = { Text("Dark") }
            )
            FilterChip(
                selected = !dark,
                onClick = { onDarkChange(false) },
                label = { Text("Light") }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ---- Poses ----
        Text("Poses", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Choose the poses you shoot. They appear in the camera in this order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedButton(onClick = { menuOpen = true }) {
                Text("${enabled.size} selected  ▾")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                poses.forEach { pose ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = pose.enabled, onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(pose.name)
                            }
                        },
                        onClick = {
                            scope.launch {
                                val ok = container.poses.setEnabled(pose.id, !pose.enabled)
                                notice = if (ok) null else KEEP_ONE
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = enabled.joinToString("  →  ") { it.name },
            style = MaterialTheme.typography.bodyMedium
        )
        notice?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(28.dp))

        // ---- Custom poses ----
        Text("Custom poses", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Add as many as you like. Removing one keeps its photos in the gallery.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        if (customs.isEmpty()) {
            Text(
                text = "No custom poses yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        customs.forEach { pose ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pose.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { openDialog(pose.id, pose.name) }) { Text("Rename") }
                TextButton(onClick = { removing = pose }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { openDialog(null, "") }) {
            Text("+ Add custom pose")
        }
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

    removing?.let { pose ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove \"${pose.name}\"?") },
            text = { Text("It disappears from the camera. Photos already taken in it stay in the gallery.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = container.poses.removeCustom(pose.id)
                        notice = if (ok) null else KEEP_ONE
                        removing = null
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancel") }
            }
        )
    }
}
