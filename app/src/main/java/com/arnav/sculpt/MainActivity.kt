package com.arnav.sculpt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arnav.sculpt.ui.theme.SculptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by remember { mutableStateOf(SettingsStore.isDark(this@MainActivity)) }
            SculptTheme(darkTheme = dark) {
                SculptApp(
                    dark = dark,
                    onDarkChange = {
                        dark = it
                        SettingsStore.setDark(this@MainActivity, it)
                    }
                )
            }
        }
    }
}
