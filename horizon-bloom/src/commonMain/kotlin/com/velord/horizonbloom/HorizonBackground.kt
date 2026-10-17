package com.velord.horizonbloom

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/*
 * Fills the canvas with backgroundColor and a faint vertical cool/warm wash.
 * atmosphere and sunrise/intro scale the wash; it is the scene's first pass.
 */
internal fun DrawScope.drawBackground(scene: HorizonBloomScene) = with(scene) {
    drawRect(config.backgroundColor)
    // The later planet pass hides this lower-sky wash below the curved edge.
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.48f to Color.Transparent,
            0.82f to config.coolColor.copy(alpha = (upperGlow * 0.07f).coerceIn(0f, 1f)),
            1f to config.warmColor.copy(alpha = (light * 0.035f).coerceIn(0f, 1f)),
        ),
    )
}
