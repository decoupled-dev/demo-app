package com.test.design.presentation.ivi.climate.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.climate.TemperatureUnit
import com.test.design.presentation.ivi.climate.temperatureScaleLabelFits
import com.test.design.presentation.ivi.climate.rememberClimateHaptics
import com.test.design.presentation.ivi.climate.temperatureScaleDragSteps
import com.test.design.presentation.ivi.climate.temperatureScaleEdgeFade
import com.test.design.presentation.ivi.climate.temperatureScaleIndex
import com.test.design.presentation.ivi.climate.temperatureScaleIsMajorTick
import com.test.design.presentation.ivi.climate.temperatureScaleSegmentStep
import com.test.design.presentation.ivi.climate.temperatureScaleSideBrightness
import com.test.design.presentation.ivi.climate.temperatureScaleTickHeightFraction
import com.test.design.presentation.ivi.climate.toDisplayTemperature
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private val TickSpacing = 12.dp
private val MaxTickLength = 22.dp

private fun roundedTriangle(
    apex: Offset,
    bottomLeft: Offset,
    bottomRight: Offset,
    cornerRadius: Float,
): Path {
    fun along(from: Offset, to: Offset, distance: Float): Offset {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = hypot(dx, dy).coerceAtLeast(1f)
        val t = (distance / length).coerceIn(0f, 0.45f)
        return Offset(from.x + dx * t, from.y + dy * t)
    }
    val apexToLeft = along(apex, bottomLeft, cornerRadius)
    val leftFromApex = along(bottomLeft, apex, cornerRadius)
    val leftToRight = along(bottomLeft, bottomRight, cornerRadius)
    val rightFromLeft = along(bottomRight, bottomLeft, cornerRadius)
    val rightToApex = along(bottomRight, apex, cornerRadius)
    val apexFromRight = along(apex, bottomRight, cornerRadius)
    return Path().apply {
        moveTo(apexToLeft.x, apexToLeft.y)
        lineTo(leftFromApex.x, leftFromApex.y)
        quadraticBezierTo(bottomLeft.x, bottomLeft.y, leftToRight.x, leftToRight.y)
        lineTo(rightFromLeft.x, rightFromLeft.y)
        quadraticBezierTo(bottomRight.x, bottomRight.y, rightToApex.x, rightToApex.y)
        lineTo(apexFromRight.x, apexFromRight.y)
        quadraticBezierTo(apex.x, apex.y, apexToLeft.x, apexToLeft.y)
        close()
    }
}

/**
 * Fixed needle, sliding scale. Drag follows the finger: the value under the
 * needle is the set-point. Cooler degrees sit to the left, warmer to the right.
 */
