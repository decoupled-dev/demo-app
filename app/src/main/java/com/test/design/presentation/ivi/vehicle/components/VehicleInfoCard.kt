package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.R
import com.test.design.presentation.ivi.vehicle.DriveMode
import com.test.design.presentation.ivi.vehicle.VehicleGearOrder
import com.test.design.presentation.ivi.vehicle.VehicleUiState
import com.test.design.presentation.ivi.vehicle.toInfoReadout
import com.test.design.theme.ExpressiveShapes

private val CardShape = ExpressiveShapes.large
private val GearShape = RoundedCornerShape(24.dp)
private val StageShape = RoundedCornerShape(28.dp)

@Composable
fun VehicleInfoCard(
    state: VehicleUiState,
    onDriveModeSelected: (DriveMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readout = state.toInfoReadout()
    val accent = driveAccent(state.driveMode)
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(CardShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f), accent.copy(alpha = 0.45f)),
                ),
                shape = CardShape,
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B2130), Color(0xFF10141C)),
                ),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "DRIVE",
                    color = Color(0xFFB7BCC4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                )
                Text(
                    text = state.driveMode.label,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Text(
                    text = state.driveMode.subtitle,
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(StageShape)
                .background(Color.Black.copy(alpha = 0.28f)),
        ) {
            SuvStage(
                driveMode = state.driveMode,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
        }
        MorphingDriveModeSelector(
            selected = state.driveMode,
            onSelected = onDriveModeSelected,
            embedded = true,
        )
        GearBar(
            selected = readout.selectedGear,
            accent = accent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun driveAccent(mode: DriveMode): Color = when (mode) {
    DriveMode.Eco -> Color(0xFF3DDC84)
    DriveMode.Comfort -> Color(0xFF7EB6FF)
    DriveMode.Sport -> Color(0xFFFF6A3D)
}

/** Width / height of the side-profile SUV drawable, cropped at the tire contact patch. */
private const val SuvAspect = 1079f / 364f

@Composable
private fun SuvStage(
    driveMode: DriveMode,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val carWidth = minOf(maxWidth - 24.dp, (maxHeight * 0.66f) * SuvAspect)
        val carHeight = carWidth / SuvAspect
        val carTop = (maxHeight - carHeight) * 0.46f
        val groundTop = carTop + carHeight
        Canvas(modifier = Modifier.fillMaxSize()) {
            val groundPx = groundTop.toPx()
            val carWidthPx = carWidth.toPx()
            val carHeightPx = carHeight.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.45f,
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.22f), accent.copy(alpha = 0.06f)),
                    startY = groundPx - carHeightPx * 0.12f,
                    endY = size.height,
                ),
                topLeft = Offset(0f, groundPx - carHeightPx * 0.12f),
                size = Size(size.width, size.height - groundPx + carHeightPx * 0.12f),
            )
            val shadowWidth = carWidthPx * 0.92f
            val shadowHeight = carHeightPx * 0.16f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.70f), Color.Black.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width / 2f, groundPx),
                    radius = shadowWidth / 2f,
                ),
                topLeft = Offset((size.width - shadowWidth) / 2f, groundPx - shadowHeight / 2f),
                size = Size(shadowWidth, shadowHeight),
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.55f), Color.Transparent),
                ),
                start = Offset(size.width * 0.08f, groundPx + 1.dp.toPx()),
                end = Offset(size.width * 0.92f, groundPx + 1.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
        AnimatedSuvArt(
            driveMode = driveMode,
            modifier = Modifier
                .padding(top = carTop)
                .align(Alignment.TopCenter)
                .width(carWidth)
                .height(carHeight),
        )
    }
}

@Composable
private fun AnimatedSuvArt(
    driveMode: DriveMode,
    modifier: Modifier = Modifier,
) {
    val bobMs = when (driveMode) {
        DriveMode.Eco -> 3200
        DriveMode.Comfort -> 2200
        DriveMode.Sport -> 1100
    }
    val bobDp = when (driveMode) {
        DriveMode.Eco -> 1.5f
        DriveMode.Comfort -> 2.5f
        DriveMode.Sport -> 4f
    }
    val transition = rememberInfiniteTransition(label = "suv_motion")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(bobMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "suv_bob",
    )
    Image(
        painter = painterResource(R.drawable.vehicle_suv_side),
        contentDescription = "SUV side profile",
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            translationY = -(bob * bobDp * density)
        },
    )
}

@Composable
private fun GearBar(
    selected: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = VehicleGearOrder.indexOf(selected)
    BoxWithConstraints(
        modifier = modifier
            .height(72.dp)
            .clip(GearShape)
            .background(Color(0xFF101318))
            .padding(6.dp)
            .semantics { contentDescription = "Gear ${selected ?: "unknown"}" },
    ) {
        val segment = maxWidth / VehicleGearOrder.size
        val indicatorOffset by animateDpAsState(
            targetValue = segment * selectedIndex.coerceAtLeast(0),
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            label = "gear_indicator",
        )
        if (selectedIndex >= 0) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segment)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.72f)),
                        ),
                    ),
            )
        }
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            VehicleGearOrder.forEach { gear ->
                val isSelected = gear == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = gear,
                        color = if (isSelected) Color.White else Color(0xFF8E96A0),
                        fontSize = 22.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
