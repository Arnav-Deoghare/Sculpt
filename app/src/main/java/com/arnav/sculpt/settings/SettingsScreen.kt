package com.arnav.sculpt.settings

import com.arnav.sculpt.data.Pose
import com.arnav.sculpt.data.SettingsStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(SettingsStore.enabledPoses(context)) }
    var customName by remember { mutableStateOf(SettingsStore.customPoseName(context)) }
    var menuOpen by remember { mutableStateOf(false) }

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
            text = "Choose the poses you shoot. They appear in the camera slider in this order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedButton(onClick = { menuOpen = true }) {
                Text("${enabled.size} selected  ▾")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Pose.entries.forEach { pose ->
                    val checked = pose in enabled
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(pose.label(customName))
                            }
                        },
                        onClick = {
                            // Keep at least one pose enabled.
                            val updated = if (checked) {
                                if (enabled.size > 1) enabled - pose else enabled
                            } else {
                                Pose.entries.filter { it in enabled || it == pose }
                            }
                            enabled = updated
                            SettingsStore.setEnabledPoses(context, updated)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = enabled.joinToString("  →  ") { it.label(customName) },
            style = MaterialTheme.typography.bodyMedium
        )

        if (Pose.CUSTOM in enabled) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = customName,
                onValueChange = { value ->
                    val name = value.take(20)
                    customName = name
                    SettingsStore.setCustomPoseName(context, name)
                },
                label = { Text("Custom pose name") },
                singleLine = true
            )
        }
    }
}
