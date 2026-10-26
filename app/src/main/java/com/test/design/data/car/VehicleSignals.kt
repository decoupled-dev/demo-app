package com.test.design.data.car

/**
 * Readings from the small set of VHAL properties that reference AAOS builds
 * usually expose: energy, range, odometer, speed, gear, parking brake,
 * outside temperature, and tire pressure.
 *
 * Units here are the VHAL canonical units. Display conversion happens in
 * [toDisplay].
 */
data class VehicleSignals(
    val connected: Boolean = false,
    val batteryRemainingWh: Float? = null,
    val batteryCapacityWh: Float? = null,
    val rangeMeters: Float? = null,
    val odometerKilometers: Float? = null,
    val chargeRateRaw: Float? = null,
    val chargePortConnected: Boolean? = null,
    val fuelMilliliters: Float? = null,
    val fuelCapacityMilliliters: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val gear: VehicleGearKind? = null,
    val parkingBrakeOn: Boolean? = null,
    val outsideCelsius: Float? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.Miles,
    val speedInMilesPerHour: Boolean = true,
    val pressureUnit: PressureUnit = PressureUnit.Kilopascal,
    val temperatureInFahrenheit: Boolean = false,
    val tires: List<TireSignal> = emptyList(),
    val tiresAvailable: Boolean = false,
)

enum class DistanceUnit { Miles, Kilometers }

enum class PressureUnit { Psi, Kilopascal, Bar }

enum class VehicleGearKind { Park, Reverse, Neutral, Drive }

enum class WheelPosition { FrontLeft, FrontRight, RearLeft, RearRight, Other }

data class TireSignal(
    val position: WheelPosition,
    val kiloPascals: Float,
    val criticallyLow: Boolean,
)

data class VehicleDisplay(
    val batteryPercent: Int?,
    val fuelPercent: Int?,
    val rangeMiles: Int?,
    val rangeLabel: String?,
    val odometerMiles: Int?,
    val odometerLabel: String?,
    val chargeRateKw: Float?,
    val isCharging: Boolean,
    val chargePortLabel: String?,
    val speedLabel: String?,
    val gearLabel: String?,
    val outsideTempLabel: String?,
    val parkingBrakeLabel: String?,
    val tires: List<DisplayedTire>,
    val tiresAvailable: Boolean,
    val energyLabel: String,
    val packCapacityKwh: Float?,
)

data class DisplayedTire(
    val position: WheelPosition,
    val pressureLabel: String,
    val kiloPascals: Float,
    val criticallyLow: Boolean,
)

private const val METERS_PER_MILE = 1609.344f
private const val KM_PER_MILE = 1.609344f
private const val KPA_PER_PSI = 6.894757f
private const val KPA_PER_BAR = 100f

/** Remaining Wh over pack capacity. A 0–100 reading with no capacity is used as-is. */
fun batteryPercent(remainingWh: Float?, capacityWh: Float?): Int? {
    if (remainingWh == null || remainingWh < 0f) return null
    if (capacityWh != null && capacityWh >= 500f) {
        return ((remainingWh / capacityWh) * 100f).rounded().coerceIn(0, 100)
    }
    if (remainingWh <= 100f) return remainingWh.rounded().coerceIn(0, 100)
    return null
}

/** Tank level over capacity. A 0–100 reading with no capacity is used as-is. */
fun fuelPercent(levelMilliliters: Float?, capacityMilliliters: Float?): Int? {
    if (levelMilliliters == null || levelMilliliters < 0f) return null
    if (capacityMilliliters != null && capacityMilliliters > 1f) {
        return ((levelMilliliters / capacityMilliliters) * 100f).rounded().coerceIn(0, 100)
    }
    if (levelMilliliters <= 100f) return levelMilliliters.rounded().coerceIn(0, 100)
    return null
}

/**
 * Charge rate is watts on some stacks and milliwatts on others.
 * Values at or above 1,000,000 are treated as milliwatts.
 */
fun chargeRateKilowatts(raw: Float?): Float? {
    if (raw == null) return null
    val kilowatts = if (kotlin.math.abs(raw) >= 1_000_000f) raw / 1_000_000f else raw / 1_000f
    return kilowatts
}

