package com.velord.horizonbloom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

private const val SECONDS_PER_NANO = 0.000000001f
private const val MAX_FRAME_DELTA_SECONDS = 0.05f

internal data class HorizonBloomRuntimeState(
    val elapsed: Float,
    val auroraElapsed: Float,
    val introStart: Float,
    val lastBloomTime: Float,
    val bloomLocation: Float,
    val pointer: Offset,
)

@Composable
fun HorizonBloom(
    config: HorizonBloomConfig,
    modifier: Modifier = Modifier,
) {
    val state = remember {
        mutableStateOf(
            HorizonBloomRuntimeState(
                elapsed = 0f,
                auroraElapsed = 0f,
                introStart = 0f,
                lastBloomTime = -AURORA_DURATION,
                bloomLocation = 0.5f,
                pointer = Offset.Zero,
            ),
        )
    }

    HorizonBloomMotion(
        config = config,
        state = state,
    )
    HorizonBloomCanvas(
        config = config,
        state = state,
        modifier = modifier,
    )
}

@Composable
private fun HorizonBloomMotion(
    config: HorizonBloomConfig,
    state: MutableState<HorizonBloomRuntimeState>,
) {
    LaunchedEffect(config.intro) {
        if (config.intro) state.value = state.value.copy(introStart = state.value.elapsed)
    }

    LaunchedEffect(config.paused, config.speed, config.auroraSpeed) {
        if (config.paused || config.speed <= 0f) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val frameNanos = now - previous
                val elapsedSeconds = frameNanos * SECONDS_PER_NANO
                val delta = elapsedSeconds.coerceIn(0f, MAX_FRAME_DELTA_SECONDS)
                val sceneDelta = delta * config.speed
                val current = state.value
                val auroraDelta = auroraTimeStep(
                    deltaSeconds = delta,
                    sceneSpeed = config.speed,
                    auroraSpeed = config.auroraSpeed,
                )
                state.value = current.copy(
                    elapsed = current.elapsed + sceneDelta,
                    auroraElapsed = current.auroraElapsed + auroraDelta,
                )
                previous = now
            }
        }
    }
}

@Composable
private fun HorizonBloomCanvas(
    config: HorizonBloomConfig,
    state: MutableState<HorizonBloomRuntimeState>,
    modifier: Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(config.interactive) {
                if (config.interactive.not()) return@pointerInput
                detectTapGestures { offset ->
                    val current = state.value
                    state.value = current.copy(
                        bloomLocation = (offset.x / size.width).coerceIn(0f, 1f),
                        lastBloomTime = current.auroraElapsed,
                    )
                }
            }
            .pointerInput(config.interactive) {
                if (config.interactive.not()) return@pointerInput
                detectDragGestures { change, _ ->
                    state.value = state.value.copy(
                        pointer = Offset(
                            x = change.position.x / size.width - 0.5f,
                            y = change.position.y / size.height - 0.5f,
                        ),
                    )
                }
            },
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        drawHorizonScene(config, state.value)
    }
}

@Preview
@Composable
private fun Preview() {
    HorizonBloom(
        config = HorizonBloomConfig.DAWN.copy(paused = true, intro = false),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
    )
}
