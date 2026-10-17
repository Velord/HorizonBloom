package com.velord.horizonbloom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/*
 * Draws cool-to-warm curtains above the horizon, over stars and below Outer Bloom.
 * autoAurora repeats pulses; interactive taps start one at bloomLocation.
 * aurora sets opacity while drift, scene speed, and auroraSpeed govern motion.
 */
internal const val AURORA_DURATION = 6.5f
internal const val AURORA_SPEED_DEFAULT = 1f

private const val AUTO_AURORA_INTERVAL = 10.5f
private const val AUTO_AURORA_CENTER = 0.5f
private const val AUTO_AURORA_WANDER = 0.17f
private const val AUTO_AURORA_SPAN = 0.46f
private const val TAP_AURORA_SPAN = 0.42f
private const val AURORA_LAYER_COUNT = 2
private const val AURORA_STRAND_COUNT = 90
private const val AURORA_VEIL_SEGMENTS = 44
private const val AURORA_VEIL_BASE_HEIGHT = 0.035f
private const val AURORA_VEIL_ENVELOPE_HEIGHT = 0.17f
private const val AURORA_VEIL_RIPPLE_HEIGHT = 0.013f
private const val AURORA_VEIL_OPACITY = 0.14f
private const val AURORA_RISE_DURATION = 0.9f
private const val AURORA_FADE_DURATION = 2f
private const val AURORA_MAX_OPACITY = 0.6f
private const val AURORA_STRAND_BASE_HEIGHT = 0.075f
private const val AURORA_STRAND_HEIGHT_VARIATION = 0.19f
private const val AURORA_STRAND_ENVELOPE_BASE = 0.65f
private const val AURORA_STRAND_ENVELOPE_GAIN = 0.35f
private const val AURORA_OPACITY_BASE = 0.26f
private const val AURORA_OPACITY_LAYER_GAIN = 0.045f

internal fun DrawScope.drawAurora(scene: HorizonBloomScene) = with(scene) {
    drawAutomaticAurora(config, frame.auroraElapsed, ::edgeY)
    // Tap pulses use their own age so an automatic pulse cannot restart their fade.
    val pulseAge = frame.auroraElapsed - frame.lastBloomTime
    if (config.interactive && pulseAge in 0f..AURORA_DURATION) {
        drawTapAurora(
            config = config,
            time = frame.auroraElapsed,
            edgeY = ::edgeY,
            originX = frame.bloomLocation,
            fade = auroraPulseEnvelope(pulseAge, AURORA_DURATION),
        )
    }
}

internal fun DrawScope.drawAutomaticAurora(
    config: HorizonBloomConfig,
    time: Float,
    edgeY: (Float) -> Float,
) {
    if (config.autoAurora.not()) return

    // Modulo restarts the rise/hold/fade envelope at each automatic pulse interval.
    val cycle = time % AUTO_AURORA_INTERVAL
    // Hold each pulse's center steady, then vary it between cycles.
    val center = AUTO_AURORA_CENTER +
        sin((time / AUTO_AURORA_INTERVAL).toInt() * 2.1f) * AUTO_AURORA_WANDER
    drawAuroraCurtain(
        config = config,
        time = time,
        edgeY = edgeY,
        center = center,
        span = AUTO_AURORA_SPAN,
        fade = auroraPulseEnvelope(cycle, AURORA_DURATION),
    )
}

internal fun DrawScope.drawTapAurora(
    config: HorizonBloomConfig,
    time: Float,
    edgeY: (Float) -> Float,
    originX: Float,
    fade: Float,
) {
    drawAuroraCurtain(
        config = config,
        time = time,
        edgeY = edgeY,
        center = originX,
        span = TAP_AURORA_SPAN,
        fade = fade,
    )
}

