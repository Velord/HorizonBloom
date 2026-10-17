package com.velord.horizonbloom

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath

/*
 * Sun position, height, and drift locate this white-to-warm disc glow.
 * sunSize sets radius; sunrise/intro supplies light. Drawn after planet
 * and clipped above it, before Inner Bloom and sunburst.
 */
private const val SUN_DISC_GLOW_RADIUS_FACTOR = 1.8f

internal fun DrawScope.drawSunDisc(scene: HorizonBloomScene) = with(scene) {
    if (sunRadius > 0f && light > 0f) {
        val sunDiscGlowRadius = sunRadius * SUN_DISC_GLOW_RADIUS_FACTOR
        // Mask the glow so its soft edge cannot bleed onto the planet surface.
        clipPath(planetPath, clipOp = ClipOp.Difference) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = (light * 0.42f).coerceIn(0f, 1f)),
                    0.35f to config.warmColor.copy(alpha = (light * 0.58f).coerceIn(0f, 1f)),
                    1f to Color.Transparent,
                    center = sunCenter,
                    radius = sunDiscGlowRadius,
                ),
                center = sunCenter,
                radius = sunDiscGlowRadius,
            )
        }
    }
}
