package com.arnav.sculpt

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Two circular arrows around a lens: the "flip camera" icon. Drawn in code, no icon library. */
@Composable
fun FlipCameraIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = center
        val r = s * 0.30f
        val stroke = s * 0.085f
        val topLeft = Offset(c.x - r, c.y - r)
        val arcSize = Size(2 * r, 2 * r)

        listOf(200f, 20f).forEach { start ->
            val sweep = 130f
            drawArc(
                color = tint,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Arrowhead at the end of the arc, pointing along the direction of travel.
            val rad = Math.toRadians((start + sweep).toDouble())
            val px = c.x + r * cos(rad).toFloat()
            val py = c.y + r * sin(rad).toFloat()
            val tx = -sin(rad).toFloat()
            val ty = cos(rad).toFloat()
            val nx = cos(rad).toFloat()
            val ny = sin(rad).toFloat()
            val h = s * 0.17f
            val head = Path().apply {
                moveTo(px + tx * h, py + ty * h)
                lineTo(px + nx * h * 0.8f, py + ny * h * 0.8f)
                lineTo(px - nx * h * 0.8f, py - ny * h * 0.8f)
                close()
            }
            drawPath(head, tint, style = Fill)
        }

        drawCircle(color = tint, radius = r * 0.32f, center = c, style = Stroke(width = stroke * 0.8f))
    }
}

/** Lightning bolt. Off = outlined with a slash, Auto = solid white plus an "A", On = solid yellow. */
@Composable
fun FlashIcon(mode: Int, modifier: Modifier = Modifier) {
    val on = mode == ImageCapture.FLASH_MODE_ON
    val off = mode == ImageCapture.FLASH_MODE_OFF
    val tint = if (on) Color(0xFFFFD54F) else Color.White

    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val k = size.minDimension / 24f
            val bolt = Path().apply {
                moveTo(7f * k, 2f * k)
                lineTo(7f * k, 13f * k)
                lineTo(10f * k, 13f * k)
                lineTo(10f * k, 22f * k)
                lineTo(17f * k, 10f * k)
                lineTo(13f * k, 10f * k)
                lineTo(17f * k, 2f * k)
                close()
            }
            if (off) {
                drawPath(bolt, tint, style = Stroke(width = 1.6f * k, join = StrokeJoin.Round))
                drawLine(
                    color = tint,
                    start = Offset(3f * k, 3f * k),
                    end = Offset(21f * k, 21f * k),
                    strokeWidth = 1.8f * k,
                    cap = StrokeCap.Round
                )
            } else {
                drawPath(bolt, tint, style = Fill)
            }
        }
        if (mode == ImageCapture.FLASH_MODE_AUTO) {
            Text(
                text = "A",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
