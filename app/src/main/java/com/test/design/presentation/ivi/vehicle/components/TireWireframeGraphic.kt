package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.R
import com.test.design.presentation.ivi.vehicle.FrontAxleSpan
import com.test.design.presentation.ivi.vehicle.RearAxleSpan
import com.test.design.presentation.ivi.vehicle.TireCornerDisplay
import com.test.design.presentation.ivi.vehicle.TireStatus
import com.test.design.presentation.ivi.vehicle.TireWireframe
import com.test.design.presentation.ivi.vehicle.TopWireframeAspect

private val CardShape = RoundedCornerShape(20.dp)

private data class StatusPalette(val accent: Color, val fill: Color, val bar: Color)

private fun TireStatus.palette(): StatusPalette = when (this) {
    TireStatus.Normal -> StatusPalette(Color(0xFF3A7BC8), Color(0xFF14283D), Color(0xFF2F8FE8))
    TireStatus.Warning -> StatusPalette(Color(0xFFE0B03A), Color(0xFF3A3016), Color(0xFFF2B233))
    TireStatus.Critical -> StatusPalette(Color(0xFFE4572E), Color(0xFF3B1A15), Color(0xFFF0682E))
}

@Composable
internal fun TireWireframeGraphic(
    wireframe: TireWireframe,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TireCornerCard(wireframe.frontLeft)
            TireCornerCard(wireframe.rearLeft)
        }
        TopDownCar(
            wireframe = wireframe,
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TireCornerCard(wireframe.frontRight)
            TireCornerCard(wireframe.rearRight)
        }
    }
}

@Composable
private fun TireCornerCard(corner: TireCornerDisplay) {
    val palette = corner.status.palette()
    val alert = corner.status != TireStatus.Normal
    val textAccent = if (alert) palette.accent else Color.White
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, palette.accent, CardShape)
                .background(palette.fill, CardShape)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (alert) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = corner.title,
                    color = textAccent,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (alert) {
                    Icon(
                        imageVector = Icons.Outlined.TireRepair,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(24.dp),
                    )
                }
                Text(
                    text = corner.psi?.toString() ?: "—",
                    color = textAccent,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "psi",
                    color = textAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = corner.temperature,
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = corner.updated,
            color = Color(0xFF9AA3B2),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TopDownCar(
    wireframe: TireWireframe,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "tire_bar_pulse").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tire_bar_alpha",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val car = fittedCarRect(size)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = car.center,
                    radius = car.height * 0.55f,
                ),
                radius = car.height * 0.55f,
                center = car.center,
            )
        }
        Image(
            painter = painterResource(R.drawable.vehicle_top_wireframe),
            contentDescription = "Vehicle top view",
            contentScale = ContentScale.Fit,
            alpha = 0.92f,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val car = fittedCarRect(size)
            val barW = car.width * 0.07f
            drawTireBar(car, FrontAxleSpan, left = true, status = wireframe.frontLeft.status, barW = barW, pulse = pulse)
            drawTireBar(car, FrontAxleSpan, left = false, status = wireframe.frontRight.status, barW = barW, pulse = pulse)
            drawTireBar(car, RearAxleSpan, left = true, status = wireframe.rearLeft.status, barW = barW, pulse = pulse)
            drawTireBar(car, RearAxleSpan, left = false, status = wireframe.rearRight.status, barW = barW, pulse = pulse)
        }
    }
}

private fun fittedCarRect(canvas: Size): Rect {
    val heightLimited = canvas.width / canvas.height > TopWireframeAspect
    val h = if (heightLimited) canvas.height else canvas.width / TopWireframeAspect
    val w = h * TopWireframeAspect
    val left = (canvas.width - w) / 2f
    val top = (canvas.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

private fun DrawScope.drawTireBar(
    car: Rect,
    span: ClosedFloatingPointRange<Float>,
    left: Boolean,
    status: TireStatus,
    barW: Float,
    pulse: Float,
) {
    val palette = status.palette()
    val top = car.top + car.height * span.start
    val bottom = car.top + car.height * span.endInclusive
    val x = if (left) car.left - barW * 0.55f else car.right - barW * 0.45f
    val alpha = if (status == TireStatus.Normal) 1f else pulse
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(palette.bar.copy(alpha = 0.35f * alpha), Color.Transparent),
            center = Offset(x + barW / 2f, (top + bottom) / 2f),
            radius = (bottom - top) * 0.8f,
        ),
        topLeft = Offset(x - barW, top - barW),
        size = Size(barW * 3f, bottom - top + barW * 2f),
        cornerRadius = CornerRadius(barW, barW),
    )
    drawRoundRect(
        color = palette.bar.copy(alpha = alpha),
        topLeft = Offset(x, top),
        size = Size(barW, bottom - top),
        cornerRadius = CornerRadius(barW * 0.45f, barW * 0.45f),
    )
}
