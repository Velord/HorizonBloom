package com.velord.horizonbloom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath

/*
 * Last light pass: hotspot and halo sit in sky, while a faint veil crosses foreground.
 * `flare`, Sunrise/Intro, and sun visibility set energy; `sunSize` scales every radius.
 * Drawn after Rim and before Grain as the scene moves and reveals the sun.
 */
private const val FLARE_INTENSITY_GAIN = 1.35f
private const val FLARE_HOTSPOT_RADIUS_FACTOR = 0.95f
private const val FLARE_HALO_BASE_RADIUS_FACTOR = 2.7f
private const val FLARE_HALO_GAIN_RADIUS_FACTOR = 1.8f
private const val FLARE_VEIL_BASE_RADIUS_FACTOR = 4f
private const val FLARE_VEIL_GAIN_RADIUS_FACTOR = 3f
private const val FLARE_VEIL_VERTICAL_OFFSET_FACTOR = 0.2f

internal fun DrawScope.drawFlare(scene: HorizonBloomScene) = with(scene) {
    val visibleSun = flareVisibility(horizonAtSun, sunCenter.y, sunRadius)
    val flareEnergy = flareIntensity(config.flare, light, visibleSun)
    // The bright source stays behind the planet; only its faint veil crosses foreground.
    clipPath(planetPath, clipOp = ClipOp.Difference) {
        drawHorizonFlareHotspot(
            config = config,
            sun = sunCenter,
            sunRadius = sunRadius,
            intensity = flareEnergy,
        )
    }
    drawHorizonFlareVeil(
        config = config,
        sun = sunCenter,
        sunRadius = sunRadius,
        intensity = flareEnergy,
    )
}

internal fun flareVisibility(horizonYAtSun: Float, sunY: Float, sunRadius: Float): Float {
    if (sunRadius <= 0f) return 0f

    // Ease the sun's edge crossing so the hotspot fades smoothly under occlusion.
    // Center one radius below the rim maps to zero; one radius above maps to one.
    val progress = ((horizonYAtSun - sunY) / sunRadius + 1f) * 0.5f
    val visible = progress.coerceIn(0f, 1f)
    return visible * visible * (3f - 2f * visible)
}

internal fun flareIntensity(flare: Float, light: Float, visibility: Float): Float {
    // Flare slider cannot create light before sunrise or while the sun remains hidden.
    val source = flare.coerceIn(0f, 2f) * light.coerceIn(0f, 2f)
    return (source * visibility.coerceIn(0f, 1f) * FLARE_INTENSITY_GAIN).coerceIn(0f, 1f)
}

internal fun DrawScope.drawHorizonFlareHotspot(
    config: HorizonBloomConfig,
    sun: Offset,
    sunRadius: Float,
    intensity: Float,
) {
    if (intensity <= 0f || sunRadius <= 0f) return

    // Small white core and broader warm/cool halo add light without hard circle edges.
    val coreRadius = sunRadius * FLARE_HOTSPOT_RADIUS_FACTOR
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = intensity * 0.95f),
            0.16f to Color.White.copy(alpha = intensity * 0.86f),
            0.42f to config.warmColor.copy(alpha = intensity * 0.43f),
            1f to Color.Transparent,
            center = sun,
            radius = coreRadius,
        ),
        center = sun,
        radius = coreRadius,
        blendMode = BlendMode.Plus,
    )

    // Halo grows as intensity rises, so a stronger flare affects more sky as well as brightness.
    val haloRadius = sunRadius * (
        FLARE_HALO_BASE_RADIUS_FACTOR + intensity * FLARE_HALO_GAIN_RADIUS_FACTOR
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to config.warmColor.copy(alpha = intensity * 0.26f),
            0.26f to config.warmColor.copy(alpha = intensity * 0.15f),
            0.58f to config.coolColor.copy(alpha = intensity * 0.035f),
            1f to Color.Transparent,
            center = sun,
            radius = haloRadius,
        ),
        center = sun,
        radius = haloRadius,
        blendMode = BlendMode.Plus,
    )
}

internal fun DrawScope.drawHorizonFlareVeil(
    config: HorizonBloomConfig,
    sun: Offset,
    sunRadius: Float,
    intensity: Float,
) {
    if (intensity <= 0f || sunRadius <= 0f) return

    // Lower the unmasked veil toward the horizon, where it can tint the planet surface.
    val center = Offset(sun.x, sun.y + sunRadius * FLARE_VEIL_VERTICAL_OFFSET_FACTOR)
    val veilRadius = sunRadius * (
        FLARE_VEIL_BASE_RADIUS_FACTOR + intensity * FLARE_VEIL_GAIN_RADIUS_FACTOR
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to config.warmColor.copy(alpha = intensity * 0.10f),
            0.2f to config.warmColor.copy(alpha = intensity * 0.055f),
            0.65f to config.coolColor.copy(alpha = intensity * 0.018f),
            1f to Color.Transparent,
            center = center,
            radius = veilRadius,
        ),
        center = center,
        radius = veilRadius,
        blendMode = BlendMode.Plus,
    )
}
