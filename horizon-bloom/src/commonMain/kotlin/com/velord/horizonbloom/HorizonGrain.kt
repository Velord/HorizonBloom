package com.velord.horizonbloom

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.random.Random

/*
 * Grain adds film-like texture over the finished Horizon Bloom scene.
 * Eight lazily built tiles contain light and dark pixels with transparent gaps. A shader repeats
 * the selected tile across the viewport; scene time cycles through tiles to animate the texture.
 * Speed changes that cycle rate, and Pause freezes it. The Grain setting controls overlay opacity;
 * zero skips drawing. The sky uses full strength, while the planet gets a quieter clipped pass.
 * Grain changes the final image, not the geometry or calculations of other light effects.
 */
private const val GRAIN_TILE_SIZE_PX = 256
private const val GRAIN_FRAME_COUNT = 8
private const val GRAIN_PATTERN_REFRESH_RATE_HZ = 24f
private const val GRAIN_RANDOM_SEED = 947
private const val GRAIN_GROUP_COUNT = 4
private const val GRAIN_LIGHT_SOFT_GROUP = 0
private const val GRAIN_LIGHT_STRONG_GROUP = 1
private const val GRAIN_DARK_SOFT_GROUP = 2
private const val GRAIN_DARK_STRONG_GROUP = 3
private const val GRAIN_POINT_COORDINATES = 2
private const val GRAIN_POINT_CENTER_OFFSET = 0.5f
private const val GRAIN_POINT_SIZE_PX = 1f
private const val GRAIN_LIGHT_SOFT_THRESHOLD = 0.2f
private const val GRAIN_LIGHT_STRONG_THRESHOLD = 0.4f
private const val GRAIN_DARK_SOFT_THRESHOLD = 0.575f
private const val GRAIN_DARK_STRONG_THRESHOLD = 0.75f
private const val GRAIN_LIGHT_SOFT_OPACITY = 0.03f
private const val GRAIN_LIGHT_STRONG_OPACITY = 0.055f
private const val GRAIN_DARK_SOFT_OPACITY = 0.18f
private const val GRAIN_DARK_STRONG_OPACITY = 0.32f
private const val GRAIN_PLANET_OPACITY_SCALE = 0.55f
internal const val GRAIN_DEFAULT_STRENGTH = 0.25f

// Build each noise tile on first use instead of creating every frame during the first draw.
private val grainBrushes = Array(GRAIN_FRAME_COUNT) { frame ->
    lazy { createGrainBrush(frame) }
}

internal fun DrawScope.drawGrain(scene: HorizonBloomScene) = with(scene) {
    val strength = config.grain.coerceAtLeast(0f)
    if (strength == 0f) return@with

    // Scene time already reflects Speed and Pause, so grain moves with the rest of the scene.
    val frameProgress = frame.elapsed * GRAIN_PATTERN_REFRESH_RATE_HZ
    val tileFrame = frameProgress.toInt()
    val brushIndex = tileFrame % GRAIN_FRAME_COUNT
    val brush = grainBrushes[brushIndex].value
    // Difference excludes the planet; the second pass clips to it at lower opacity.
    clipPath(planetPath, clipOp = ClipOp.Difference) {
        drawRect(brush = brush, alpha = strength.coerceAtMost(1f))
    }
    clipPath(planetPath) {
        drawRect(brush = brush, alpha = (strength * GRAIN_PLANET_OPACITY_SCALE).coerceAtMost(1f))
    }
}

private fun createGrainBrush(frame: Int): Brush {
    val image = ImageBitmap(GRAIN_TILE_SIZE_PX, GRAIN_TILE_SIZE_PX)
    val canvas = Canvas(image)
    // A fixed seed per frame keeps the tile stable until scene time selects another frame.
    val random = Random(GRAIN_RANDOM_SEED + frame)
    val pixelCount = GRAIN_TILE_SIZE_PX * GRAIN_TILE_SIZE_PX
    val points = Array(GRAIN_GROUP_COUNT) {
        FloatArray(pixelCount * GRAIN_POINT_COORDINATES)
    }
    val pointCounts = IntArray(GRAIN_GROUP_COUNT)

    // Group random pixel centers by shade so each shade can be drawn in one batch.
    for (y in 0 until GRAIN_TILE_SIZE_PX) {
        for (x in 0 until GRAIN_TILE_SIZE_PX) {
            val sample = random.nextFloat()
            val group = grainGroup(sample) ?: continue

            val coordinates = points[group]
            // drawRawPoints reads alternating x/y floats; this index is the next free float.
            val index = pointCounts[group]
            coordinates[index] = x + GRAIN_POINT_CENTER_OFFSET
            coordinates[index + 1] = y + GRAIN_POINT_CENTER_OFFSET
            pointCounts[group] = index + GRAIN_POINT_COORDINATES
        }
    }

    // Disable antialiasing so each point remains a sharp, one-pixel grain sample.
    val paint = Paint().apply {
        isAntiAlias = false
        style = PaintingStyle.Stroke
        strokeWidth = GRAIN_POINT_SIZE_PX
    }
    for (group in 0 until GRAIN_GROUP_COUNT) {
        paint.color = grainColor(group)
        val coordinates = points[group].copyOf(pointCounts[group])
        canvas.drawRawPoints(PointMode.Points, coordinates, paint)
    }

    // Repeat the fixed-size tile rather than stretching its one-pixel grain to the viewport.
    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader = ImageShader(
            image = image,
            tileModeX = TileMode.Repeated,
            tileModeY = TileMode.Repeated,
        )
    }
}

// Four thresholds assign light and dark shades; remaining positions stay transparent.
private fun grainGroup(sample: Float): Int? = when {
    sample < GRAIN_LIGHT_SOFT_THRESHOLD -> GRAIN_LIGHT_SOFT_GROUP
    sample < GRAIN_LIGHT_STRONG_THRESHOLD -> GRAIN_LIGHT_STRONG_GROUP
    sample < GRAIN_DARK_SOFT_THRESHOLD -> GRAIN_DARK_SOFT_GROUP
    sample < GRAIN_DARK_STRONG_THRESHOLD -> GRAIN_DARK_STRONG_GROUP
    else -> null
}

private fun grainColor(group: Int): Color = when (group) {
    GRAIN_LIGHT_SOFT_GROUP -> Color.White.copy(alpha = GRAIN_LIGHT_SOFT_OPACITY)
    GRAIN_LIGHT_STRONG_GROUP -> Color.White.copy(alpha = GRAIN_LIGHT_STRONG_OPACITY)
    GRAIN_DARK_SOFT_GROUP -> Color.Black.copy(alpha = GRAIN_DARK_SOFT_OPACITY)
    GRAIN_DARK_STRONG_GROUP -> Color.Black.copy(alpha = GRAIN_DARK_STRONG_OPACITY)
    else -> error("Unknown grain group: $group")
}
