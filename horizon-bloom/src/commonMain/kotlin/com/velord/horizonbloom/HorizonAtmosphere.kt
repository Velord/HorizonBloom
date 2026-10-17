package com.velord.horizonbloom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import kotlin.math.sqrt

/*
 * Before the planet, paints cool sky haze/halo and a sun-centered warm glow.
 * `atmosphere` sets strength, `thickness` shapes height, and `spread` masks cool reach.
 * All passes exclude the planet; Sunrise/Intro also scale the Spread-unmasked warm glow.
 */
private const val SPREAD_SKY_PEAK_OPACITY = 1f
private const val SKY_HALO_COOL_MIX = 0.25f
private const val SPREAD_SKY_HEIGHT_MULTIPLIER = 0.25f
private const val SPREAD_SKY_NEAR_FRACTION = 0.09f
private const val SPREAD_SKY_MID_FRACTION = 0.27f

internal fun DrawScope.drawAtmosphere(scene: HorizonBloomScene) = with(scene) {
    if (upperGlow <= 0f) return@with

    // Horizontal radius and vertical compression make a wide, shallow glow at the horizon.
    val center = Offset(centerX, horizonY)
    val radius = width * (0.65f + config.thickness * 0.16f)
    val heightScale = 0.16f + config.thickness * 0.08f
    // The saved-layer mask fades cool haze along the arc; clipping removes the planet.
    drawWithSpreadMask(spreadOuterAtmosphereMaskBrush) {
        clipPath(planetPath, clipOp = ClipOp.Difference) {
            scale(scaleX = 1f, scaleY = heightScale, pivot = center) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to config.coolColor.copy(alpha = upperGlow * 0.17f),
                        0.42f to config.coolColor.copy(alpha = upperGlow * 0.08f),
                        1f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                    center = center,
                    radius = radius,
                )
            }
        }
    }
    // The narrower mask keeps the sharper halo closer to the sun along the horizon.
    drawWithSpreadMask(spreadSkyHaloMaskBrush) {
        clipPath(planetPath, clipOp = ClipOp.Difference) {
            drawSkyHalo(scene)
        }
    }
    // Warm light follows the sun without Spread; the scene caps Atmosphere's gain at one.
    if (warmAtmosphereLight > 0f) {
        clipPath(planetPath, clipOp = ClipOp.Difference) {
            scale(scaleX = 1f, scaleY = heightScale, pivot = sunCenter) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to config.warmColor.copy(alpha = warmAtmosphereLight * 0.14f),
                        0.45f to config.warmColor.copy(alpha = warmAtmosphereLight * 0.05f),
                        1f to Color.Transparent,
                        center = sunCenter,
                        radius = radius * 0.6f,
                    ),
                    center = sunCenter,
                    radius = radius * 0.6f,
                )
            }
        }
    }
}

private fun DrawScope.drawSkyHalo(scene: HorizonBloomScene) = with(scene) {
    // Thickness sets skyward reach; near and mid-stops are pixel distances from the edge.
    val haloHeight = height * (0.14f + config.thickness * 0.08f) * SPREAD_SKY_HEIGHT_MULTIPLIER
    val nearDistance = haloHeight * SPREAD_SKY_NEAR_FRACTION
    val midDistance = haloHeight * SPREAD_SKY_MID_FRACTION
    // All stops share one warm/cool hue while opacity falls from the rim into the sky.
    val peakOpacity = upperGlow * SPREAD_SKY_PEAK_OPACITY
    val nearOpacity = peakOpacity * 0.55f
    val midOpacity = peakOpacity * 0.27f
    val haloColor = lerp(config.warmColor, config.coolColor, SKY_HALO_COOL_MIX)
    val peakColor = haloColor.copy(alpha = peakOpacity.coerceIn(0f, 1f))
    val nearColor = haloColor.copy(alpha = nearOpacity.coerceIn(0f, 1f))
    val midColor = haloColor.copy(alpha = midOpacity.coerceIn(0f, 1f))
    // Fit sampled edge points so the halo can approximate the horizon's curvature.
    val glowCircle = horizonGlowCircle(
        width = width,
        centerX = centerX,
        horizonY = horizonY,
        leftEdgeY = edgeY(0f),
        rightEdgeY = edgeY(width),
    )
    // A flat horizon has no finite glow circle, so fade along its tilted normal instead.
    val haloBrush = if (glowCircle == null) {
        // (tiltSlope, -1) points skyward; normalization keeps haloHeight in screen pixels.
        val normalLength = sqrt(1f + tiltSlope * tiltSlope)
        val gradientEnd = Offset(
            x = centerX + tiltSlope * haloHeight / normalLength,
            y = horizonY - haloHeight / normalLength,
        )
        // Stop fractions place near and mid fades along this edge-to-sky line.
        Brush.linearGradient(
            0f to peakColor,
            SPREAD_SKY_NEAR_FRACTION to nearColor,
            SPREAD_SKY_MID_FRACTION to midColor,
            1f to Color.Transparent,
            start = Offset(centerX, horizonY),
            end = gradientEnd,
        )
    } else {
        val circleCenter = Offset(x = glowCircle.centerX, y = glowCircle.centerY)
        // The fitted radius marks the sampled edge; extra height extends the fade beyond it.
        val gradientRadius = glowCircle.radius + haloHeight
        // Convert edge-relative pixel distances into stops on the expanded radial gradient.
        val edgeStop = glowCircle.radius / gradientRadius
        val nearStop = (glowCircle.radius + nearDistance) / gradientRadius
        val midStop = (glowCircle.radius + midDistance) / gradientRadius
        // Fade in from a transparent center, peak at the edge, then fade into the sky.
        Brush.radialGradient(
            0f to Color.Transparent,
            edgeStop to peakColor,
            nearStop to nearColor,
            midStop to midColor,
            1f to Color.Transparent,
            center = circleCenter,
            radius = gradientRadius,
        )
    }
    // Fill the viewport; the caller's planet clip and Spread mask limit visible pixels.
    drawRect(brush = haloBrush)
}
