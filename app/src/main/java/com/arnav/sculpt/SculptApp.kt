package com.arnav.sculpt

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arnav.sculpt.camera.CameraScreen
import com.arnav.sculpt.gallery.GalleryScreen
import com.arnav.sculpt.home.HomeScreen
import com.arnav.sculpt.settings.SettingsScreen
import com.arnav.sculpt.ui.theme.MatFloor

private val tabLabels = listOf("Home", "Camera", "Gallery", "Settings")
private const val TAB_HOME = 0
private const val TAB_CAMERA = 1
private const val TAB_GALLERY = 2

@Composable
fun SculptApp(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val activity = LocalContext.current as ComponentActivity
    var tabIndex by rememberSaveable { mutableStateOf(TAB_HOME) }
    val onHome = tabIndex == TAB_HOME
    val onCamera = tabIndex == TAB_CAMERA

    // The camera is always black, so it needs light status/navigation bar icons in both themes.
    val lightIcons = dark || onCamera
    DisposableEffect(lightIcons) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { lightIcons },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { lightIcons }
        )
        onDispose { }
    }

    Box(Modifier.fillMaxSize()) {
        if (!onCamera) {
            MatFloor(modifier = Modifier.fillMaxSize(), maroonRows = if (onHome) 2 else 0)
        }
        Scaffold(
            containerColor = if (onCamera) MaterialTheme.colorScheme.background else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                SculptBottomBar(selected = tabIndex, onSelect = { tabIndex = it })
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (tabIndex) {
                    TAB_HOME -> HomeScreen(onOpenCamera = { tabIndex = TAB_CAMERA })
                    TAB_CAMERA -> CameraScreen(onDone = { tabIndex = TAB_HOME })
                    TAB_GALLERY -> GalleryScreen()
                    else -> SettingsScreen(dark = dark, onDarkChange = onDarkChange)
                }
            }
        }
    }
}

@Composable
private fun SculptBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
