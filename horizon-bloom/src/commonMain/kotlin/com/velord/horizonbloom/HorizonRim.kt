package com.velord.horizonbloom

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/*
 * After Sunburst, overlays soft colored edge light and a narrow core before Flare.
 * `rim` scales the assembled layer; `spread` limits reach along the curved horizon.
 * Its mask follows moving sun geometry; Atmosphere strength does not control it.
 */
private const val RIM_FULL_STRENGTH = 2f
private const val SPREAD_GLOW_LAYER_COUNT = 12
private const val SPREAD_RIM_CORE_WIDTH_HEIGHT_FACTOR = 0.003f

internal fun DrawScope.drawRim(scene: HorizonBloomScene) = with(scene) {
    // Map the full Rim range to opacity; Spread alone determines horizontal reach.
    val boundedRim = config.rim.coerceAtLeast(0f)
    val rimOpacity = (boundedRim / RIM_FULL_STRENGTH).coerceAtMost(1f)
    if (rimOpacity <= 0f) return@with

    val canvas = drawContext.canvas
    val layerBounds = Rect(0f, 0f, width, height)
    val layerPaint = Paint().apply { alpha = rimOpacity }
    // Scale combined strokes once so overlap does not change the Rim slider response.
    canvas.saveLayer(layerBounds, layerPaint)
    try {
        // Mask the assembled glow, then add a separate thin edge using that mask as color.
        drawWithSpreadMask(spreadRimMaskBrush) { drawRimLighting(scene) }
        drawPath(
            path = horizonPath,
            brush = spreadRimMaskBrush,
            style = Stroke(
                width = height * SPREAD_RIM_CORE_WIDTH_HEIGHT_FACTOR,
                cap = StrokeCap.Round,
            ),
        )
    } finally {
        canvas.restore()
    }
}

private fun DrawScope.drawRimLighting(scene: HorizonBloomScene) = with(scene) {
    // Strokes narrow and brighten toward the core to approximate a soft edge bloom.
    repeat(SPREAD_GLOW_LAYER_COUNT) { index ->
        val focus = (index + 1f) / SPREAD_GLOW_LAYER_COUNT
        val widthFraction = 0.004f + (1f - focus) * 0.044f
        val opacity = RIM_FULL_STRENGTH * (0.024f + focus * focus * 0.05f)
        drawPath(
            path = horizonPath,
            color = Color(0xFFF9DBFF).copy(alpha = opacity.coerceIn(0f, 1f)),
            style = Stroke(width = height * widthFraction, cap = StrokeCap.Round),
        )
    }
    // Solid white and cool lines anchor the edge beneath its final color variation.
    drawPath(
        path = horizonPath,
        color = Color.White,
        style = Stroke(
            width = height * SPREAD_RIM_CORE_WIDTH_HEIGHT_FACTOR,
            cap = StrokeCap.Round,
        ),
    )
    drawPath(
        path = horizonPath,
        color = config.coolColor.copy(alpha = (0.24f * RIM_FULL_STRENGTH).coerceIn(0f, 1f)),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round),
    )
    // The last hairline shifts from cool to warm across the viewport, then back to cool.
    drawPath(
        path = horizonPath,
        brush = Brush.horizontalGradient(
            0f to config.coolColor.copy(alpha = (0.26f * RIM_FULL_STRENGTH).coerceIn(0f, 1f)),
            0.45f to config.warmColor.copy(alpha = (0.55f * RIM_FULL_STRENGTH).coerceIn(0f, 1f)),
            0.75f to Color.White,
            1f to config.coolColor.copy(alpha = (0.3f * RIM_FULL_STRENGTH).coerceIn(0f, 1f)),
        ),
        style = Stroke(width = 1.4f, cap = StrokeCap.Round),
    )
}
