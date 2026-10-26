package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.vehicle.DistanceReadout
import com.test.design.presentation.ivi.vehicle.RangeBand
import com.test.design.presentation.ivi.vehicle.RangeCapacityMiles
import com.test.design.presentation.ivi.vehicle.distanceReadout
import com.test.design.presentation.ivi.vehicle.odometerDigitDimmed
import com.test.design.presentation.ivi.vehicle.odometerDigits
import com.test.design.presentation.ivi.vehicle.odometerPlaces
import com.test.design.presentation.ivi.vehicle.rangeBand
import com.test.design.presentation.ivi.vehicle.rangeFillFraction
import com.test.design.presentation.ivi.vehicle.rangeRoad

private val DrumWell = Color(0xFF121417)

@Composable
internal fun rangeAccentColor(band: RangeBand): Color = when (band) {
    RangeBand.Empty -> MaterialTheme.colorScheme.onSurfaceVariant
    RangeBand.Low -> MaterialTheme.colorScheme.error
    RangeBand.Mid -> MaterialTheme.colorScheme.tertiary
    RangeBand.Good -> MaterialTheme.colorScheme.primary
}

@Composable
fun OdometerGraphic(
    label: String?,
    fallbackMiles: Int,
    missingLabelIsUnavailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val readout = distanceReadout(
        label = label,
        fallbackWhole = fallbackMiles,
        missingLabelIsUnavailable = missingLabelIsUnavailable,
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Odometer",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = DrumWell,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            if (readout.available) {
                OdometerDrums(readout = readout, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun OdometerDrums(
    readout: DistanceReadout,
    modifier: Modifier = Modifier,
) {
    val places = odometerPlaces(readout.whole)
    val digits = odometerDigits(readout.whole, places)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val unitSlot = 44.dp
        val gap = 3.dp
        val digitWidth = ((maxWidth - unitSlot - gap * (places - 1)) / places).coerceIn(14.dp, 34.dp)
        val digitHeight = (digitWidth * 1.45f).coerceIn(28.dp, 48.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            digits.forEachIndexed { index, digit ->
                OdometerDigit(
                    digit = digit,
                    dimmed = odometerDigitDimmed(digits, index),
                    width = digitWidth,
                    modifier = Modifier.size(width = digitWidth, height = digitHeight),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = readout.unit,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun OdometerDigit(
    digit: Int,
    dimmed: Boolean,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val motionSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val color = if (dimmed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF1A1E24))
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.35f),
                    ),
                ),
            )
        }
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                slideInVertically(animationSpec = motionSpec) { height -> height } togetherWith
                    slideOutVertically(animationSpec = motionSpec) { height -> -height }
            },
            label = "odometer_digit",
        ) { target ->
            Text(
                text = target.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = (width.value * 0.58f).coerceIn(13f, 24f).sp,
                ),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun RangeGraphic(
    label: String,
    miles: Int,
    modifier: Modifier = Modifier,
    capacityMiles: Int = RangeCapacityMiles,
) {
    val available = label.isNotBlank() && label != "—"
    val fraction = if (available) rangeFillFraction(miles, capacityMiles) else 0f
    val band = if (available) rangeBand(fraction) else RangeBand.Empty
    val accent = rangeAccentColor(band)
    val fill by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700),
        label = "range_road_fill",
    )
    val pulseTransition = rememberInfiniteTransition(label = "range_alert")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "range_pulse",
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val roadHeight = if (maxHeight == Dp.Infinity) {
            156.dp
        } else {
            (maxHeight - 40.dp).coerceIn(72.dp, 280.dp)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Range",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (band == RangeBand.Low) {
                    Text(
                        "Low range",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(roadHeight)
                    .clip(RoundedCornerShape(20.dp)),
            ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRangeRoad(
                    fraction = fill,
                    accent = accent,
                    pulse = if (band == RangeBand.Low) pulse else 1f,
                    critical = band == RangeBand.Low,
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                contentColor = accent,
            ) {
                Text(
                    text = if (available) label else "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }
        }
    }
}

@Composable
fun RangeFillTrack(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = color.copy(alpha = 0.18f),
            size = size,
            cornerRadius = radius,
        )
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped > 0.01f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0.45f), color),
                    endX = size.width * clamped,
                ),
                size = Size(size.width * clamped, size.height),
                cornerRadius = radius,
            )
            val capX = size.width * clamped
            drawCircle(
                color = Color.White,
                radius = size.height * 0.28f,
                center = Offset(capX.coerceIn(size.height / 2f, size.width - size.height / 2f), size.height / 2f),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRangeRoad(
    fraction: Float,
    accent: Color,
    pulse: Float,
    critical: Boolean,
) {
    val road = rangeRoad(size.width, size.height, fraction)
    val clip = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                cornerRadius = CornerRadius(size.height * 0.08f, size.height * 0.08f),
            ),
        )
    }
    clipPath(clip) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.30f),
                    Color(0xFF10141A),
                ),
                endY = road.horizonY * 1.4f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(size.width * 0.5f, road.horizonY),
                radius = size.width * 0.22f,
            ),
            radius = size.width * 0.22f,
            center = Offset(size.width * 0.5f, road.horizonY),
        )
        val asphalt = Path().apply {
            moveTo(road.nearLeft, road.bottom)
            lineTo(road.farLeft, road.horizonY)
            lineTo(road.farRight, road.horizonY)
            lineTo(road.nearRight, road.bottom)
            close()
        }
        drawPath(path = asphalt, color = DrumWell)
        if (fraction > 0.01f) {
            val fill = Path().apply {
                moveTo(road.nearLeft, road.bottom)
                lineTo(road.fillFarLeft, road.fillFarY)
                lineTo(road.fillFarRight, road.fillFarY)
                lineTo(road.nearRight, road.bottom)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    colors = listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.35f)),
                    startY = road.fillFarY,
                    endY = road.bottom,
                ),
            )
            val dashes = 6
            for (index in 0 until dashes) {
                val start = index / dashes.toFloat()
                val end = (index + 0.42f) / dashes.toFloat()
                val span = (road.bottom - road.horizonY).coerceAtLeast(1f)
                val fillFromHorizon = ((road.fillFarY - road.horizonY) / span).coerceIn(0f, 1f)
                if (start < fillFromHorizon) continue
                val y0 = road.horizonY + span * start
                val y1 = road.horizonY + span * end.coerceAtMost(1f)
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(size.width / 2f, y0),
                    end = Offset(size.width / 2f, y1),
                    strokeWidth = size.width * 0.012f,
                )
            }
        }
        val glow = if (critical) 5f * pulse else 2.5f
        drawPath(
            path = asphalt,
            color = accent.copy(alpha = if (critical) 0.95f * pulse else 0.9f),
            style = Stroke(width = glow),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(road.farLeft, road.horizonY),
            end = Offset(road.farRight, road.horizonY),
            strokeWidth = 2f,
        )
        val nose = Offset(size.width / 2f, road.bottom - size.height * 0.06f)
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(nose.x - size.width * 0.035f, road.bottom - size.height * 0.015f),
            end = nose,
            strokeWidth = 3f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = nose,
            end = Offset(nose.x + size.width * 0.035f, road.bottom - size.height * 0.015f),
            strokeWidth = 3f,
        )
    }
}
