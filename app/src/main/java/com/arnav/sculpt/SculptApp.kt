package com.arnav.sculpt

import com.arnav.sculpt.camera.CameraScreen
import com.arnav.sculpt.gallery.GalleryScreen
import com.arnav.sculpt.settings.SettingsScreen
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val tabLabels = listOf("Camera", "Gallery", "Settings")

@Composable
fun SculptApp(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val activity = LocalContext.current as ComponentActivity
    var tabIndex by rememberSaveable { mutableStateOf(0) }

    // Keep status/navigation bar icon colours readable in both themes.
    DisposableEffect(dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { dark },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { dark }
        )
        onDispose { }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { SculptBottomBar(selected = tabIndex, onSelect = { tabIndex = it }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tabIndex) {
                0 -> CameraScreen()
                1 -> GalleryScreen()
                else -> SettingsScreen(dark = dark, onDarkChange = onDarkChange)
            }
        }
    }
}

@Composable
private fun SculptBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
        ) {
            tabLabels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
