package com.arnav.sculpt.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

/** What the camera draws over the preview to help you line up: nothing, framing lines, or a body outline. */
enum class GuideMode {
    OFF, FRAME, BODY;

    val label: String
        get() = when (this) {
            OFF -> "Off"
            FRAME -> "Frame"
            BODY -> "Body"
        }

    fun next(): GuideMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromOrdinal(ordinal: Int): GuideMode = entries.getOrElse(ordinal) { BODY }
    }
}

// The guide fits between these fractions of the screen height, and is narrowed if the screen is too
// narrow for the widest pose (arms flexed out to the sides take about 0.7 of the body height).
private const val AREA_TOP = 0.10f
private const val AREA_BOTTOM = 0.92f
private const val WIDEST_POSE = 0.72f

/** Where the guide's head top starts and how tall the guide is, in pixels. Both modes share it. */
private fun DrawScope.guideBounds(): Pair<Float, Float> {
    val area = size.height * (AREA_BOTTOM - AREA_TOP)
    val height = minOf(area, size.width * 0.95f / WIDEST_POSE)
    val top = size.height * AREA_TOP + (area - height) / 2f
    return top to height
}

private val FrameColor = Color(0x99FFFFFF)

/** Draws the chosen guide for [poseId]. It only appears on screen; it is never part of a saved photo. */
@Composable
fun PoseGuide(mode: GuideMode, poseId: String, modifier: Modifier = Modifier) {
    if (mode == GuideMode.OFF) return
    // The body is drawn solid white into its own layer, then the whole layer is faded once, so
    // overlapping limbs do not show up darker than the rest.
    val layer = if (mode == GuideMode.BODY) {
        Modifier.graphicsLayer(alpha = 0.22f, compositingStrategy = CompositingStrategy.Offscreen)
    } else {
        Modifier
    }
    Canvas(modifier = modifier.fillMaxSize().then(layer)) {
        when (mode) {
            GuideMode.FRAME -> drawFrame()
            GuideMode.BODY -> drawBody(poseId)
            GuideMode.OFF -> Unit
        }
    }
}

private fun DrawScope.drawFrame() {
    val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
    val stroke = Stroke(width = 2f, pathEffect = dash)
    val cx = size.width / 2f
    val (top, height) = guideBounds()
    val head = top
    val feet = top + height

    drawLine(FrameColor, Offset(cx, head), Offset(cx, feet), strokeWidth = 2f, pathEffect = dash)
    for (y in listOf(head, feet)) {
        drawLine(
            color = FrameColor,
            start = Offset(size.width * 0.2f, y),
            end = Offset(size.width * 0.8f, y),
            strokeWidth = stroke.width,
            pathEffect = dash
        )
    }
}

private fun DrawScope.drawBody(poseId: String) {
    val (top, height) = guideBounds()
    val cx = size.width / 2f

    // Positions are in units of body height, measured from the centre line and from the head top.
    fun x(v: Float) = cx + v * height
    fun y(v: Float) = top + v * height
    fun pt(px: Float, py: Float) = Offset(x(px), y(py))

    fun limb(width: Float, from: Offset, via: Offset, to: Offset? = null) {
        val path = Path().apply {
            moveTo(from.x, from.y)
            lineTo(via.x, via.y)
            if (to != null) lineTo(to.x, to.y)
        }
        drawPath(
            path,
            Color.White,
            style = Stroke(width = width * height, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    drawCircle(Color.White, radius = 0.062f * height, center = pt(0f, 0.065f))
    limb(0.05f, pt(0f, 0.11f), pt(0f, 0.17f))

    if (poseId == "side") {
        val torso = Path().apply {
            moveTo(x(-0.07f), y(0.17f))
            lineTo(x(0.075f), y(0.17f))
            lineTo(x(0.07f), y(0.50f))
            lineTo(x(-0.065f), y(0.50f))
            close()
        }
        drawPath(torso, Color.White, style = Fill)
        limb(0.085f, pt(0f, 0.50f), pt(0f, 0.96f))
        limb(0.035f, pt(0.0f, 0.985f), pt(0.11f, 0.985f))
        limb(0.055f, pt(0f, 0.19f), pt(0.01f, 0.35f), pt(0.03f, 0.50f))
        return
    }

    val torso = Path().apply {
        moveTo(x(-0.14f), y(0.17f))
        lineTo(x(0.14f), y(0.17f))
        lineTo(x(0.105f), y(0.34f))
        lineTo(x(0.115f), y(0.50f))
        lineTo(x(-0.115f), y(0.50f))
        lineTo(x(-0.105f), y(0.34f))
        close()
    }
    drawPath(torso, Color.White, style = Fill)

    for (side in listOf(-1f, 1f)) {
        limb(0.10f, pt(side * 0.06f, 0.50f), pt(side * 0.075f, 0.96f))
        limb(0.035f, pt(side * 0.075f, 0.985f), pt(side * 0.135f, 0.985f))
        when (poseId) {
            "front_bicep" ->
                limb(0.06f, pt(side * 0.145f, 0.19f), pt(side * 0.30f, 0.21f), pt(side * 0.27f, 0.07f))
            "back_flexed" ->
                limb(0.06f, pt(side * 0.145f, 0.19f), pt(side * 0.26f, 0.33f), pt(side * 0.12f, 0.43f))
            else ->
                limb(0.055f, pt(side * 0.145f, 0.19f), pt(side * 0.19f, 0.35f), pt(side * 0.20f, 0.50f))
        }
    }
}
