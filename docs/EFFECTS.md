# Horizon Bloom Effects

Horizon Bloom is an original Compose scene: a rising sun behind a dark planet,
with light following its curved horizon. The [React Bits Horizon Bloom
reference](https://pro.reactbits.dev/docs/components/horizon-bloom) informs the
meaning of the controls, not a pixel-perfect rendering target. Keep the sun,
atmosphere, edge light, and secondary effects visually distinct.

## Library Overview

`:horizon-bloom` is a Kotlin Multiplatform library module containing shared
Compose rendering for Android, Desktop/JVM, and iOS. `HorizonBloom` is the public
scene composable; it accepts a `HorizonBloomConfig` and fills the bounds given
by its caller. The configuration exposes the Dawn, Twilight, and Aurora
presets, colors, geometry, animation, and effect strengths. The individual
renderers are internal to the library. `HorizonDemoScreen` lives in the separate
`:demo` module; it is an editor and preview, not another renderer.

The Kotlin source root is
`horizon-bloom/src/commonMain/kotlin/com/velord/horizonbloom/`. The renderer has
no platform-specific source set; the same scene code serves all three targets.
The demo code is under `demo/src/commonMain/kotlin/com/velord/horizonbloom/demo/`.

## File Structure

| File | Responsibility |
| --- | --- |
| `horizon-bloom/build.gradle.kts`, `demo/build.gradle.kts` | KMP library and demo setup. |
| `HorizonBloom.kt`, `HorizonBloomConfig.kt` | Public composable, configuration and presets, frame clock, pointer input, preview. |
| `HorizonBloomScene.kt` | Shared frame geometry, Spread masks, and compositing order. |
| `HorizonGeometry.kt` | Curved horizon, sun position, parallax, arc distance, and Spread falloff. |
| `HorizonBackground.kt`, `HorizonStars.kt`, `HorizonAurora.kt`, `HorizonPlanet.kt`, `HorizonClouds.kt`, `HorizonGrain.kt` | Sky, foreground, environmental, and texture renderers. |
| `HorizonBloomGlow.kt`, `HorizonAtmosphere.kt`, `HorizonAirglow.kt`, `HorizonSunDisc.kt`, `HorizonSunburst.kt`, `HorizonRim.kt`, `HorizonFlare.kt` | Source, atmospheric, and edge-light renderers. |
| `HorizonDemoScreen.kt`, `HorizonControlSection.kt`, `HorizonColorSection.kt` | Demo preview, presets, controls, colors, and options. |
| `HorizonDemoRange.kt` | Numeric control ranges. |
| `HorizonColorHex.kt` | RGB hex parsing and formatting. |
| `HorizonDemoVM.kt`, `HorizonDemoUiState.kt`, `HorizonDemoUiAction.kt` | Demo state, actions, preset selection, and configuration updates. |
| `HorizonDemoHeader.kt`, platform actuals, `Main.kt` | Native demo header behavior and Desktop launcher. |

## Scene Geometry

`HorizonBloomConfig` holds the scene controls and Dawn, Twilight, and Aurora
presets. `horizon` places the edge; `curvature` shapes its circular arc;
`tilt` inclines it. `sunPosition`, `sunHeight`, and `sunSize` place and size
the source independently. `drift` moves the sun and clouds; drag-driven
`parallax` shifts the horizon and stars when `interactive` is enabled.

`sunrise * intro` supplies source-dependent light. `intro` ramps that light
in; `paused` and `speed` control scene time. `auroraSpeed` controls aurora
time separately. A tap can trigger a temporary aurora when interaction is on.

| Preset | Light and color character |
| --- | --- |
| Dawn | Pink source, violet edge; default `spread = 0.4`, `sunburst = 0.25`, `airglow = 0.5`. |
| Twilight | Warmer amber source, blue edge, lower Sunrise, stronger Bloom. |
| Aurora | Teal source, blue edge, dimmer Sunrise, stronger Atmosphere and automatic Aurora. |

## Rendered Effects

The entries below follow compositing order. `HorizonBloomScene.kt` owns their
shared geometry and draw order; the corresponding `Horizon*.kt` files own the
individual renderers.

### Background

`HorizonBackground.kt` fills the sky with the preset background color and a
faint lower-sky gradient. Cool color follows Atmosphere; warm color follows
Sunrise. It provides depth without becoming a bright, independent horizon
band.

### Stars

`HorizonStars.kt` draws stable pseudo-random points only above the horizon.
They twinkle with scene time and shift with parallax. `stars` changes their
alpha, not the number of generated points; Sunrise does not control them.

### Aurora

`HorizonAurora.kt` draws translucent veils and fine vertical strands rising
from the horizon. Automatic pulses occur when `autoAurora` is enabled;
interactive taps start a pulse at the selected position. `aurora` controls
visibility, `auroraSpeed` controls its clock, and `drift` affects strand
motion. It is not horizon lighting and does not use Spread.

### Bloom

`HorizonBloomGlow.kt` draws a broad outer radial glow before the foreground
and a tighter inner glow near the source after the sun disc. Warm energy
follows Sunrise; the outer cool component follows Atmosphere. `bloom = 0`
removes both Bloom layers. Bloom is diffuse light, not Flare's concentrated
white point and not Sunburst's rays.

### Atmosphere

`HorizonAtmosphere.kt` gives the sky-facing edge a diffuse cool haze and an
edge-adjacent halo. Both follow `atmosphere` and are limited along the
horizon by Spread. A separate warm glow stays centered on the sun and
follows Sunrise; it is not Spread-masked. The sky-side light is clipped
against the planet silhouette.

`thickness` changes the halo's height and the outer haze's shape. It does
not set how far light travels along the curved edge. At `atmosphere = 0`,
these haze layers disappear, while Rim, Airglow, and the sun remain under
their own controls. The halo should fade smoothly, without a hard parallel
arc or a broad wash over the upper sky.

### Airglow

`HorizonAirglow.kt` adds a faint lavender glow close to the curved edge,
with a small source-local sky glow. Its main density grows rapidly at low
`airglow` values using `1 - exp(-3 * airglow)`. A separate upper band begins
only above `airglow = 1.4`; ordinary/default Airglow should not show a
detached upper stripe. `airglow = 0` removes this renderer's light.

Airglow remains under the curved-arc Spread mask across the full Spread
range. Its own slider controls energy; Spread controls horizontal reach.
Airglow does not replace Rim's thin outline or Atmosphere's broad haze.

### Planet

`HorizonPlanet.kt` draws the dark foreground silhouette over sky-side
light. Its base is near-black. A subtle tint just below the edge responds
to Atmosphere and source light; there is no permanently bright purple
strip embedded in the planet fill. The silhouette occludes the sun and
other sky layers where appropriate.

### Clouds

`HorizonClouds.kt` draws short, irregular, low-opacity wisps at varying
depths just below the edge, clipped to the planet. They drift gently and
are more visible near the sun; a very faint flattened haze sits near the
source. `clouds = 0` removes both wisps and haze. The effect must read as
subtle surface texture, not repeated full-width arcs or round glowing spots.

### Sun Disc

`HorizonSunDisc.kt` draws the visible source above the silhouette as a
soft white-to-warm glow, clipped to the sky. Its radius follows `sunSize`,
while its location follows the sun geometry. Sunrise and Intro govern its
light. Flare owns the stronger concentrated point, so disabling Flare
does not remove the Sun Disc.

### Sunburst

`HorizonSunburst.kt` draws optional radial rays from the visible sun,
clipped to the sky. `sunburst` controls their intensity; ray count is a
separate discrete `4..16` control with default `8`. Rays fade as the sun
is occluded and with Sunrise. Dawn's default intensity is `0.25`, keeping
the rays secondary to the source and edge lighting. Flare never owns rays.

### Rim

`HorizonRim.kt` draws the thin white/warm/cool outline of the planet and
soft layers immediately around it. `rim` scales the assembled light as one
layer across its `0..2` range; Spread masks its reach along the arc from
the sun. `rim = 0` removes the Rim renderer, not the independent Airglow
or Atmosphere light.

### Flare

`HorizonFlare.kt` owns the concentrated white/warm hotspot, nearby halo,
and faint wider glare veil. Hotspot and halo are clipped to the sky; the
subtle veil is composited after planet occlusion. Their radii scale with
the sun, not Bloom's broad radius. Flare strength follows `flare`,
Sunrise/Intro light, and the visible fraction of the sun disc.

At `flare = 0`, hotspot and veil disappear while Sun Disc, Bloom, Rim,
and Atmosphere keep their own contributions. Flare has no directional
streaks or Sunburst rays.

### Grain

`HorizonGrain.kt` overlays fine animated noise on the whole scene,
including the dark planet. Its texture changes at a reduced frame rate;
the planet receives a quieter version of the same texture. `grain = 0`
removes it, while higher values must not obscure the source, edge, or
aurora.

## Shared Reach: Spread

Spread is an adjustment, not a renderer. The UI range is `0..2`; Dawn
starts at `0.4`. `HorizonGeometry.kt` measures distance along the actual
curved and tilted horizon from the sun's position. A smooth, sun-centered
falloff widens as Spread increases. Its width remains nonzero at zero,
so nearby light stays visible even when the distant edge is dark.

The same arc falloff is used with different widths for the outer
Atmosphere, sky halo, Rim, and Airglow. Airglow remains masked even above
the Dawn default. Spread changes their horizontal reach, not their
individual intensity, vertical height, or the localized sun source.
Thickness shapes atmospheric height; Atmosphere, Rim, and Airglow each
retain their own intensity controls. Bloom, Flare, Sunburst, Clouds,
Stars, and Aurora do not become Spread effects.

The mask samples the horizon at 120 segments. Visibility falls as
`exp(-0.5 * normalizedArcDistance^3)`, with a nonzero local width of
`0.06 * viewportHeight` before each layer's scale. Width grows with the
slider; above Dawn's `0.4`, its growth rate is halved.

| Spread-masked layer | Width scale |
| --- | ---: |
| Sky halo | `0.5` |
| Rim | Unscaled base falloff |
| Outer Atmosphere | `0.8` |
| Airglow | `1.0` |
