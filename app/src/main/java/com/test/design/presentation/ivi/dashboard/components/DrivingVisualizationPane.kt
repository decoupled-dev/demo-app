package com.test.design.presentation.ivi.dashboard.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.presentation.ivi.vehicle.VehicleUiState

private val VizCanvas = Color(0xFF0A0A0B)
private val RoadAsphalt = Color(0xFF141618)
private val LaneGlow = Color(0xFF3D9EFF)
private val TrafficCar = Color(0xFF6B6E74)
private val EgoCar = Color(0xFFE8EAED)
private val BatteryFill = Color(0xFF34C759)

/**
 * Tesla left cluster (~34%): compact speed/PRND/range, road viz fills the rest.
 * Uses explicit sp sizes — CarTypography is too large for this dense pane.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DrivingVisualizationPane(
    vehicleState: VehicleUiState,
    onVehicleClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    speedMph: Int = 20,
    gear: String = "D",
) {
    Surface(
        modifier = widgetContainerTransform(
            widget = DashboardWidget.Vehicle,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = modifier.fillMaxHeight(),
        ),
        color = VizCanvas,
        onClick = onVehicleClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            DrivingVizHeader(
                speedMph = speedMph,
                gear = gear,
                batteryPercent = vehicleState.batteryPercent,
                rangeLabel = vehicleState.rangeLabel,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                DrivingRoadVisualization(modifier = Modifier.fillMaxSize())
            }

            DrivingVizFooter()
        }
    }
}

@Composable
private fun DrivingVizHeader(
    speedMph: Int,
    gear: String,
    batteryPercent: Int,
    rangeLabel: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Tire pressure",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(16.dp),
            )
            Icon(
                imageVector = Icons.Outlined.WbSunny,
                contentDescription = "Headlights",
                tint = Color(0xFF34C759),
                modifier = Modifier.size(16.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$speedMph",
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 64.sp,
            )
            Text(
                text = "MPH",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("P", "R", "N", "D").forEach { g ->
                    val selected = g == gear
                    Text(
                        text = g,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.28f),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = rangeLabel,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                LinearProgressIndicator(
                    progress = { batteryPercent / 100f },
                    modifier = Modifier
                        .width(52.dp)
                        .height(4.dp),
                    color = BatteryFill,
                    trackColor = Color.White.copy(alpha = 0.16f),
                )
            }
        }
    }
}

@Composable
private fun DrivingRoadVisualization(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val vanishingX = w * 0.5f
        val vanishingY = h * 0.04f
        val roadBottomHalf = w * 0.55f

        val road = Path().apply {
            moveTo(vanishingX - w * 0.06f, vanishingY)
            lineTo(vanishingX + w * 0.06f, vanishingY)
            lineTo(vanishingX + roadBottomHalf, h)
            lineTo(vanishingX - roadBottomHalf, h)
            close()
        }
        drawPath(
            path = road,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF101214), RoadAsphalt),
                startY = vanishingY,
                endY = h,
            ),
        )

        for (i in 0 until 10) {
            val t0 = (i + 0.12f) / 10f
            val t1 = (i + 0.4f) / 10f
            val y0 = vanishingY + (h - vanishingY) * t0 * t0
            val y1 = vanishingY + (h - vanishingY) * t1 * t1
            drawLine(
                color = Color.White.copy(alpha = 0.28f),
                start = Offset(vanishingX, y0),
                end = Offset(vanishingX, y1),
                strokeWidth = 2.5f * (0.1f + t0),
            )
        }

        fun laneEdge(left: Boolean) {
            val edge = Path().apply {
                val insetTop = w * 0.045f
                val insetBot = roadBottomHalf * 0.86f
                if (left) {
                    moveTo(vanishingX - insetTop, vanishingY)
                    lineTo(vanishingX - insetBot, h)
                } else {
                    moveTo(vanishingX + insetTop, vanishingY)
                    lineTo(vanishingX + insetBot, h)
                }
            }
            drawPath(edge, LaneGlow.copy(alpha = 0.2f), style = Stroke(width = 14f))
            drawPath(edge, LaneGlow.copy(alpha = 0.7f), style = Stroke(width = 3.5f))
        }
        laneEdge(true)
        laneEdge(false)

        fun traffic(cxFrac: Float, depth: Float, carW: Float, carH: Float) {
            val y = vanishingY + (h - vanishingY) * depth
            val scale = 0.18f + depth * 0.82f
            val cx = vanishingX + cxFrac * w * 0.2f * scale
            val bw = carW * scale
            val bh = carH * scale
            drawRoundRect(
                color = TrafficCar,
                topLeft = Offset(cx - bw / 2f, y - bh),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(4f * scale, 4f * scale),
            )
        }
        traffic(-0.5f, 0.4f, 34f, 50f)
        traffic(0.48f, 0.3f, 28f, 44f)

        val egoW = w * 0.36f
        val egoH = egoW * 1.4f
        val egoLeft = vanishingX - egoW / 2f
        val egoTop = h - egoH - 4f
        drawRoundRect(
            color = EgoCar,
            topLeft = Offset(egoLeft, egoTop),
            size = Size(egoW, egoH),
            cornerRadius = CornerRadius(egoW * 0.12f, egoW * 0.12f),
        )
        drawRoundRect(
            color = Color(0xFFB0B4BA),
            topLeft = Offset(egoLeft + egoW * 0.14f, egoTop + egoH * 0.1f),
            size = Size(egoW * 0.72f, egoH * 0.2f),
            cornerRadius = CornerRadius(egoW * 0.06f, egoW * 0.06f),
        )
        drawCircle(
            color = Color(0xFFE53935),
            radius = egoW * 0.045f,
            center = Offset(egoLeft + egoW * 0.2f, egoTop + egoH * 0.55f),
        )
        drawCircle(
            color = Color(0xFFE53935),
            radius = egoW * 0.045f,
            center = Offset(egoLeft + egoW * 0.8f, egoTop + egoH * 0.55f),
        )
    }
}

@Composable
private fun DrivingVizFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VizFooterButton(Icons.Default.Videocam, "Camera")
            VizFooterButton(Icons.Default.Bolt, "Energy")
            VizFooterButton(Icons.Default.Mic, "Voice")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == 1) 6.dp else 5.dp)
                        .background(
                            color = Color.White.copy(alpha = if (index == 1) 0.85f else 0.28f),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun VizFooterButton(icon: ImageVector, contentDescription: String) {
    Surface(
        onClick = { },
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
