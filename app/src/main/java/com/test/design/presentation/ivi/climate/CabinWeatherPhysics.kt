package com.test.design.presentation.ivi.climate

import kotlin.math.abs
import kotlin.math.max

/**
 * Quiet cabin atmosphere. Values stay in 0..1 so the canvas can stay photographic
 * — wide color mixes, glass haze, and edge light, never particle FX.
 */
object CabinWeatherPhysics {
    fun frontStrength(
        driverFraction: Float,
        passengerFraction: Float,
        synced: Boolean,
        auto: Boolean,
    ): Float {
        val delta = abs(driverFraction - passengerFraction)
        val base = (delta / 0.36f).coerceIn(0f, 1f)
        val collapse = when {
            synced -> 0.12f
            auto -> 0.42f
            else -> 1f
        }
        return base * collapse
    }

    fun frontOffset(driverFraction: Float, passengerFraction: Float): Float =
        ((passengerFraction - driverFraction) * 0.14f).coerceIn(-0.16f, 0.16f)

    fun mixHalfWidth(frontStrength: Float): Float =
        0.38f - frontStrength.coerceIn(0f, 1f) * 0.26f

    fun breathHertz(fanSpeed: Int, maxFanSpeed: Int, powerOn: Boolean): Float {
        if (!powerOn || fanSpeed <= 0) return 0.08f
        val fraction = fanSpeed / maxFanSpeed.coerceAtLeast(1).toFloat()
        return 0.12f + fraction * 0.22f
    }

    fun breathAmplitude(fanSpeed: Int, maxFanSpeed: Int, powerOn: Boolean): Float {
        if (!powerOn) return 0.04f
        if (fanSpeed <= 0) return 0.06f
        return (0.08f + 0.22f * fanSpeed / maxFanSpeed.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 0.3f)
    }

    fun frostAmount(
        driverCool: Float,
        passengerCool: Float,
        defrostOn: Boolean,
        feetDefrost: Boolean,
        powerOn: Boolean,
    ): Float {
        val cold = max(driverCool, passengerCool)
        val glass = when {
            feetDefrost && !defrostOn -> (0.4f + cold * 0.45f).coerceIn(0f, 1f)
            powerOn -> (0.08f + cold * 0.7f).coerceIn(0f, 1f)
            else -> (cold * 0.28f).coerceIn(0f, 1f)
        }
        return if (defrostOn) glass * 0.18f else glass
    }

    fun outsidePressure(acOn: Boolean, recirc: Boolean, powerOn: Boolean): Float {
        if (!powerOn) return 0.12f
        val fight = if (acOn) 0.55f else 0.4f
        return if (recirc) fight * 0.22f else fight
    }

    fun atmosphereY(mode: AirflowMode, auto: Boolean): Float {
        if (auto) return 0.54f
        return when (mode) {
            AirflowMode.Face -> 0.38f
            AirflowMode.BiLevel -> 0.5f
            AirflowMode.Feet -> 0.7f
            AirflowMode.FeetDefrost -> 0.58f
            AirflowMode.Auto -> 0.54f
        }
    }
}
