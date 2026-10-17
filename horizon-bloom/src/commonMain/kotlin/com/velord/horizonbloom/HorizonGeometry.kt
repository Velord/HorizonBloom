package com.velord.horizonbloom

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val MAX_CURVATURE = 2f
private const val CURVATURE_SAGITTA_WIDTH_FACTOR = 0.12f
private const val SUN_DRIFT_PHASE_RATE = 0.16f
private const val SUN_DRIFT_WIDTH_FACTOR = 0.025f
private const val POINTER_MAX_OFFSET = 0.5f
private const val MAX_PARALLAX_STRENGTH = 2f
private const val PARALLAX_SHIFT_WIDTH_FACTOR = 0.16f
private const val STAR_PARALLAX_SHIFT_FACTOR = 0.3f
private const val SUN_DISC_RADIUS_WIDTH_FACTOR = 0.026f
private const val SPREAD_LOCAL_WIDTH_HEIGHT_FACTOR = 0.06f
private const val SPREAD_WIDTH_GAIN = 1.25f
private const val SPREAD_AFTER_DEFAULT_WIDTH_GAIN = 0.5f
private const val GLOW_CIRCLE_FLATNESS_FRACTION = 0.0001f

internal data class HorizonGlowCircle(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
)

internal fun horizonEdgeY(
    x: Float,
    width: Float,
    horizonY: Float,
    curvature: Float,
    centerX: Float,
    tiltSlope: Float,
): Float {
    if (width <= 0f) return horizonY

    val distance = x - centerX
    val tilt = distance * tiltSlope
    val boundedCurvature = curvature.coerceIn(0f, MAX_CURVATURE)
    val sagittaWidth = boundedCurvature * width
    val sagitta = sagittaWidth * CURVATURE_SAGITTA_WIDTH_FACTOR
    if (sagitta == 0f) return horizonY + tilt

    val span = max(centerX, width - centerX)
    val spanSquared = span * span
    val sagittaSquared = sagitta * sagitta
    val radiusNumerator = spanSquared + sagittaSquared
    val radiusDenominator = 2f * sagitta
    val radius = radiusNumerator / radiusDenominator
    val radiusSquared = radius * radius
    val distanceSquared = distance * distance
    val radicand = radiusSquared - distanceSquared
    val nonNegativeRadicand = radicand.coerceAtLeast(0f)
    val circleDrop = sqrt(nonNegativeRadicand)
    val circleHeight = radius - circleDrop
    val curvedY = horizonY + circleHeight
    return curvedY + tilt
}

internal fun horizonGlowCircle(
    width: Float,
    centerX: Float,
    horizonY: Float,
    leftEdgeY: Float,
    rightEdgeY: Float,
): HorizonGlowCircle? {
    if (width <= 0f) return null

    // Fit the circle through the left, center, and right points of the horizon.
    val leftDx = -centerX
    val rightDx = width - centerX
    val leftDy = leftEdgeY - horizonY
    val rightDy = rightEdgeY - horizonY
    val leftDistanceSquared = leftDx * leftDx + leftDy * leftDy
    val rightDistanceSquared = rightDx * rightDx + rightDy * rightDy
    val determinant = leftDx * rightDy - rightDx * leftDy
    val flatnessThreshold = width * width * GLOW_CIRCLE_FLATNESS_FRACTION
    if (abs(determinant) <= flatnessThreshold) return null

    val centerOffsetX =
        (leftDistanceSquared * rightDy - rightDistanceSquared * leftDy) / (2f * determinant)
    val centerOffsetY =
        (leftDx * rightDistanceSquared - rightDx * leftDistanceSquared) / (2f * determinant)
    val radius = sqrt(centerOffsetX * centerOffsetX + centerOffsetY * centerOffsetY)
    return HorizonGlowCircle(
        centerX = centerX + centerOffsetX,
        centerY = horizonY + centerOffsetY,
        radius = radius,
    )
}

internal fun sunX(
    width: Float,
    position: Float,
    drift: Float,
    time: Float,
): Float {
    val normalized = position.coerceIn(-1f, 1f)
    val phase = time * SUN_DRIFT_PHASE_RATE
    val oscillation = sin(phase)
    val distanceFromCenter = abs(normalized)
    val edgeFalloff = 1f - distanceFromCenter
    val driftOscillation = oscillation * drift
    val scaledDrift = driftOscillation * SUN_DRIFT_WIDTH_FACTOR
    val motion = scaledDrift * edgeFalloff
    val shiftedPosition = normalized + 1f
    val baseFraction = shiftedPosition * 0.5f
    val positionFraction = baseFraction + motion
    val x = width * positionFraction
    return x.coerceIn(0f, width)
}

