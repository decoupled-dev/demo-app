package com.test.design.presentation.ivi.vehicle

import com.test.design.data.car.DisplayedTire
import com.test.design.data.car.VehicleSignals
import com.test.design.data.car.WheelPosition
import com.test.design.data.car.toDisplay

/**
 * Applies a live VHAL snapshot. Demo values stay in place until the car
 * actually exposes one of the standard properties.
 */
fun VehicleUiState.withLiveSignals(signals: VehicleSignals): VehicleUiState {
    if (!signals.connected) {
        return if (isLive) {
            VehicleUiState(driveMode = driveMode, selectedSystemId = selectedSystemId)
        } else {
            this
        }
    }
    val display = signals.toDisplay()
    val percent = display.batteryPercent ?: display.fuelPercent
    return copy(
        isLive = true,
        energyLabel = display.energyLabel,
        batteryPercent = percent ?: 0,
        rangeMiles = display.rangeMiles ?: 0,
        rangeLabel = display.rangeLabel ?: "—",
        odometerMiles = display.odometerMiles ?: 0,
        odometerLabel = display.odometerLabel,
        isCharging = display.isCharging,
        chargeRateKw = display.chargeRateKw ?: 0f,
        speedLabel = display.speedLabel,
        gearLabel = display.gearLabel,
        outsideTempLabel = display.outsideTempLabel,
        parkingBrakeLabel = display.parkingBrakeLabel,
        chargePortLabel = display.chargePortLabel,
        tires = display.tires.map { it.toReading() },
        tiresAvailable = display.tiresAvailable,
        packCapacityKwh = display.packCapacityKwh,
    )
}

private fun DisplayedTire.toReading(): TireReading {
    val (id, shortLabel, label) = when (position) {
        WheelPosition.FrontLeft -> Triple("fl", "FL", "Front left")
        WheelPosition.FrontRight -> Triple("fr", "FR", "Front right")
        WheelPosition.RearLeft -> Triple("rl", "RL", "Rear left")
        WheelPosition.RearRight -> Triple("rr", "RR", "Rear right")
        WheelPosition.Other -> Triple("tire", "—", "Tire")
    }
    return TireReading(
        id = id,
        shortLabel = shortLabel,
        label = label,
        pressureLabel = pressureLabel,
        kiloPascals = kiloPascals,
        criticallyLow = criticallyLow,
    )
}

/** 320 kPa (~46 psi) fills the tire graphic. A typical placard pressure is about 240 kPa. */
internal const val TireVisualFullKpa = 320f

/** Passenger-car placard used as the mark on the tire graphic. */
internal const val TireRecommendedKpa = 241f

internal fun tireFillFraction(kiloPascals: Float): Float =
    (kiloPascals / TireVisualFullKpa).coerceIn(0f, 1f)
