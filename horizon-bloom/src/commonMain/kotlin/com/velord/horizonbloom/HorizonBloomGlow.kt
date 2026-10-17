package com.velord.horizonbloom

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/*
 * Two sun-centered glows follow drift and scale with bloom.
 * Outer Bloom paints a broad wash before atmosphere and planet; its cool tint
 * follows atmosphere, while sunrise/intro sets warm light.
 * Inner Bloom is tighter and drawn after planet and sun disc, before sunburst.
 */
private const val BLOOM_BASE_WIDTH_FACTOR = 0.532f
private const val BLOOM_BASE_GAIN = 0.8f
private const val BLOOM_POWER_GAIN = 0.4f
private const val INNER_BLOOM_RADIUS_FACTOR = 0.42f

// Shared base radius keeps both passes scaled together as bloom changes.
internal fun bloomSize(width: Float, bloomPower: Float): Float {
    val bloomPowerContribution = bloomPower * BLOOM_POWER_GAIN
    val bloomGain = BLOOM_BASE_GAIN + bloomPowerContribution
    val baseBloomSize = width * BLOOM_BASE_WIDTH_FACTOR
    return baseBloomSize * bloomGain
}

internal fun DrawScope.drawOuterBloom(scene: HorizonBloomScene) = with(scene) {
    // Warm center follows sunrise light; the cool shoulder follows atmosphere independently.
    drawCircle(
        brush = Brush.radialGradient(
            0f to config.warmColor.copy(alpha = (light * bloomPower * 0.26f).coerceIn(0f, 1f)),
            0.19f to config.coolColor.copy(
                alpha = (upperGlow * bloomPower * 0.17f).coerceIn(0f, 1f),
            ),
            1f to Color.Transparent,
            center = sunCenter,
            radius = bloomSize,
        ),
        center = sunCenter,
        radius = bloomSize,
    )
}

internal fun DrawScope.drawInnerBloom(scene: HorizonBloomScene) = with(scene) {
    // Drawn after the planet, this compact pass brightens the sun's immediate edge.
    val innerBloomRadius = bloomSize * INNER_BLOOM_RADIUS_FACTOR
    drawCircle(
        brush = Brush.radialGradient(
            0f to config.warmColor.copy(alpha = (light * bloomPower * 0.42f).coerceIn(0f, 1f)),
            0.06f to config.coolColor.copy(
                alpha = (light * bloomPower * 0.25f).coerceIn(0f, 1f),
            ),
            1f to Color.Transparent,
            center = sunCenter,
            radius = innerBloomRadius,
        ),
        center = sunCenter,
        radius = innerBloomRadius,
    )
}