fun VehicleSignals.toDisplay(): VehicleDisplay {
    val battery = batteryPercent(batteryRemainingWh, batteryCapacityWh)
    val fuel = fuelPercent(fuelMilliliters, fuelCapacityMilliliters)
    val rateKw = chargeRateKilowatts(chargeRateRaw)
    val charging = rateKw != null && rateKw > 0.2f
    return VehicleDisplay(
        batteryPercent = battery,
        fuelPercent = fuel,
        rangeMiles = rangeMeters?.let { (it / METERS_PER_MILE).rounded() },
        rangeLabel = rangeMeters?.let { formatDistanceKm(it / 1000f, distanceUnit) },
        odometerMiles = odometerKilometers?.let { (it / KM_PER_MILE).rounded() },
        odometerLabel = odometerKilometers?.let { formatDistanceKm(it, distanceUnit) },
        chargeRateKw = rateKw?.takeIf { it > 0f },
        isCharging = charging,
        chargePortLabel = if (chargePortConnected == true && !charging) "Connected" else null,
        speedLabel = speedMetersPerSecond?.let { formatSpeed(it, speedInMilesPerHour) },
        gearLabel = gear?.label(),
        outsideTempLabel = outsideCelsius?.let { formatTemperature(it, temperatureInFahrenheit) },
        parkingBrakeLabel = parkingBrakeOn?.let { if (it) "On" else "Off" },
        tires = tires
            .sortedBy { wheelOrder.indexOf(it.position) }
            .map { tire ->
                DisplayedTire(
                    position = tire.position,
                    pressureLabel = formatPressure(tire.kiloPascals, pressureUnit),
                    kiloPascals = tire.kiloPascals,
                    criticallyLow = isTireCritical(tire.kiloPascals),
                )
            },
        tiresAvailable = tiresAvailable,
        energyLabel = if (battery == null && fuel != null) "Fuel" else "Battery",
        packCapacityKwh = batteryCapacityWh?.takeIf { it >= 500f }?.div(1000f),
    )
}

private fun isTireCritical(kiloPascals: Float): Boolean = kiloPascals <= 0f || kiloPascals < 180f

private fun formatDistanceKm(kilometers: Float, unit: DistanceUnit): String {
    val value = if (unit == DistanceUnit.Miles) kilometers / KM_PER_MILE else kilometers
    val suffix = if (unit == DistanceUnit.Miles) "mi" else "km"
    return "${value.rounded()} $suffix"
}

private fun formatSpeed(metersPerSecond: Float, milesPerHour: Boolean): String {
    val value = if (milesPerHour) metersPerSecond * 2.236936f else metersPerSecond * 3.6f
    val suffix = if (milesPerHour) "mph" else "km/h"
    return "${value.rounded()} $suffix"
}

private fun formatTemperature(celsius: Float, fahrenheit: Boolean): String {
    val value = if (fahrenheit) celsius * 9f / 5f + 32f else celsius
    val suffix = if (fahrenheit) "F" else "C"
    return "${value.rounded()}°$suffix"
}

private fun formatPressure(kiloPascals: Float, unit: PressureUnit): String = when (unit) {
    PressureUnit.Psi -> "${(kiloPascals / KPA_PER_PSI).rounded()} psi"
    PressureUnit.Bar -> "%.1f bar".format(java.util.Locale.US, kiloPascals / KPA_PER_BAR)
    PressureUnit.Kilopascal -> "${kiloPascals.rounded()} kPa"
}

private val wheelOrder = listOf(
    WheelPosition.FrontLeft,
    WheelPosition.FrontRight,
    WheelPosition.RearLeft,
    WheelPosition.RearRight,
    WheelPosition.Other,
)

private fun VehicleGearKind.label(): String = when (this) {
    VehicleGearKind.Park -> "P"
    VehicleGearKind.Reverse -> "R"
    VehicleGearKind.Neutral -> "N"
    VehicleGearKind.Drive -> "D"
}

private fun Float.rounded(): Int = kotlin.math.round(this).toInt()
