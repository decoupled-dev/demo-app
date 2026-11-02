package com.test.design.presentation.ivi.climate.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.climate.CabinWeatherPhysics
import com.test.design.presentation.ivi.climate.rememberClimateHaptics
import com.test.design.theme.CarDesignTokens
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Fan as a single material that thickens — Control Center volume, not five bars.
 */
@Composable
fun ClimateFanTrack(
    fanSpeed: Int,
    maxFanSpeed: Int,
    onSpeedSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    powerOn: Boolean = true,
) {
    val haptics = rememberClimateHaptics()
    val max = maxFanSpeed.coerceAtLeast(1)
    val fill by animateFloatAsState(
        targetValue = (fanSpeed / max.toFloat()).coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "fan_fill",
    )
    val hz = CabinWeatherPhysics.breathHertz(fanSpeed, max, powerOn)
    val amp = CabinWeatherPhysics.breathAmplitude(fanSpeed, max, powerOn)
    val clock = rememberInfiniteTransition(label = "fan_breath")
    val t by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fan_breath_clock",
    )
    val inhale = 1f + sin(t * 2f * PI.toFloat() * hz * 18f) * amp * 0.35f
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val material = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val density = LocalDensity.current
    val latestSpeed = rememberUpdatedState(fanSpeed)
    val latestSelect = rememberUpdatedState(onSpeedSelected)

    fun speedAt(x: Float, widthPx: Float): Int {
        if (widthPx <= 0f) return 0
        return (x / widthPx * max).roundToInt().coerceIn(0, max)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(CarDesignTokens.MinTouchTarget)
            .clip(RoundedCornerShape(100.dp))
            .background(track)
            .semantics {
                contentDescription = if (fanSpeed <= 0) "Fan off" else "Fan $fanSpeed of $max"
            }
            .pointerInput(max) {
                detectTapGestures { offset ->
                    val next = speedAt(offset.x, size.width.toFloat())
                    if (next != latestSpeed.value) haptics.tick()
                    latestSelect.value(next)
                }
            }
            .pointerInput(max) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val next = speedAt(change.position.x, size.width.toFloat())
                    if (next != latestSpeed.value) {
                        haptics.tick()
                        latestSelect.value(next)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (widthPx * fill).toDp() })
                .graphicsLayer {
                    scaleY = inhale
                    alpha = (0.85f + (inhale - 1f) * 2f).coerceIn(0.7f, 1f)
                }
                .clip(RoundedCornerShape(100.dp))
                .background(material),
        )
    }
}