private fun DrawScope.drawAuroraCurtain(
    config: HorizonBloomConfig,
    time: Float,
    edgeY: (Float) -> Float,
    center: Float,
    span: Float,
    fade: Float,
) {
    if (config.aurora <= 0f || fade <= 0f) return

    val width = size.width
    val height = size.height
    val spread = width * span
    val centerX = width * center
    val halfSpread = spread * 0.5f
    val startX = centerX - halfSpread
    val veilWidth = spread / AURORA_VEIL_SEGMENTS * 1.6f
    // Layers share a footprint but drift at different rates, adding depth without relocating it.
    repeat(AURORA_LAYER_COUNT) { layer ->
        val phase = time * config.drift * (0.28f + layer * 0.07f)
        // Broad veil fills gaps before the finer strands are drawn.
        for (segment in 0..AURORA_VEIL_SEGMENTS) {
            val position = segment.toFloat() / AURORA_VEIL_SEGMENTS
            val x = startX + spread * position
            // Sine closes the curtain at both ends; ripple breaks its otherwise uniform top edge.
            val envelope = sin(position * PI.toFloat()).coerceAtLeast(0f)
            val envelopeHeight = envelope * AURORA_VEIL_ENVELOPE_HEIGHT
            val ripple = sin(position * 14f + phase + layer) * AURORA_VEIL_RIPPLE_HEIGHT
            val veilHeight = AURORA_VEIL_BASE_HEIGHT + envelopeHeight + ripple
            val baseY = edgeY(x)
            val topY = baseY - height * veilHeight
            // Squaring the envelope also fades the wide strokes before the curtain's ends.
            val veilAlpha = (config.aurora * fade * AURORA_VEIL_OPACITY * envelope * envelope)
                .coerceIn(0f, 0.28f)
            if (veilAlpha > 0f) {
                drawLine(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to config.coolColor.copy(alpha = veilAlpha * 0.28f),
                        0.84f to config.coolColor.copy(alpha = veilAlpha * 0.55f),
                        1f to config.warmColor.copy(alpha = veilAlpha),
                        startY = topY,
                        endY = baseY,
                    ),
                    start = Offset(x, topY),
                    end = Offset(x, baseY),
                    strokeWidth = veilWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
        repeat(AURORA_STRAND_COUNT) { index ->
            val position = (index + 0.5f) / AURORA_STRAND_COUNT
            val x = startX + spread * position
            // Seeded height stays fixed; time-dependent sway and ripple animate only the tip.
            val variation = random01(index * 107 + layer * 311)
            val sway = sin(position * 17f + phase + layer) * width * 0.004f
            val ripple = sin(position * 31f - phase * 0.7f) * width * 0.002f
            val envelope = sin(position * PI.toFloat()).coerceAtLeast(0f)
            val base = Offset(x, edgeY(x) - height * 0.006f)
            val heightVariation = variation * AURORA_STRAND_HEIGHT_VARIATION
            val strandHeightFraction = AURORA_STRAND_BASE_HEIGHT + heightVariation
            val envelopeContribution = envelope * AURORA_STRAND_ENVELOPE_GAIN
            val envelopeGain = AURORA_STRAND_ENVELOPE_BASE + envelopeContribution
            val baseLength = height * strandHeightFraction
            val length = baseLength * envelopeGain
            val tip = Offset(x + sway + ripple, base.y - length)
            val alpha = auroraOpacity(config.aurora, fade, position, layer) *
                (0.45f + variation * 0.55f)
            if (alpha > 0f) {
                // A wide glow behind each narrow core softens the strand edges.
                val glow = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.5f to config.coolColor.copy(alpha = alpha * 0.08f),
                    0.82f to config.coolColor.copy(alpha = alpha * 0.23f),
                    1f to config.warmColor.copy(alpha = alpha * 0.5f),
                    start = tip,
                    end = base,
                )
                drawLine(
                    brush = glow,
                    start = tip,
                    end = base,
                    strokeWidth = width * 0.006f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.48f to config.coolColor.copy(alpha = alpha * 0.12f),
                        0.85f to config.coolColor.copy(alpha = alpha * 0.47f),
                        1f to config.warmColor.copy(alpha = alpha),
                        start = tip,
                        end = base,
                    ),
                    start = tip,
                    end = base,
                    strokeWidth = width * (0.0008f + variation * 0.001f),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

internal fun auroraTimeStep(
    deltaSeconds: Float,
    sceneSpeed: Float,
    auroraSpeed: Float,
): Float {
    // Scene speed and Aurora speed multiply, so either zero value pauses curtain motion.
    val sceneDelta = deltaSeconds * sceneSpeed
    val boundedAuroraSpeed = auroraSpeed.coerceAtLeast(0f)
    return sceneDelta * boundedAuroraSpeed
}

internal fun auroraPulseEnvelope(age: Float, duration: Float): Float = when {
    age !in 0f..<duration -> 0f
    // A squared rise starts gently, then the pulse holds before its linear fade.
    age < AURORA_RISE_DURATION -> {
        val progress = age / AURORA_RISE_DURATION
        progress * progress
    }
    age < duration - AURORA_FADE_DURATION -> 1f
    else -> (duration - age) / AURORA_FADE_DURATION
}

internal fun auroraOpacity(
    strength: Float,
    fade: Float,
    position: Float,
    layer: Int,
): Float {
    val boundedPosition = position.coerceIn(0f, 1f)
    val angle = boundedPosition * PI.toFloat()
    val envelope = sin(angle)
    val boundedStrength = strength.coerceIn(0f, 2f)
    val boundedFade = fade.coerceIn(0f, 1f)
    val strengthFade = boundedStrength * boundedFade
    // Two envelope weights suppress strand opacity at the footprint's left and right edges.
    val firstEnvelopeWeight = strengthFade * envelope
    val secondEnvelopeWeight = firstEnvelopeWeight * envelope
    val boundedLayer = layer.coerceIn(0, 2)
    val layerGain = boundedLayer * AURORA_OPACITY_LAYER_GAIN
    val layerWeight = AURORA_OPACITY_BASE + layerGain
    val opacity = secondEnvelopeWeight * layerWeight
    return opacity.coerceIn(0f, AURORA_MAX_OPACITY)
}
