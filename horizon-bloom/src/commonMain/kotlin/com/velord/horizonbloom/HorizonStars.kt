package com.velord.horizonbloom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/*
 * Seeds fixed stars above the curved horizon, with occasional cool halos.
 * stars sets opacity, elapsed time twinkles, and interactive pointer parallax shifts them.
 * Drawn over background and below aurora.
 */
private const val STAR_COUNT = 180
private const val STAR_HORIZON_MARGIN_HEIGHT_FACTOR = 0.035f
private const val STAR_HALO_INTERVAL = 19

internal fun DrawScope.drawStars(scene: HorizonBloomScene) = with(scene) {
    // Stable pseudo-random positions avoid allocating a particle simulation each frame.
    val horizonMargin = height * STAR_HORIZON_MARGIN_HEIGHT_FACTOR
    repeat(STAR_COUNT) { index ->
        val x = starParallaxX(random01(index * 71 + 17) * width, cameraShift, width)
        val y = random01(index * 113 + 9) * height
        // Leave a dark gap above the local curved edge instead of clipping stars at the rim.
        val starVisibilityLimit = edgeY(x) - horizonMargin
        if (y < starVisibilityLimit) {
            // Seeded brightness stays fixed; phase-offset sine changes only each star's twinkle.
            val twinkle = 0.75f + 0.25f * sin(frame.elapsed + index * 2.3f)
            val brightness = random01(index * 19 + 7)
            val starAlpha = (config.stars * brightness * twinkle * 0.58f).coerceIn(0f, 1f)
            // Sparse halos add depth without turning the star field into a diffuse wash.
            if (index % STAR_HALO_INTERVAL == 0) {
                val haloCenter = Offset(x, y)
                val haloRadius = 3f + brightness * 3f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to config.coolColor.copy(alpha = starAlpha * 0.18f),
                        1f to Color.Transparent,
                        center = haloCenter,
                        radius = haloRadius,
                    ),
                    center = haloCenter,
                    radius = haloRadius,
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = starAlpha),
                radius = 0.3f + random01(index * 23 + 3) * 1.15f,
                center = Offset(x, y),
            )
        }
    }
}
