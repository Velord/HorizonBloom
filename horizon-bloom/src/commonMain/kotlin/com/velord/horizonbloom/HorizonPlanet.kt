package com.velord.horizonbloom

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/*
 * Paints the dark foreground over earlier sky effects, before Clouds and Sun Disc.
 * `atmosphere` controls shallow edge tints; Sunrise/Intro further scale the warm tint.
 * The later Sun Disc, Sunburst, and flare hotspot clip against this silhouette.
 */
private val PLANET_GRADIENT_COLORS = listOf(Color(0xFF08070D), Color(0xFF08070D), Color.Black)

internal fun DrawScope.drawPlanet(scene: HorizonBloomScene) = with(scene) {
    drawPath(
        path = planetPath,
        brush = Brush.verticalGradient(
            colors = PLANET_GRADIENT_COLORS,
            startY = horizonY,
            endY = height,
        ),
    )
    // Keep the base silhouette dark while the shallow tint responds to scene light.
    // Fade the tint within the upper fifth of the viewport, leaving the lower planet black.
    drawPath(
        path = planetPath,
        brush = Brush.verticalGradient(
            0f to config.coolColor.copy(alpha = (upperGlow * 0.11f).coerceIn(0f, 1f)),
            0.45f to config.warmColor.copy(
                alpha = (warmAtmosphereLight * 0.055f).coerceIn(0f, 1f),
            ),
            1f to Color.Transparent,
            startY = horizonY,
            endY = horizonY + height * 0.2f,
        ),
    )
}
