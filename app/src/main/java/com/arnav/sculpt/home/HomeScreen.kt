package com.arnav.sculpt.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnav.sculpt.appContainer
import com.arnav.sculpt.data.db.PhotoEntity
import com.arnav.sculpt.data.db.PoseEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class TodayState { NOT_STARTED, IN_PROGRESS, COMPLETE }

/** Where today's set stands, judged from today's photos against the poses currently switched on. */
private fun todayState(photos: List<PhotoEntity>, poses: List<PoseEntity>): TodayState {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val todays = photos.filter {
        Instant.ofEpochMilli(it.takenAt).atZone(zone).toLocalDate() == today
    }
    val donePoseIds = todays.mapNotNull { it.poseId }.toSet()
    return when {
        todays.isEmpty() -> TodayState.NOT_STARTED
        poses.isNotEmpty() && poses.all { it.id in donePoseIds } -> TodayState.COMPLETE
        else -> TodayState.IN_PROGRESS
    }
}

/**
 * The home screen: one or two text buttons over the mat floor drawn behind the app.
 * Until the guided capture flow exists, every button just opens the camera.
 */
@Composable
fun HomeScreen(onOpenCamera: () -> Unit) {
    val container = LocalContext.current.appContainer
    val photos by container.photos.photos.collectAsState(initial = emptyList())
    val poses by container.poses.enabledPoses.collectAsState(initial = emptyList())
    val state = remember(photos, poses) { todayState(photos, poses) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (state) {
                TodayState.NOT_STARTED ->
                    FancyTextButton("Capture today", primary = true, onClick = onOpenCamera)

                TodayState.IN_PROGRESS ->
                    FancyTextButton("Continue today", primary = true, onClick = onOpenCamera)

                TodayState.COMPLETE -> {
                    FancyTextButton("Recapture", primary = true, onClick = onOpenCamera)
                    FancyTextButton("Capture more", primary = false, onClick = onOpenCamera)
                }
            }
        }
    }
}

/**
 * A text-only button: wide-spaced capitals with a light band of brightness sweeping across them,
 * a small pulsing line underneath, and a gentle shrink while pressed. No box, no ripple.
 */
@Composable
private fun FancyTextButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "pressScale")

    val transition = rememberInfiniteTransition(label = "fancy")
    val sweep by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "sweep"
    )
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val colors = MaterialTheme.colorScheme
    val base = if (primary) colors.onSurfaceVariant else colors.outline
    val highlight = if (primary) colors.onBackground else colors.onSurfaceVariant
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(sweep, 0f),
        end = Offset(sweep + 320f, 0f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 14.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                brush = brush,
                fontSize = if (primary) 28.sp else 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = if (primary) 6.sp else 5.sp
            )
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(if (primary) 48.dp else 32.dp)
                .height(1.dp)
                .background(highlight.copy(alpha = glow))
        )
    }
}
