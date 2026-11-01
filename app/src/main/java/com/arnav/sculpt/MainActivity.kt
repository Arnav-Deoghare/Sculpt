package com.arnav.sculpt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arnav.sculpt.data.SettingsStore
import com.arnav.sculpt.ui.theme.SculptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = applicationContext.appContainer
        setContent {
            var dark by remember { mutableStateOf(SettingsStore.isDark(this@MainActivity)) }
            val ready by container.ready.collectAsState()
            SculptTheme(darkTheme = dark) {
                if (ready) {
                    SculptApp(
                        dark = dark,
                        onDarkChange = {
                            dark = it
                            SettingsStore.setDark(this@MainActivity, it)
                        }
                    )
                } else {
                    // Brief blank screen while the database starts up.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}
