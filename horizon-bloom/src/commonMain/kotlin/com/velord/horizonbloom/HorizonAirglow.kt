package com.velord.horizonbloom

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.exp

/*
 * Between Atmosphere and planet, adds faint lavender edge and source-local sky light.
 * `airglow` sets density; `spread` masks arc reach around moving sun geometry.
 * A separate upper band appears only above 1.4, before Rim draws its thin core.
 */
private const val AIRGLOW_LAYER_COUNT = 20
private const val AIRGLOW_RIM_LAYER_COUNT = 14

internal fun DrawScope.drawAirglow(scene: HorizonBloomScene) = with(scene) {
    val intensity = config.airglow.coerceIn(0f, 2f)
    if (intensity <= 0f) return@with

    // Spread clips the combined sky haze and edge strokes along distance from the sun.
    drawWithSpreadMask(spreadAirglowMaskBrush) { drawContent(scene, intensity) }
}

private fun DrawScope.drawContent(scene: HorizonBloomScene, intensity: Float) = with(scene) {
    // Rise quickly at low slider values while approaching a bounded density.
    val density = 1f - exp(-3f * intensity)
    // Only the sun-centered haze is clipped above the planet and flattened vertically.
    clipPath(planetPath, clipOp = ClipOp.Difference) {
        scale(scaleX = 1f, scaleY = 0.24f, pivot = sunCenter) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFEFCFFF).copy(alpha = density * 0.07f),
                    0.35f to config.coolColor.copy(alpha = density * 0.009f),
                    1f to Color.Transparent,
                    center = sunCenter,
                    radius = width * 0.42f,
                ),
                center = sunCenter,
                radius = width * 0.42f,
            )
        }
    }
    // Wide, translucent strokes build the edge fringe; the final thin stroke sharpens it.
    repeat(AIRGLOW_RIM_LAYER_COUNT) { index ->
        val focus = (index + 1f) / AIRGLOW_RIM_LAYER_COUNT
        val widthFraction = 0.017f + (1f - focus) * 0.092f
        val opacity = density * (0.009f + (1f - focus) * 0.006f)
        drawPath(
            path = horizonPath,
            color = Color(0xFFEBC9FF).copy(alpha = opacity),
            style = Stroke(width = height * widthFraction, cap = StrokeCap.Round),
        )
    }
    drawPath(
        path = horizonPath,
        color = Color(0xFFFFDEFF).copy(alpha = density * 0.11f),
        style = Stroke(width = height * 0.012f, cap = StrokeCap.Round),
    )

    val upperProgress = ((intensity - 1.4f) / 0.6f).coerceIn(0f, 1f)
    if (upperProgress <= 0f) return@with

    // Ease in the upper band so crossing its threshold does not reveal a hard stripe.
    val upperDensity = upperProgress * upperProgress * (3f - 2f * upperProgress)
    // Sample the same curved edge at a fixed skyward offset to form a separate upper band.
    val airglowPath = Path().apply {
        moveTo(0f, edgeY(0f) - height * 0.035f)
        for (index in 1..EDGE_SEGMENTS) {
            val x = width * index / EDGE_SEGMENTS
            lineTo(x, edgeY(x) - height * 0.035f)
        }
    }
    // As strokes narrow, their opacity rises to give the upper band a soft center.
    repeat(AIRGLOW_LAYER_COUNT) { index ->
        val focus = (index + 1f) / AIRGLOW_LAYER_COUNT
        val widthFraction = 0.006f + (1f - focus) * 0.046f
        val opacity = upperDensity * (0.003f + focus * focus * 0.016f)
        drawPath(
            path = airglowPath,
            color = Color(0xFFE6C6FF).copy(alpha = opacity),
            style = Stroke(width = height * widthFraction, cap = StrokeCap.Round),
        )
    }
}
