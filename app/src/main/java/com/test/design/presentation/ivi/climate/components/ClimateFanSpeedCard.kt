package com.test.design.presentation.ivi.climate.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.test.design.presentation.ivi.climate.AirflowMode
import com.test.design.presentation.ivi.climate.fanSpeedFromTrackPosition
import com.test.design.presentation.ivi.climate.rememberClimateHaptics
import com.test.design.presentation.ivi.common.MorphingDetailSurfaceCard
import com.test.design.theme.ClimateCardActiveRadii
import com.test.design.theme.ClimateCardRestRadii

@Composable
fun ClimateFanSpeedCard(
    fanSpeed: Int,
    maxFanSpeed: Int,
    isAcEnabled: Boolean,
    onSpeedSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    MorphingDetailSurfaceCard(
        morphExpanded = isAcEnabled,
        compactRadii = ClimateCardRestRadii,
        expandedRadii = ClimateCardActiveRadii,
        modifier = modifier.fillMaxWidth(),
    ) {
        FanSpeedControl(
            fanSpeed = fanSpeed,
            maxFanSpeed = maxFanSpeed,
            onSpeedSelected = onSpeedSelected,
            compact = compact,
            singleRow = false,
        )
    }
}

@Composable
fun ClimateBlowerCard(
    fanSpeed: Int,
    maxFanSpeed: Int,
    isAcEnabled: Boolean,
    onSpeedSelected: (Int) -> Unit,
    showFan: Boolean,
    showAirflow: Boolean,
    airflowMode: AirflowMode,
    airflowModes: List<AirflowMode>,
    onAirflowSelected: (AirflowMode) -> Unit,
    sideBySide: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!showFan && !showAirflow) return
    val horizontal = sideBySide && showFan && showAirflow
    MorphingDetailSurfaceCard(
        morphExpanded = isAcEnabled,
        compactRadii = ClimateCardRestRadii,
        expandedRadii = ClimateCardActiveRadii,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (horizontal) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FanSpeedControl(
                    fanSpeed = fanSpeed,
                    maxFanSpeed = maxFanSpeed,
                    onSpeedSelected = onSpeedSelected,
                    compact = false,
                    singleRow = true,
                    modifier = Modifier.weight(1.15f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                )
                MorphingAirflowSegmentedButton(
                    selectedMode = airflowMode,
                    onModeSelected = onAirflowSelected,
                    modes = airflowModes,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showFan) {
                    FanSpeedControl(
                        fanSpeed = fanSpeed,
                        maxFanSpeed = maxFanSpeed,
                        onSpeedSelected = onSpeedSelected,
                        compact = false,
                        singleRow = false,
                    )
                }
                if (showAirflow) {
                    MorphingAirflowSegmentedButton(
                        selectedMode = airflowMode,
                        onModeSelected = onAirflowSelected,
                        modes = airflowModes,
                    )
                }
            }
        }
    }
}

@Composable
private fun FanSpeedControl(
    fanSpeed: Int,
    maxFanSpeed: Int,
    onSpeedSelected: (Int) -> Unit,
    compact: Boolean,
    singleRow: Boolean,
    modifier: Modifier = Modifier,
) {
    val steps = maxFanSpeed.coerceAtLeast(0)
    val speed = fanSpeed.coerceIn(0, steps)
    val readout = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        ) {
            SpinningFanIcon(
                fanSpeed = speed,
                maxFanSpeed = steps,
                size = if (compact) 28.dp else if (singleRow) 36.dp else 40.dp,
            )
            Text(
                text = if (speed <= 0) "Off" else speed.toString(),
                style = if (compact || singleRow) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = MaterialTheme.colorScheme.onSurface,
                modifier = if (singleRow) Modifier.width(72.dp) else Modifier,
            )
        }
    }
    val track = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
        ) {
            FanStepButton(
                icon = Icons.Default.Remove,
                contentDescription = "Decrease fan speed",
                onClick = { onSpeedSelected((speed - 1).coerceAtLeast(0)) },
                enabled = speed > 0,
                size = if (compact) 32.dp else 44.dp,
            )
            FanSpeedTrack(
                fanSpeed = speed,
                maxFanSpeed = steps,
                onSpeedSelected = onSpeedSelected,
                compact = compact,
                modifier = Modifier.weight(1f),
            )
            FanStepButton(
                icon = Icons.Default.Add,
                contentDescription = "Increase fan speed",
                onClick = { onSpeedSelected((speed + 1).coerceAtMost(steps)) },
                enabled = speed < steps,
                size = if (compact) 32.dp else 44.dp,
            )
        }
    }
    if (singleRow) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            readout()
            Box(modifier = Modifier.weight(1f)) { track() }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        ) {
            readout()
            track()
        }
    }
}

