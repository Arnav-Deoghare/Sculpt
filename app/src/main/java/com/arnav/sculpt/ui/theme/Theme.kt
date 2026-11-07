package com.arnav.sculpt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors for the rubber-mat floor drawn by [MatFloor]. */
data class MatColors(
    val tileA: Color,
    val tileB: Color,
    val maroonA: Color,
    val maroonB: Color,
    val seam: Color,
    val grainLight: Color,
    val grainDark: Color
)

private val DarkMat = MatColors(
    tileA = Color(0xFF0F0E0E),
    tileB = Color(0xFF1A1717),
    maroonA = Color(0xFF5C1121),
    maroonB = Color(0xFF4A0D1A),
    seam = Color(0xFF050404),
    grainLight = Color(0x0FFFFFFF),
    grainDark = Color(0x40000000)
)

private val LightMat = MatColors(
    tileA = Color(0xFFF1E8E3),
    tileB = Color(0xFFE8DCD6),
    maroonA = Color(0xFF8E2A3E),
    maroonB = Color(0xFF7A1A2E),
    seam = Color(0xFFCDBBB5),
    grainLight = Color(0x33FFFFFF),
    grainDark = Color(0x14000000)
)

val LocalMatColors = staticCompositionLocalOf { DarkMat }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC23A52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C1121),
    onPrimaryContainer = Color(0xFFEDE3DC),
    secondary = Color(0xFFB08A90),
    onSecondary = Color(0xFF1A1717),
    background = Color(0xFF0F0E0E),
    onBackground = Color(0xFFEDE3DC),
    surface = Color(0xFF0F0E0E),
    onSurface = Color(0xFFEDE3DC),
    surfaceVariant = Color(0xFF2A2323),
    onSurfaceVariant = Color(0xFFA08E89),
    surfaceContainer = Color(0xFF161313),
    surfaceContainerHigh = Color(0xFF211C1C),
    outline = Color(0xFF6B5A58),
    outlineVariant = Color(0xFF3A2F2F)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF7A1A2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2CFD5),
    onPrimaryContainer = Color(0xFF3F0B16),
    secondary = Color(0xFF8A5560),
    background = Color(0xFFF6F0EC),
    onBackground = Color(0xFF1C1414),
    surface = Color(0xFFF6F0EC),
    onSurface = Color(0xFF1C1414),
    surfaceVariant = Color(0xFFE8DCD7),
    onSurfaceVariant = Color(0xFF5E4C49),
    surfaceContainer = Color(0xFFEFE4DF),
    surfaceContainerHigh = Color(0xFFE6D9D3),
    outline = Color(0xFF8A7571),
    outlineVariant = Color(0xFFD2C2BD)
)

@Composable
fun SculptTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMatColors provides if (darkTheme) DarkMat else LightMat) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content
        )
    }
}
