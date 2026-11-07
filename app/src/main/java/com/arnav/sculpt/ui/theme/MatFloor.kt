package com.arnav.sculpt.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import kotlin.math.ceil
import kotlin.random.Random

private const val TILES_ACROSS = 4
private const val TEETH_PER_EDGE = 3
private const val GRAIN_SIZE = 128

/** A small speckle texture, tiled over the floor. Cheap to build and to draw. */
private fun grainBitmap(light: Int, dark: Int): Bitmap {
    val random = Random(4)
    val pixels = IntArray(GRAIN_SIZE * GRAIN_SIZE) {
        when (random.nextInt(100)) {
            in 0..7 -> light
            in 8..19 -> dark
            else -> 0
        }
    }
    return Bitmap.createBitmap(pixels, GRAIN_SIZE, GRAIN_SIZE, Bitmap.Config.ARGB_8888)
}

/**
 * A floor of interlocking rubber gym-mat tiles with a speckled grain.
 * The top [maroonRows] rows of tiles are maroon; the rest alternate black and charcoal.
 */
@Composable
fun MatFloor(
    modifier: Modifier = Modifier,
    maroonRows: Int = 0,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val mat = LocalMatColors.current
    val grain = remember(mat) {
        ShaderBrush(
            ImageShader(
                grainBitmap(mat.grainLight.toArgb(), mat.grainDark.toArgb()).asImageBitmap(),
                TileMode.Repeated,
                TileMode.Repeated
            )
        )
    }
    Box(
        modifier = modifier.drawWithCache {
            val tile = size.width / TILES_ACROSS
            val cols = ceil(size.width / tile).toInt()
            val rows = ceil(size.height / tile).toInt()
            val tooth = tile * 0.08f
            val seamStroke = Stroke(width = 1.5f, join = StrokeJoin.Round)

            val tiles = buildList {
                for (r in 0 until rows) for (c in 0 until cols) {
                    val checker = (r + c) % 2 == 0
                    val color = when {
                        r < maroonRows -> if (checker) mat.maroonA else mat.tileB
                        else -> if (checker) mat.tileA else mat.tileB
                    }
                    add(tilePath(c * tile, r * tile, tile, tooth) to color)
                }
            }

            onDrawBehind {
                drawRect(mat.seam)
                tiles.forEach { (path, color) ->
                    drawPath(path, color)
                    drawPath(path, mat.seam, style = seamStroke)
                }
                drawRect(grain)
            }
        },
        content = content
    )
}

/**
 * One tile: teeth stick out of the right and bottom edges and notches are cut into the left and top
 * edges, so a tile's teeth slot into its neighbors' notches.
 */
private fun tilePath(x: Float, y: Float, s: Float, depth: Float): Path {
    val segments = 2 * TEETH_PER_EDGE + 1
    val unit = s / segments
    val notch = depth + 1.5f
    return Path().apply {
        moveTo(x, y)
        for (k in 0 until segments) {
            val a = x + k * unit
            val b = a + unit
            if (k % 2 == 1) {
                lineTo(a, y + notch); lineTo(b, y + notch); lineTo(b, y)
            } else lineTo(b, y)
        }
        for (k in 0 until segments) {
            val a = y + k * unit
            val b = a + unit
            if (k % 2 == 1) {
                lineTo(x + s + depth, a); lineTo(x + s + depth, b); lineTo(x + s, b)
            } else lineTo(x + s, b)
        }
        for (k in 0 until segments) {
            val a = x + s - k * unit
            val b = a - unit
            if (k % 2 == 1) {
                lineTo(a, y + s + depth); lineTo(b, y + s + depth); lineTo(b, y + s)
            } else lineTo(b, y + s)
        }
        for (k in 0 until segments) {
            val a = y + s - k * unit
            val b = a - unit
            if (k % 2 == 1) {
                lineTo(x + notch, a); lineTo(x + notch, b); lineTo(x, b)
            } else lineTo(x, b)
        }
        close()
    }
}