@Composable
private fun SpinningFanIcon(
    fanSpeed: Int,
    maxFanSpeed: Int,
    size: Dp,
) {
    val latestSpeed by rememberUpdatedState(fanSpeed)
    val latestMax by rememberUpdatedState(maxFanSpeed)
    var rotation by remember { mutableFloatStateOf(0f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrame = now
                val target = fanDegreesPerSecond(latestSpeed, latestMax)
                val blend = (dt * 2.4f).coerceIn(0f, 1f)
                val nextVelocity = velocity + (target - velocity) * blend
                val settled = if (latestSpeed <= 0 && abs(nextVelocity) < 6f) 0f else nextVelocity
                if (settled != velocity) velocity = settled
                if (settled != 0f) {
                    rotation = (rotation + settled * dt) % 360f
                }
            }
        }
    }
    val spinning = fanSpeed > 0 || abs(velocity) > 6f
    Icon(
        painter = painterResource(if (spinning) ClimateHvacIcons.Fan else ClimateHvacIcons.FanOff),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
    )
}

@Composable
private fun FanStepButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: Dp,
) {
    val haptics = rememberClimateHaptics()
    FilledIconButton(
        onClick = {
            haptics.tick()
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.size(size),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
private fun FanSpeedTrack(
    fanSpeed: Int,
    maxFanSpeed: Int,
    onSpeedSelected: (Int) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberClimateHaptics()
    val latestSelect by rememberUpdatedState(onSpeedSelected)
    var lastSent by remember { mutableIntStateOf(fanSpeed) }
    LaunchedEffect(fanSpeed) { lastSent = fanSpeed }
    val trackHeight = if (compact) 28.dp else 40.dp
    val thumbSize = if (compact) 22.dp else 32.dp
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val tickIdle = MaterialTheme.colorScheme.onSurfaceVariant
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .semantics {
                contentDescription = if (fanSpeed <= 0) {
                    "Fan off"
                } else {
                    "Fan speed $fanSpeed of $maxFanSpeed"
                }
            }
            .pointerInput(maxFanSpeed) {
                detectTapGestures { offset ->
                    val speed = fanSpeedFromTrackPosition(offset.x, size.width.toFloat(), maxFanSpeed)
                    if (speed == lastSent) return@detectTapGestures
                    lastSent = speed
                    haptics.tick()
                    latestSelect(speed)
                }
            }
            .pointerInput(maxFanSpeed) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val speed = fanSpeedFromTrackPosition(change.position.x, size.width.toFloat(), maxFanSpeed)
                    if (speed == lastSent) return@detectHorizontalDragGestures
                    lastSent = speed
                    haptics.tick()
                    latestSelect(speed)
                }
            },
    ) {
        val travel = (maxWidth - thumbSize).coerceAtLeast(0.dp)
        val fraction = if (maxFanSpeed <= 0) 0f else fanSpeed / maxFanSpeed.toFloat()
        val thumbOffset by animateDpAsState(
            targetValue = travel * fraction,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "fan_thumb",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(thumbOffset + thumbSize)
                .clip(CircleShape)
                .background(primary),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (maxFanSpeed <= 0) return@Canvas
            val inset = thumbSize.toPx() / 2f
            val span = (size.width - thumbSize.toPx()).coerceAtLeast(1f)
            for (index in 0..maxFanSpeed) {
                val x = inset + span * (index / maxFanSpeed.toFloat())
                drawCircle(
                    color = if (index <= fanSpeed) onPrimary.copy(alpha = 0.9f) else tickIdle.copy(alpha = 0.55f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(x, size.height / 2f),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(onPrimary)
                .border(2.dp, primary, CircleShape),
        )
    }
}

/** Degrees per second. Low blower steps turn slowly; the top step turns quickly. */
private fun fanDegreesPerSecond(fanSpeed: Int, maxFanSpeed: Int): Float {
    if (fanSpeed <= 0 || maxFanSpeed <= 0) return 0f
    val t = if (maxFanSpeed <= 1) 1f else (fanSpeed - 1).toFloat() / (maxFanSpeed - 1)
    val secondsPerTurn = 2.6f - t * 2.25f
    return 360f / secondsPerTurn
}