@Composable
fun TemperatureScale(
    temperature: Float,
    temperatureLabel: String,
    sortKey: Float,
    minTemperature: Float,
    maxTemperature: Float,
    temperatureStepCelsius: Float,
    temperatureStepFahrenheit: Float,
    minTemperatureFahrenheit: Float?,
    temperatureUnit: TemperatureUnit,
    onTemperatureSteps: (Int) -> Unit,
    modifier: Modifier = Modifier,
    canvasHeight: Dp = 100.dp,
    readoutFontSize: TextUnit = 44.sp,
    readoutLineHeight: TextUnit = 52.sp,
) {
    val step = temperatureScaleSegmentStep(temperatureUnit, temperatureStepCelsius)
    val tickCount = temperatureScaleIndex(maxTemperature, minTemperature, step).coerceAtLeast(0)
    val selectedIndex = temperatureScaleIndex(temperature, minTemperature, step)
        .coerceIn(0, tickCount)
    val density = LocalDensity.current
    val spacingPx = with(density) { TickSpacing.toPx() }
    var residual by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val haptics = rememberClimateHaptics()
    val latestHaptics by rememberUpdatedState(haptics)
    val latestSteps by rememberUpdatedState(onTemperatureSteps)
    val latestIndex by rememberUpdatedState(selectedIndex)
    val latestTickCount by rememberUpdatedState(tickCount)
    val releaseSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val motionSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (dragging) snap() else motionSpec,
        label = "temperature_scale_index",
    )
    val shownShift by animateFloatAsState(
        targetValue = if (dragging) residual else 0f,
        animationSpec = if (dragging) snap() else releaseSpec,
        label = "temperature_scale_shift",
    )
    val tickColor = MaterialTheme.colorScheme.primary
    val labelFont = (canvasHeight.value * 0.2f).coerceIn(14f, 20f).sp
    val labelStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = labelFont,
        lineHeight = (labelFont.value * 1.2f).sp,
    )
    val readoutStyle = MaterialTheme.typography.displayMedium.copy(
        fontSize = readoutFontSize,
        lineHeight = readoutLineHeight,
    )
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .pointerInput(spacingPx) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        residual = 0f
                        dragging = true
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val next = residual + dragAmount
                        val atCoolEnd = latestIndex <= 0 && next > 0f
                        val atWarmEnd = latestIndex >= latestTickCount && next < 0f
                        if (atCoolEnd || atWarmEnd) {
                            residual = 0f
                        } else {
                            residual = next
                            val rawSteps = temperatureScaleDragSteps(residual, spacingPx)
                            val steps = rawSteps.coerceIn(-latestIndex, latestTickCount - latestIndex)
                            if (steps != 0) {
                                residual += steps * spacingPx
                                latestHaptics.tick()
                                latestSteps(steps)
                            }
                        }
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight),
        ) {
            val shift = shownShift
            val center = size.width / 2f
            val halfWidth = (size.width / 2f).coerceAtLeast(1f)
            val labelClearance = 8.dp.toPx()
            val sampleLabel = textMeasurer.measure(
                text = "88",
                style = labelStyle.merge(TextStyle(textAlign = TextAlign.Center)),
            )
            val labelHeight = sampleLabel.size.height.toFloat()
            val pointerGap = 6.dp.toPx()
            val pointerHeight = 14.dp.toPx()
            var maxLen = MaxTickLength.toPx().coerceAtMost(size.height * 0.28f)
            var baseline = size.height - 2.dp.toPx() - pointerHeight - pointerGap
            var labelTop = baseline - maxLen - labelClearance - labelHeight
            if (labelTop < 0f) {
                maxLen = (maxLen + labelTop).coerceAtLeast(maxLen * 0.55f)
                labelTop = baseline - maxLen - labelClearance - labelHeight
                if (labelTop < 0f) {
                    baseline -= labelTop
                    labelTop = 0f
                }
            }
            val stroke = 2.dp.toPx()
            var lastLabelRight = Float.NEGATIVE_INFINITY
            for (index in 0..tickCount) {
                val x = center + (index - animatedIndex) * spacingPx + shift
                if (x < -TickSpacing.toPx() || x > size.width + TickSpacing.toPx()) continue
                val distance = abs(x - center)
                val onLeft = x <= center
                val fade = temperatureScaleEdgeFade(
                    distance,
                    halfWidth,
                    fadeStart = if (onLeft) 0.9f else 0.82f,
                )
                val side = temperatureScaleSideBrightness(x - center, halfWidth)
                val alpha = fade * side
                if (alpha <= 0.02f) continue
                val selected = distance < spacingPx * 0.45f
                val arc = temperatureScaleTickHeightFraction(distance, halfWidth)
                val celsius = minTemperature + index * step
                val display = celsius.toDisplayTemperature(
                    unit = temperatureUnit,
                    minCelsius = minTemperature,
                    celsiusStep = temperatureStepCelsius,
                    minFahrenheit = minTemperatureFahrenheit,
                    fahrenheitStep = temperatureStepFahrenheit,
                )
                val major = temperatureScaleIsMajorTick(display, temperatureUnit)
                val tickLen = if (selected) maxLen else maxLen * arc
                val tickTop = baseline - tickLen
                val blur = if (selected || onLeft) 0f else (1f - fade) * 4.dp.toPx()
                val markStroke = when {
                    selected -> stroke * 2.2f
                    major -> stroke * 2f
                    else -> stroke + blur * 0.35f
                }
                val markColor = if (selected || x <= center) {
                    tickColor
                } else {
                    val right = ((x - center) / halfWidth).coerceIn(0f, 1f)
                    lerp(tickColor, Color.Black, 0.1f + right * 0.16f)
                }
                if (blur > 0.5f) {
                    val passes = 4
                    for (pass in passes downTo 1) {
                        val spread = blur * pass / passes
                        drawLine(
                            color = markColor.copy(alpha = alpha * 0.16f),
                            start = Offset(x, tickTop),
                            end = Offset(x, baseline),
                            strokeWidth = markStroke + spread * 2f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                drawLine(
                    color = markColor.copy(alpha = if (selected) 1f else alpha),
                    start = Offset(x, tickTop),
                    end = Offset(x, baseline),
                    strokeWidth = markStroke,
                    cap = StrokeCap.Round,
                )
                if (major) {
                    val label = display.roundToInt().toString()
                    val layout = textMeasurer.measure(
                        text = label,
                        style = labelStyle.merge(TextStyle(textAlign = TextAlign.Center)),
                    )
                    val labelWidth = layout.size.width.toFloat()
                    if (
                        temperatureScaleLabelFits(
                            centerPx = x,
                            labelWidthPx = labelWidth,
                            previousRightPx = lastLabelRight,
                            gapPx = labelClearance,
                        )
                    ) {
                        drawText(
                            textLayoutResult = layout,
                            color = markColor.copy(alpha = alpha),
                            topLeft = Offset(x - labelWidth / 2f, labelTop),
                        )
                        lastLabelRight = x + labelWidth / 2f
                    }
                }
            }
            val apexY = baseline + pointerGap
            val baseY = apexY + pointerHeight
            val half = 11.dp.toPx()
            drawPath(
                path = roundedTriangle(
                    apex = Offset(center, apexY),
                    bottomLeft = Offset(center - half, baseY),
                    bottomRight = Offset(center + half, baseY),
                    cornerRadius = 3.5.dp.toPx(),
                ),
                color = tickColor,
            )
        }
        AnimatedTemperatureCounter(
            temperatureLabel = temperatureLabel,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            sortKey = sortKey,
            textStyle = readoutStyle,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
