package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.vehicle.rangeBand
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget
import kotlin.math.cos
import kotlin.math.sin

private const val GaugeStartDegrees = 135f
private const val GaugeSweepDegrees = 270f

@Composable
fun VehicleEnergyCockpit(
    percent: Int,
    rangeMiles: Int,
    maxRangeMiles: Int,
    isCharging: Boolean,
    chargeRateKw: Float,
    gaugeShape: Shape,
    onGaugeClick: () -> Unit,
    rangeLabel: String = "$rangeMiles mi",
    energyTitle: String = "Energy",
    supportingText: String? = null,
    summaryFraction: Float? = null,
    summaryRemaining: String? = null,
    summaryAverage: String? = null,
    summaryCapacity: String? = null,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    controlsModifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val gaugeSize = if (compact) 100.dp else 260.dp
    val strokeDp = if (compact) 6.dp else 14.dp
    val rangeStrokeDp = if (compact) 4.dp else 8.dp
    val padding = if (compact) 4.dp else CarDesignTokens.TouchTargetSpacing
    val motionScheme = MaterialTheme.motionScheme
    val animatedPercent by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = motionScheme.slowSpatialSpec(),
        label = "battery_percent",
    )
    val animatedRange by animateFloatAsState(
        targetValue = (rangeMiles / maxRangeMiles.toFloat()).coerceIn(0f, 1f),
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "range_fraction",
    )
    val arcColor by animateColorAsState(
        targetValue = when {
            isCharging -> Color(0xFF00E5FF)
            percent >= 60 -> MaterialTheme.colorScheme.primary
            percent >= 30 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        },
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "battery_color",
    )
    val pulseTransition = rememberInfiniteTransition(label = "charge_pulse")
    val chargePulse by pulseTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "charge_pulse_scale",
    )
    val orbDrift by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb_drift",
    )
    val chargeOrbit by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "charge_orbit",
    )

    Box(
        modifier = modifier
            .then(if (compact) Modifier.wrapContentHeight() else Modifier)
            .clip(gaugeShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.98f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    ),
                ),
                shape = gaugeShape,
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                    center = Offset(0.3f + orbDrift * 0.15f, 0.2f),
                    radius = 700f,
                ),
            )
            .carTouchTarget()
            .clickable(onClick = onGaugeClick),
    ) {
        if (!compact) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            arcColor.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension * 0.55f,
                    ),
                    radius = size.minDimension * 0.55f,
                    center = center,
                )
            }
        }

        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GaugeWithStats(
                    percent = percent,
                    rangeLabel = rangeLabel,
                    gaugeSize = gaugeSize,
                    strokeDp = strokeDp,
                    rangeStrokeDp = rangeStrokeDp,
                    animatedPercent = animatedPercent,
                    animatedRange = animatedRange,
                    arcColor = arcColor,
                    isCharging = isCharging,
                    chargePulse = chargePulse,
                        chargeOrbit = chargeOrbit,
                        compact = true,
                        showCenterRange = true,
                        contentModifier = contentModifier,
                    )
                Text(
                    text = when {
                        isCharging -> "Charging · ${chargeRateKw.toInt()} kW"
                        percent < 30 -> "Low · $rangeLabel"
                        else -> "Ready · $rangeLabel"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = controlsModifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val showSummary = summaryFraction != null &&
                    summaryRemaining != null &&
                    summaryAverage != null
                val heroGauge = heroGaugeSize(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    reserved = if (showSummary) 268.dp else 156.dp,
                )
                val heightBounded = maxHeight != Dp.Infinity
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (heightBounded) Modifier.fillMaxSize() else Modifier)
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (heightBounded) {
                        Arrangement.SpaceBetween
                    } else {
                        Arrangement.spacedBy(12.dp)
                    },
                ) {
                    EnergyHeader(
                        title = if (isCharging) "Charging" else energyTitle,
                        status = when {
                            isCharging -> "${chargeRateKw.toInt()} kW"
                            percent < 30 -> "Low"
                            else -> "Ready"
                        },
                        statusColor = arcColor,
                    )

                    GaugeWithStats(
                        percent = percent,
                        rangeLabel = rangeLabel,
                        gaugeSize = heroGauge,
                        strokeDp = strokeDp,
                        rangeStrokeDp = rangeStrokeDp,
                        animatedPercent = animatedPercent,
                        animatedRange = animatedRange,
                        arcColor = arcColor,
                        isCharging = isCharging,
                        chargePulse = chargePulse,
                        chargeOrbit = chargeOrbit,
                        compact = false,
                        showCenterRange = !showSummary,
                        contentModifier = contentModifier,
                    )

                    if (showSummary) {
                        EnergyRangeSummary(
                            fraction = summaryFraction,
                            remaining = summaryRemaining,
                            average = summaryAverage,
                            capacity = summaryCapacity,
                            modifier = controlsModifier.fillMaxWidth(),
                        )
                    } else {
                        Column(
                            modifier = controlsModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = when {
                                    isCharging -> "${chargeRateKw.toInt()} kW"
                                    percent < 30 -> if (energyTitle == "Fuel") "Low fuel" else "Low battery"
                                    else -> "Ready to drive"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = supportingText ?: when {
                                    isCharging -> "Plugged in"
                                    else -> "$maxRangeMiles mi capacity"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun heroGaugeSize(maxWidth: Dp, maxHeight: Dp, reserved: Dp = 156.dp): Dp {
    if (maxHeight == Dp.Infinity) return 260.dp
    val available = (maxHeight - reserved).coerceAtLeast(140.dp)
    return minOf(maxWidth * 0.9f, available).coerceIn(140.dp, 400.dp)
}

@Composable
private fun EnergyHeader(
    title: String,
    status: String,
    statusColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "ENERGY",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(statusColor.copy(alpha = 0.16f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun EnergyRangeSummary(
    fraction: Float,
    remaining: String,
    average: String,
    capacity: String?,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "range_instrument",
    )
    val stats = buildList {
        add(splitStat(remaining) to "Remaining")
        add(splitStat(average) to "Average")
        if (capacity != null) add(splitStat(capacity) to "Capacity")
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "RANGE",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${(animated * 100f).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF7DFFB0),
            )
        }
        RangeInstrument(fraction = animated)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            stats.forEachIndexed { index, (measure, label) ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .width(1.dp)
                            .height(44.dp)
                            .background(Color.White.copy(alpha = 0.12f)),
                    )
                }
                InstrumentReadout(
                    value = measure.first,
                    unit = measure.second,
                    label = label,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RangeInstrument(fraction: Float) {
    val fill = fraction.coerceIn(0f, 1f)
    val green = Color(0xFF3DDC84)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
    ) {
        val knob = 18.dp
        val travel = (maxWidth - knob).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFB8FFC9), green),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = travel * fill)
                .size(knob)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, green, CircleShape),
        )
    }
}

@Composable
private fun InstrumentReadout(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                    maxLines = 1,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun splitStat(text: String): Pair<String, String> {
    val space = text.lastIndexOf(' ')
    if (space <= 0) return text to ""
    return text.substring(0, space) to text.substring(space + 1)
}

@Composable
private fun GaugeWithStats(
    percent: Int,
    rangeLabel: String,
    gaugeSize: androidx.compose.ui.unit.Dp,
    strokeDp: androidx.compose.ui.unit.Dp,
    rangeStrokeDp: androidx.compose.ui.unit.Dp,
    animatedPercent: Float,
    animatedRange: Float,
    arcColor: Color,
    isCharging: Boolean,
    chargePulse: Float,
    chargeOrbit: Float,
    compact: Boolean,
    showCenterRange: Boolean,
    contentModifier: Modifier,
) {
    Box(
        modifier = Modifier.size(gaugeSize),
        contentAlignment = Alignment.Center,
    ) {
        val gaugeScale = if (isCharging) chargePulse else 1f
        Canvas(
            modifier = Modifier
                .size(gaugeSize)
                .graphicsLayer {
                    scaleX = gaugeScale
                    scaleY = gaugeScale
                },
        ) {
            val stroke = strokeDp.toPx()
            val rangeStroke = rangeStrokeDp.toPx()
            val arcRadius = size.minDimension / 2f - stroke / 2f
            fun polar(degrees: Float, radius: Float): Offset {
                val radians = Math.toRadians(degrees.toDouble())
                return Offset(
                    x = center.x + (cos(radians) * radius).toFloat(),
                    y = center.y + (sin(radians) * radius).toFloat(),
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        arcColor.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = arcRadius * 0.92f,
                ),
                radius = arcRadius * 0.92f,
                center = center,
            )
            if (!compact) {
                val tickCount = 18
                for (index in 0..tickCount) {
                    val angle = GaugeStartDegrees + GaugeSweepDegrees * index / tickCount
                    val major = index % 3 == 0
                    val outer = arcRadius + stroke * 0.42f
                    val inner = outer - if (major) stroke * 0.72f else stroke * 0.38f
                    drawLine(
                        color = Color.White.copy(alpha = if (major) 0.34f else 0.16f),
                        start = polar(angle, inner),
                        end = polar(angle, outer),
                        strokeWidth = if (major) 2.4f else 1.4f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = GaugeStartDegrees,
                sweepAngle = GaugeSweepDegrees,
                useCenter = false,
                style = Stroke(width = rangeStroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = arcColor.copy(alpha = 0.4f),
                startAngle = GaugeStartDegrees,
                sweepAngle = GaugeSweepDegrees * animatedRange,
                useCenter = false,
                style = Stroke(width = rangeStroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = GaugeStartDegrees,
                sweepAngle = GaugeSweepDegrees,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val filledSweep = GaugeSweepDegrees * animatedPercent
            if (filledSweep > 0.5f) {
                drawArc(
                    color = arcColor.copy(alpha = if (isCharging) 0.28f else 0.18f),
                    startAngle = GaugeStartDegrees,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    style = Stroke(width = stroke * 2.3f, cap = StrokeCap.Round),
                )
                val tip = polar(GaugeStartDegrees + filledSweep, arcRadius)
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            arcColor.copy(alpha = 0.35f),
                            arcColor,
                            Color.White.copy(alpha = 0.95f),
                        ),
                        start = polar(GaugeStartDegrees, arcRadius),
                        end = tip,
                    ),
                    startAngle = GaugeStartDegrees,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawCircle(
                    color = arcColor.copy(alpha = 0.35f),
                    radius = stroke * 0.95f,
                    center = tip,
                )
                drawCircle(
                    color = Color.White,
                    radius = stroke * 0.28f,
                    center = tip,
                )
                if (isCharging) {
                    val comet = 34f
                    val head = chargeOrbit * filledSweep
                    val cometStart = (head - comet).coerceAtLeast(0f)
                    drawArc(
                        color = Color.White.copy(alpha = 0.9f),
                        startAngle = GaugeStartDegrees + cometStart,
                        sweepAngle = (head - cometStart).coerceAtLeast(6f),
                        useCenter = false,
                        style = Stroke(width = stroke * 0.42f, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = contentModifier,
        ) {
            AnimatedStatCounter(
                value = percent,
                suffix = "%",
                style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayLarge,
            )
            if (showCenterRange) {
                val rangeAccent = rangeAccentColor(rangeBand(animatedRange))
                Text(
                    text = rangeLabel,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.headlineSmall,
                    color = rangeAccent,
                )
                if (!compact && gaugeSize >= 220.dp) {
                    RangeFillTrack(
                        fraction = animatedRange,
                        color = rangeAccent,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .width(gaugeSize * 0.46f)
                            .height(8.dp),
                    )
                }
            } else if (!compact) {
                Text(
                    text = "State of charge",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AnimatedStatCounter(
    value: Int,
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    modifier: Modifier = Modifier,
) {
    val motionSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            slideInVertically(animationSpec = motionSpec) { h -> direction * h } togetherWith
                slideOutVertically(animationSpec = motionSpec) { h -> -direction * h }
        },
        label = "stat_counter",
    ) { target ->
        Text(
            text = "$target$suffix",
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
