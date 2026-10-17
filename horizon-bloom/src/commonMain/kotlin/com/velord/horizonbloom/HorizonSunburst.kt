package com.velord.horizonbloom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Alternating long and short rays follow the drifting sun after Inner Bloom.
 * sunburst, ray count, sunSize, and sunrise/intro shape and gate the rays.
 * Each warm glow has a white core; planet masking hides submerged segments
 * before the rim pass.
 */
internal const val SUNBURST_RAY_COUNT_DEFAULT = 8
private const val SUNBURST_RENDER_RAY_LIMIT = 256
private const val SUNBURST_ENERGY_GAIN = 0.9f
private const val SUNBURST_LONG_RAY_FACTOR = 7.2f
private const val SUNBURST_SHORT_RAY_FACTOR = 4.9f
private const val SUNBURST_START_FACTOR = 0.3f
private const val SUNBURST_GLOW_WIDTH_FACTOR = 0.3f
private const val SUNBURST_CORE_WIDTH_FACTOR = 0.085f

internal fun DrawScope.drawSunburst(scene: HorizonBloomScene) = with(scene) {
    // Fade rays as the sun crosses the horizon, then discard segments inside the planet.
    val visibleSun = flareVisibility(horizonAtSun, sunCenter.y, sunRadius)
    val intensity = sunburstIntensity(config.sunburst, light, visibleSun)
    clipPath(planetPath, clipOp = ClipOp.Difference) {
        drawSunburst(
            config = config,
            sun = sunCenter,
            sunRadius = sunRadius,
            intensity = intensity,
        )
    }
}

internal fun sunburstIntensity(sunburst: Float, light: Float, visibility: Float): Float {
    // Visibility modulates combined slider and scene light before additive rays are capped.
    val energy = sunburst.coerceIn(0f, 2f) * light.coerceIn(0f, 2f)
    return (energy * visibility.coerceIn(0f, 1f) * SUNBURST_ENERGY_GAIN).coerceIn(0f, 1f)
}

internal fun DrawScope.drawSunburst(
    config: HorizonBloomConfig,
    sun: Offset,
    sunRadius: Float,
    intensity: Float,
) {
    if (intensity <= 0f || sunRadius <= 0f) return

    val rayCount = config.sunburstRayCount.coerceIn(0, SUNBURST_RENDER_RAY_LIMIT)
    repeat(rayCount) { index ->
        val angle = sunburstRayAngle(index, rayCount)
        val direction = Offset(cos(angle), sin(angle))
        // A perpendicular vector supplies the two sides of each tapered ray.
        val perpendicular = Offset(-direction.y, direction.x)
        // Alternating lengths keep neighboring rays from forming a uniform ring.
        val lengthFactor = if (index % 2 == 0) {
            SUNBURST_LONG_RAY_FACTOR
        } else {
            SUNBURST_SHORT_RAY_FACTOR
        }
        val length = sunRadius * lengthFactor * (0.85f + intensity * 0.15f)
        val start = sun + direction * (sunRadius * SUNBURST_START_FACTOR)
        val end = sun + direction * length

        // Wide warm ray and narrow white core form each streak.
        drawSunburstRay(
            start = start,
            end = end,
            perpendicular = perpendicular,
            width = sunRadius * SUNBURST_GLOW_WIDTH_FACTOR,
            intensity = intensity * 0.12f,
            color = config.warmColor,
        )
        drawSunburstRay(
            start = start,
            end = end,
            perpendicular = perpendicular,
            width = sunRadius * SUNBURST_CORE_WIDTH_FACTOR,
            intensity = intensity * 0.3f,
            color = Color.White,
        )
    }
}

// Half a sector offsets the entire fan from a ray placed exactly on the first axis.
internal fun sunburstRayAngle(index: Int, rayCount: Int): Float =
    (index + 0.5f) * PI.toFloat() * 2f / rayCount

private fun DrawScope.drawSunburstRay(
    start: Offset,
    end: Offset,
    perpendicular: Offset,
    width: Float,
    intensity: Float,
    color: Color,
) {
    val side = perpendicular * width
    // Triangle starts wide at the disc and converges to a single distant tip.
    val path = Path().apply {
        moveTo(start.x - side.x, start.y - side.y)
        lineTo(end.x, end.y)
        lineTo(start.x + side.x, start.y + side.y)
        close()
    }
    // The gradient fades lengthwise; additive blending lets overlapping rays build light.
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            0f to color.copy(alpha = intensity),
            0.22f to color.copy(alpha = intensity * 0.78f),
            0.7f to color.copy(alpha = intensity * 0.08f),
            1f to Color.Transparent,
            start = start,
            end = end,
        ),
        blendMode = BlendMode.Plus,
    )
}
