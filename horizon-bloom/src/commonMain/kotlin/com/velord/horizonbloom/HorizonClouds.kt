package com.velord.horizonbloom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.sin

/*
 * After planet fill, paints faint wisps and source-local haze inside its silhouette.
 * `clouds` sets strength; scene time and `drift` move wisps and the haze source.
 * Drawn before Sun Disc, Rim, and Flare; Spread does not affect this texture.
 */
private const val CLOUD_LAYER_COUNT = 3
private const val CLOUD_WISP_COUNT = 9
private const val CLOUD_BAND_BASE_OFFSET = 0.014f
private const val CLOUD_BAND_LAYER_OFFSET = 0.018f
private const val CLOUD_BAND_WAVE_OFFSET = 0.004f
private const val CLOUD_OPACITY_BASE = 0.095f
private const val CLOUD_OPACITY_LAYER_GAIN = 0.012f
private const val CLOUD_OPACITY_MAX = 0.19f
private const val CLOUD_DRIFT_BASE_RATE = 0.08f
private const val CLOUD_DRIFT_LAYER_RATE = 0.02f

internal fun DrawScope.drawClouds(scene: HorizonBloomScene) = with(scene) {
    drawClouds(
        config = config,
        time = frame.elapsed,
        edgeY = ::edgeY,
        planetPath = planetPath,
    )
}

internal fun DrawScope.drawClouds(
    config: HorizonBloomConfig,
    time: Float,
    edgeY: (Float) -> Float,
    planetPath: Path,
) {
    val strength = config.clouds.coerceIn(0f, 2f)
    if (strength == 0f) return

    val width = size.width
    val height = size.height
    val sunPosition = sunX(width, config.sunPosition, config.drift, time)
    // The planet clip keeps wisps and haze below the moving curved edge.
    clipPath(planetPath) {
        // Seeded wisp depth and size stay fixed while phase adds gentle motion.
        repeat(CLOUD_LAYER_COUNT) { layer ->
            val driftRate = CLOUD_DRIFT_BASE_RATE + layer * CLOUD_DRIFT_LAYER_RATE
            val phase = time * config.drift * driftRate
            repeat(CLOUD_WISP_COUNT) { index ->
                val seed = index + layer * CLOUD_WISP_COUNT
                val position = 0.18f + random01(seed * 13 + 3) * 0.64f
                val movement = sin(phase + seed * 1.4f) * width * 0.012f
                val x = (width * position + movement).coerceIn(0f, width)
                val depth = height * (0.008f + random01(seed * 17 + 7) * 0.065f)
                val y = edgeY(x) + depth
                val length = width * (0.012f + random01(seed * 19 + 11) * 0.058f)
                val amplitude = height * (0.002f + random01(seed * 23 + 5) * 0.005f)
                // Only wisps near the light source remain visible; distant ones fade out.
                val proximity = (1f - kotlin.math.abs(x - sunPosition) / (width * 0.5f))
                    .coerceIn(0f, 1f)
                val alpha = cloudOpacity(strength, layer) * 0.6f * proximity
                val startX = (x - length * 0.5f).coerceAtLeast(0f)
                val endX = (x + length * 0.5f).coerceAtMost(width)
                // A single quadratic arc gives each short wisp a shallow crest.
                val path = Path().apply {
                    moveTo(startX, y + amplitude * 0.5f)
                    quadraticTo(x, y - amplitude, endX, y + amplitude * 0.35f)
                }
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.42f to config.coolColor.copy(alpha = alpha * 0.7f),
                        0.64f to config.warmColor.copy(alpha = alpha * 0.24f),
                        1f to Color.Transparent,
                        startX = startX,
                        endX = endX,
                    ),
                    style = Stroke(
                        width = height * (0.002f + random01(seed * 29 + 9) * 0.004f),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }

        // Haze clinging to the planet immediately beneath its curved rim.
        val hazeCenter = Offset(sunPosition, edgeY(sunPosition) + height * 0.025f)
        val hazeRadius = width * 0.19f
        scale(scaleX = 1f, scaleY = 0.085f, pivot = hazeCenter) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to config.warmColor.copy(alpha = strength * 0.01f),
                    1f to Color.Transparent,
                    center = hazeCenter,
                    radius = hazeRadius,
                ),
                center = hazeCenter,
                radius = hazeRadius,
            )
        }
    }
}

internal fun cloudBandY(rimY: Float, height: Float, layer: Int, wave: Float): Float {
    // Layer offset places bands below the rim; bounded wave adds only a small wobble.
    val layerOffset = layer * CLOUD_BAND_LAYER_OFFSET
    val baseBand = CLOUD_BAND_BASE_OFFSET + layerOffset
    val boundedWave = wave.coerceIn(-1f, 1f)
    val waveOffset = boundedWave * CLOUD_BAND_WAVE_OFFSET
    val bandFraction = baseBand + waveOffset
    val verticalOffset = height * bandFraction
    return rimY + verticalOffset
}

internal fun cloudOpacity(strength: Float, layer: Int): Float {
    val boundedStrength = strength.coerceIn(0f, 2f)
    val boundedLayer = layer.coerceIn(0, 2)
    val layerGain = boundedLayer * CLOUD_OPACITY_LAYER_GAIN
    val layerWeight = CLOUD_OPACITY_BASE + layerGain
    val opacity = boundedStrength * layerWeight
    return opacity.coerceIn(0f, CLOUD_OPACITY_MAX)
}
