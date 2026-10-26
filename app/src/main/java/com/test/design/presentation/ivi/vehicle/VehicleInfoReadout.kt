package com.test.design.presentation.ivi.vehicle

import java.util.Locale
import kotlin.math.round

internal val VehicleGearOrder = listOf("N", "D", "R", "P")

internal data class VehicleInfoReadout(
    val remainingValue: String,
    val remainingUnit: String,
    val averageValue: String,
    val averageUnit: String,
    val capacityValue: String,
    val capacityUnit: String,
    val chargeFraction: Float,
    val speedValue: String,
    val speedUnit: String,
    val selectedGear: String?,
    val alertOn: Boolean,
    val batteryOn: Boolean,
)

internal fun VehicleUiState.toInfoReadout(): VehicleInfoReadout {
    val metric = rangeLabel.contains("km")
    val (rangeValue, rangeUnit) = splitMeasure(rangeLabel)
    val (speedValue, speedUnit) = speedLabel?.let(::splitMeasure)
        ?: ("0" to if (metric) "km/h" else "mph")
    val (averageValue, averageUnit) = if (isLive || efficiencyMpkWh <= 0f) {
        "—" to if (metric) "Wh/km" else "Wh/mi"
    } else {
        averageWattHours(efficiencyMpkWh, metric).toString() to if (metric) "Wh/km" else "Wh/mi"
    }
    val capacity = packCapacityKwh
        ?: if (!isLive) estimatedPackKwh(rangeMiles, batteryPercent, efficiencyMpkWh) else null
    return VehicleInfoReadout(
        remainingValue = rangeValue.ifBlank { "—" },
        remainingUnit = rangeUnit.ifBlank { if (metric) "km" else "mi" },
        averageValue = averageValue,
        averageUnit = averageUnit,
        capacityValue = capacity?.let { formatPackKwh(it) } ?: "—",
        capacityUnit = "kWh",
        chargeFraction = (batteryPercent / 100f).coerceIn(0f, 1f),
        speedValue = speedValue.ifBlank { "0" },
        speedUnit = speedUnit.ifBlank { if (metric) "km/h" else "mph" },
        selectedGear = gearLabel?.takeIf { it in VehicleGearOrder } ?: if (!isLive) "D" else null,
        alertOn = systems.any { it.health == SystemHealth.Warning } || tires.any { it.criticallyLow },
        batteryOn = isCharging || batteryPercent in 1..15,
    )
}

internal fun estimatedPackKwh(rangeMiles: Int, batteryPercent: Int, efficiencyMpkWh: Float): Float? {
    if (rangeMiles <= 0 || batteryPercent <= 0 || efficiencyMpkWh <= 0f) return null
    val energyRemainingKwh = rangeMiles.toDouble() / efficiencyMpkWh.toDouble()
    return (energyRemainingKwh / (batteryPercent / 100.0)).toFloat()
}

internal fun averageWattHours(efficiencyMpkWh: Float, metric: Boolean): Int {
    val whPerMile = 1000.0 / efficiencyMpkWh.toDouble()
    val value = if (metric) whPerMile / 1.609344 else whPerMile
    return round(value).toInt()
}

internal fun formatPackKwh(kwh: Float): String = String.format(Locale.US, "%.1f", kwh)

private fun splitMeasure(label: String): Pair<String, String> {
    val trimmed = label.trim()
    val space = trimmed.indexOf(' ')
    if (space <= 0) return trimmed to ""
    return trimmed.substring(0, space) to trimmed.substring(space + 1)
}
