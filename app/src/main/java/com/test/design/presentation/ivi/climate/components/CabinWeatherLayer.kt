package com.test.design.presentation.ivi.climate.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.test.design.presentation.ivi.climate.AirflowMode
import com.test.design.presentation.ivi.climate.CabinWeatherPhysics
import kotlin.math.PI
import kotlin.math.sin

private const val ClockMs = 24_000
private const val TwoPi = (PI * 2.0).toFloat()

/**
 * Cabin atmosphere as light, not VFX: two thermal rooms, a mixing seam,
 * glass condensation, and a faint outside rim. No particles, no HUD lines.
 */
@Composable
fun CabinWeatherLayer(
    driverFraction: Float,
    passengerFraction: Float,
    driverGlow: Color,
    passengerGlow: Color,
    driverCool: Float,
    passengerCool: Float,
    fanSpeed: Int,
    maxFanSpeed: Int,
    airflowMode: AirflowMode,
    isRecirculationOn: Boolean,
    isFrontDefrostOn: Boolean,
    isAcEnabled: Boolean,
    isPowerOn: Boolean,
    isSyncEnabled: Boolean,
    isAutoOn: Boolean,
    modifier: Modifier = Modifier,
) {
    val driverF = driverFraction.coerceIn(0f, 1f)
    val passengerF = passengerFraction.coerceIn(0f, 1f)
    val front = CabinWeatherPhysics.frontStrength(
        driverF, passengerF, isSyncEnabled, isAutoOn,
    )
    val mixHalf = CabinWeatherPhysics.mixHalfWidth(front)
    val mixCenter = 0.5f + CabinWeatherPhysics.frontOffset(driverF, passengerF)
    val frostTarget = CabinWeatherPhysics.frostAmount(
        driverCool,
        passengerCool,
        isFrontDefrostOn,
        airflowMode == AirflowMode.FeetDefrost,
        isPowerOn,
    )
    val pressureTarget = CabinWeatherPhysics.outsidePressure(
        isAcEnabled, isRecirculationOn, isPowerOn,
    )
    val yTarget = CabinWeatherPhysics.atmosphereY(airflowMode, isAutoOn)
    val breathHz = CabinWeatherPhysics.breathHertz(fanSpeed, maxFanSpeed, isPowerOn)
    val breathAmp = CabinWeatherPhysics.breathAmplitude(fanSpeed, maxFanSpeed, isPowerOn)

    val frost by animateFloatAsState(frostTarget, label = "cabin_frost")
    val pressure by animateFloatAsState(pressureTarget, label = "cabin_pressure")
    val airY by animateFloatAsState(yTarget, label = "cabin_air_y")
    val power by animateFloatAsState(if (isPowerOn) 1f else 0.28f, label = "cabin_power")
    val defrostClear by animateFloatAsState(
        if (isFrontDefrostOn) 1f else 0f,
        label = "cabin_defrost_clear",
    )
    val recircSeal by animateFloatAsState(
        if (isRecirculationOn && isPowerOn) 1f else 0f,
        label = "cabin_recirc",
    )

    val clock = rememberInfiniteTransition(label = "cabin_light")
    val t by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(ClockMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cabin_clock",
    )
    val breath = 1f + sin(t * TwoPi * breathHz * (ClockMs / 1000f)) * breathAmp
    val drift = sin(t * TwoPi) * 0.018f

    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawThermalRooms(
            w = w,
            h = h,
            mixCenter = mixCenter,
            mixHalf = mixHalf,
            airY = airY + drift,
            driver = driverGlow,
            passenger = passengerGlow,
            power = power * breath,
        )
        drawOutsideRim(
            w = w,
            h = h,
            summer = isAcEnabled,
            pressure = pressure * (1f - recircSeal * 0.75f),
            power = power,
        )
        drawGlass(
            w = w,
            h = h,
            frost = frost,
            defrostClear = defrostClear,
            power = power,
        )
        if (!isPowerOn) {
            drawResidualWarmth(w, h, driverGlow)
        }
    }
}

private fun DrawScope.drawThermalRooms(
    w: Float,
    h: Float,
    mixCenter: Float,
    mixHalf: Float,
    airY: Float,
    driver: Color,
    passenger: Color,
    power: Float,
) {
    val cy = h * airY.coerceIn(0.32f, 0.74f)
    val radius = w * 0.58f
    val left = Offset(w * (mixCenter - 0.28f).coerceIn(0.16f, 0.4f), cy)
    val right = Offset(w * (mixCenter + 0.28f).coerceIn(0.6f, 0.84f), cy)
    val fill = 0.18f * power

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(driver.copy(alpha = fill), Color.Transparent),
            center = left,
            radius = radius,
        ),
        center = left,
        radius = radius,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(passenger.copy(alpha = fill), Color.Transparent),
            center = right,
            radius = radius,
        ),
        center = right,
        radius = radius,
    )

    val seam = mixCenter.coerceIn(0.22f, 0.78f)
    val half = mixHalf.coerceIn(0.1f, 0.4f)
    drawRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                (seam - half).coerceIn(0f, 1f) to driver.copy(alpha = 0.16f * power),
                seam.coerceIn(0f, 1f) to Color.Transparent,
                (seam + half).coerceIn(0f, 1f) to passenger.copy(alpha = 0.16f * power),
            ),
        ),
    )
}

private fun DrawScope.drawOutsideRim(
    w: Float,
    h: Float,
    summer: Boolean,
    pressure: Float,
    power: Float,
) {
    val rim = if (summer) Color(0xFFE8C9A8) else Color(0xFFC5D4E0)
    val a = 0.1f * pressure * power
    if (a < 0.01f) return
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to rim.copy(alpha = a),
                0.14f to Color.Transparent,
                0.88f to Color.Transparent,
                1f to rim.copy(alpha = a * 0.45f),
            ),
        ),
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to rim.copy(alpha = a * 0.55f),
                0.1f to Color.Transparent,
                0.9f to Color.Transparent,
                1f to rim.copy(alpha = a * 0.55f),
            ),
        ),
    )
}

private fun DrawScope.drawGlass(
    w: Float,
    h: Float,
    frost: Float,
    defrostClear: Float,
    power: Float,
) {
    if (frost < 0.03f) return
    val milk = Color(0xFFD9E3EC).copy(alpha = 0.11f * frost * power)
    val edge = milk.copy(alpha = milk.alpha * (0.55f + defrostClear * 0.7f))
    val centerAlpha = milk.alpha * (1f - defrostClear * 0.92f)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFD9E3EC).copy(alpha = centerAlpha),
                edge,
                Color.Transparent,
            ),
            center = Offset(w * 0.5f, h * 0.02f),
            radius = w * (0.42f + defrostClear * 0.18f),
        ),
        size = Size(w, h * 0.28f),
    )
}

private fun DrawScope.drawResidualWarmth(w: Float, h: Float, driver: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                driver.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(w * 0.28f, h * 0.66f),
            radius = w * 0.18f,
        ),
        center = Offset(w * 0.28f, h * 0.66f),
        radius = w * 0.18f,
    )
}