internal fun sunY(horizonY: Float, height: Float): Float {
    val boundedHeight = height.coerceIn(0f, 1f)
    val remainingHeight = 1f - boundedHeight
    return horizonY * remainingHeight
}

internal fun parallaxShift(
    width: Float,
    pointerX: Float,
    strength: Float,
    interactive: Boolean,
): Float {
    if (interactive.not()) return 0f

    val boundedPointer = pointerX.coerceIn(-POINTER_MAX_OFFSET, POINTER_MAX_OFFSET)
    val boundedStrength = strength.coerceIn(0f, MAX_PARALLAX_STRENGTH)
    val pointerStrength = boundedPointer * boundedStrength
    val shiftWidth = pointerStrength * width
    return shiftWidth * PARALLAX_SHIFT_WIDTH_FACTOR
}

internal fun starParallaxX(x: Float, shift: Float, width: Float): Float {
    if (width <= 0f) return x

    val parallaxOffset = shift * STAR_PARALLAX_SHIFT_FACTOR
    val shiftedX = x - parallaxOffset
    val remainder = shiftedX % width
    val positiveRemainder = remainder + width
    return positiveRemainder % width
}

internal fun horizonArcPositions(
    width: Float,
    curvature: Float,
    centerX: Float,
    tiltSlope: Float,
    segments: Int,
): FloatArray {
    val segmentCount = segments.coerceAtLeast(0)
    val positions = FloatArray(segmentCount + 1)
    if (width <= 0f || segmentCount == 0) return positions

    val segmentWidth = width / segmentCount
    var previousY = horizonEdgeY(0f, width, 0f, curvature, centerX, tiltSlope)
    for (index in 1..segmentCount) {
        val x = segmentWidth * index
        val y = horizonEdgeY(x, width, 0f, curvature, centerX, tiltSlope)
        val verticalChange = y - previousY
        val segmentLength = sqrt(segmentWidth * segmentWidth + verticalChange * verticalChange)
        positions[index] = positions[index - 1] + segmentLength
        previousY = y
    }

    return positions
}

internal fun horizonArcPositionAtX(x: Float, width: Float, positions: FloatArray): Float {
    if (width <= 0f || positions.size < 2 || x.isFinite().not()) return 0f

    val scaledIndex = x.coerceIn(0f, width) / width * positions.lastIndex
    val leftIndex = scaledIndex.toInt().coerceAtMost(positions.lastIndex - 1)
    val fraction = scaledIndex - leftIndex
    val leftPosition = positions[leftIndex]
    val rightPosition = positions[leftIndex + 1]
    return leftPosition + (rightPosition - leftPosition) * fraction
}

internal fun horizonSpreadFalloff(
    arcDistance: Float,
    viewportHeight: Float,
    spread: Float,
    widthScale: Float,
): Float {
    if (arcDistance.isFinite().not() ||
        viewportHeight.isFinite().not() ||
        spread.isFinite().not() ||
        widthScale.isFinite().not() ||
        viewportHeight <= 0f ||
        widthScale <= 0f) return 0f

    val boundedSpread = spread.coerceIn(0f, 3f)
    val defaultSpread = HorizonBloomConfig.DAWN.spread
    val spreadBelowDefault = boundedSpread.coerceAtMost(defaultSpread)
    val spreadAboveDefault = (boundedSpread - defaultSpread).coerceAtLeast(0f)
    val highSpreadContribution = spreadAboveDefault * SPREAD_AFTER_DEFAULT_WIDTH_GAIN
    val effectiveSpread = spreadBelowDefault + highSpreadContribution
    val spreadWidth = effectiveSpread * SPREAD_WIDTH_GAIN
    val widthHeightFactor = SPREAD_LOCAL_WIDTH_HEIGHT_FACTOR + spreadWidth
    val falloffWidth = viewportHeight * widthHeightFactor * widthScale
    val normalizedDistance = abs(arcDistance) / falloffWidth
    val squaredDistance = normalizedDistance * normalizedDistance
    val cubedDistance = squaredDistance * normalizedDistance
    val exponent = -0.5f * cubedDistance
    return exp(exponent)
}

internal fun sunDiscRadius(width: Float, sunSize: Float): Float {
    val baseRadius = width * SUN_DISC_RADIUS_WIDTH_FACTOR
    val boundedSunSize = sunSize.coerceIn(0f, 2f)
    return baseRadius * boundedSunSize
}
