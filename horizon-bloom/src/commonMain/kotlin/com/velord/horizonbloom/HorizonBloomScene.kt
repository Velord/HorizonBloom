package com.velord.horizonbloom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

internal const val EDGE_SEGMENTS = 120
private const val DEGREES_PER_HALF_TURN = 180f
private const val SPREAD_MASK_SEGMENT_COUNT = 120
private const val SPREAD_SKY_HALO_WIDTH_SCALE = 0.5f
private const val SPREAD_AIRGLOW_WIDTH_SCALE = 1f
private const val SPREAD_OUTER_ATMOSPHERE_WIDTH_SCALE = 0.8f
private const val INTRO_PROGRESS_RATE = 0.45f

internal fun DrawScope.drawHorizonScene(
    config: HorizonBloomConfig,
    frame: HorizonBloomRuntimeState,
) {
    if (size.width <= 0f || size.height <= 0f) return

    with(HorizonBloomScene(config, frame, size.width, size.height)) {
        drawScene()
    }
}

internal class HorizonBloomScene(
    val config: HorizonBloomConfig,
    val frame: HorizonBloomRuntimeState,
    val width: Float,
    val height: Float,
) {
    val horizonY = config.horizon.coerceIn(0f, 1f) * height
    val cameraShift = parallaxShift(
        width = width,
        pointerX = frame.pointer.x,
        strength = config.parallax,
        interactive = config.interactive,
    )
    val horizontalCenter = width * 0.5f
    val centerX = horizontalCenter + cameraShift
    val tiltRadians = config.tilt * PI.toFloat() / DEGREES_PER_HALF_TURN
    val tiltSlope = sin(tiltRadians)
    fun edgeY(x: Float): Float = horizonEdgeY(
        x = x,
        width = width,
        horizonY = horizonY,
        curvature = config.curvature,
        centerX = centerX,
        tiltSlope = tiltSlope,
    )
    val sunPositionX = sunX(
        width = width,
        position = config.sunPosition,
        drift = config.drift,
        time = frame.elapsed,
    )
    val horizonAtSun = edgeY(sunPositionX)
    val sunPositionY = sunY(horizonAtSun, config.sunHeight)
    val sunCenter = Offset(sunPositionX, sunPositionY)
    val bloomPower = config.bloom.coerceIn(0f, 2f)
    val bloomSize = bloomSize(width, bloomPower)
    val introElapsed = (frame.elapsed - frame.introStart).coerceAtLeast(0f)
    val intro = if (config.intro.not() ||
        (introElapsed == 0f && (config.paused || config.speed == 0f))) {
        1f
    } else {
        (introElapsed * INTRO_PROGRESS_RATE).coerceIn(0f, 1f)
    }
    val light = config.sunrise * intro
    val upperGlow = config.atmosphere.coerceIn(0f, 2f)
    val warmAtmosphereFactor = upperGlow.coerceAtMost(1f)
    val warmAtmosphereLight = light * warmAtmosphereFactor
    val sunRadius = sunDiscRadius(width, config.sunSize)

    val horizonPath = Path().apply {
        moveTo(0f, edgeY(0f))
        for (index in 1..EDGE_SEGMENTS) {
            val x = width * index / EDGE_SEGMENTS
            lineTo(x, edgeY(x))
        }
    }

    val planetPath = Path().apply {
        moveTo(0f, edgeY(0f))
        for (index in 1..EDGE_SEGMENTS) {
            val x = width * index / EDGE_SEGMENTS
            lineTo(x, edgeY(x))
        }
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }

    val spreadOuterAtmosphereMaskBrush: Brush
    val spreadSkyHaloMaskBrush: Brush
    val spreadRimMaskBrush: Brush
    val spreadAirglowMaskBrush: Brush

    init {
        val arcPositions = horizonArcPositions(
            width = width,
            curvature = config.curvature,
            centerX = centerX,
            tiltSlope = tiltSlope,
            segments = SPREAD_MASK_SEGMENT_COUNT,
        )
        val sourceArcPosition = horizonArcPositionAtX(sunPositionX, width, arcPositions)
        spreadOuterAtmosphereMaskBrush = createSpreadMaskBrush(
            arcPositions,
            sourceArcPosition,
            SPREAD_OUTER_ATMOSPHERE_WIDTH_SCALE,
        )
        spreadSkyHaloMaskBrush = createSpreadMaskBrush(
            arcPositions,
            sourceArcPosition,
            SPREAD_SKY_HALO_WIDTH_SCALE,
        )
        spreadRimMaskBrush = createSpreadMaskBrush(arcPositions, sourceArcPosition)
        spreadAirglowMaskBrush = createSpreadMaskBrush(
            arcPositions,
            sourceArcPosition,
            SPREAD_AIRGLOW_WIDTH_SCALE,
        )
    }

    private fun createSpreadMaskBrush(
        arcPositions: FloatArray,
        sourceArcPosition: Float,
        widthScale: Float = 1f,
    ): Brush {
        val stops = Array(SPREAD_MASK_SEGMENT_COUNT + 1) { index ->
            val fraction = index / SPREAD_MASK_SEGMENT_COUNT.toFloat()
            val arcDistance = arcPositions[index] - sourceArcPosition
            val falloff = horizonSpreadFalloff(
                arcDistance,
                height,
                config.spread,
                widthScale,
            )
            fraction to Color.White.copy(alpha = falloff)
        }

        return Brush.horizontalGradient(*stops, startX = 0f, endX = width)
    }

    fun DrawScope.drawScene() {
        val scene = this@HorizonBloomScene
        drawBackground(scene)
        drawStars(scene)
        drawAurora(scene)
        drawOuterBloom(scene)
        drawAtmosphere(scene)
        drawAirglow(scene)
        drawPlanet(scene)
        drawClouds(scene)
        drawSunDisc(scene)
        drawInnerBloom(scene)
        drawSunburst(scene)
        drawRim(scene)
        drawFlare(scene)
        drawGrain(scene)
    }

    fun DrawScope.drawWithSpreadMask(
        maskBrush: Brush,
        content: DrawScope.() -> Unit,
    ) {
        val canvas = drawContext.canvas
        canvas.saveLayer(Rect(0f, 0f, width, height), Paint())
        try {
            content()
            drawRect(brush = maskBrush, blendMode = BlendMode.DstIn)
        } finally {
            canvas.restore()
        }
    }
}

internal fun random01(seed: Int): Float {
    val value = sin(seed * 12.9898).toFloat() * 43758.5453f
    return value - kotlin.math.floor(value)
}
