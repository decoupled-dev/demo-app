package com.test.design.presentation.ivi.climate

import kotlin.math.roundToInt

enum class AirflowMode(val label: String) {
    Face("Face"),
    BiLevel("Bi-level"),
    Feet("Feet"),
    FeetDefrost("Defrost"),
    Auto("Auto"),
}

enum class ClimateZone(val label: String) {
    Driver("Driver"),
    Passenger("Passenger"),
}

enum class TemperatureUnit(val symbol: String, val shortLabel: String) {
    Celsius("°C", "°C"),
    Fahrenheit("°F", "°F"),
}

/**
 * CarSystemUI Fahrenheit display (TemperatureControlView.celsiusToFahrenheit):
 * `minF + round((C - minC) / stepC) * stepF`
 * — not `C×9/5+32`, which drifts ~1°F on half-degree HVAC steps.
 */
fun celsiusToFahrenheitDisplay(
    celsius: Float,
    minCelsius: Float,
    celsiusStep: Float,
    minFahrenheit: Float,
    fahrenheitStep: Float,
): Float {
    if (celsiusStep <= 0f || fahrenheitStep <= 0f) {
        return celsius * 9f / 5f + 32f
    }
    val numIncrements = ((celsius - minCelsius) / celsiusStep).roundToInt()
    return fahrenheitStep * numIncrements + minFahrenheit
}

/** Converts stored Celsius to the numeric value shown for [unit]. */
fun Float.toDisplayTemperature(
    unit: TemperatureUnit,
    minCelsius: Float = 16f,
    celsiusStep: Float = 0.5f,
    minFahrenheit: Float? = null,
    fahrenheitStep: Float = 1f,
): Float = when (unit) {
    TemperatureUnit.Celsius -> this
    TemperatureUnit.Fahrenheit -> {
        val minF = minFahrenheit ?: (minCelsius * 9f / 5f + 32f)
        celsiusToFahrenheitDisplay(
            celsius = this,
            minCelsius = minCelsius,
            celsiusStep = celsiusStep,
            minFahrenheit = minF,
            fahrenheitStep = fahrenheitStep,
        )
    }
}

/**
 * Formats HVAC set-point for display the way CarSystemUI does:
 * - Celsius: one decimal when the VHAL step is fractional (e.g. `22.5°C`)
 * - Fahrenheit: discrete VHAL ladder (`minF + n×stepF`), not `C×9/5+32`
 */
fun formatTemperature(
    celsius: Float,
    unit: TemperatureUnit,
    celsiusStep: Float = 0.5f,
    minCelsius: Float = 16f,
    minFahrenheit: Float? = null,
    fahrenheitStep: Float = 1f,
): String {
    val text = when (unit) {
        TemperatureUnit.Celsius -> {
            if (celsiusStep < 0.999f) {
                String.format(java.util.Locale.US, "%.1f", celsius)
            } else {
                celsius.roundToInt().toString()
            }
        }
        TemperatureUnit.Fahrenheit -> {
            celsius.toDisplayTemperature(
                unit = unit,
                minCelsius = minCelsius,
                celsiusStep = celsiusStep,
                minFahrenheit = minFahrenheit,
                fahrenheitStep = fahrenheitStep,
            ).roundToInt().toString()
        }
    }
    return "$text${unit.symbol}"
}

/** Display text without unit symbol (system-bar / dial numeral). */
fun formatTemperatureValue(
    celsius: Float,
    unit: TemperatureUnit,
    celsiusStep: Float = 0.5f,
    minCelsius: Float = 16f,
    minFahrenheit: Float? = null,
    fahrenheitStep: Float = 1f,
): String = formatTemperature(
    celsius = celsius,
    unit = unit,
    celsiusStep = celsiusStep,
    minCelsius = minCelsius,
    minFahrenheit = minFahrenheit,
    fahrenheitStep = fahrenheitStep,
).removeSuffix(unit.symbol)

fun ClimateUiState.formatTemperature(celsius: Float): String = formatTemperature(
    celsius = celsius,
    unit = temperatureUnit,
    celsiusStep = temperatureStepCelsius,
    minCelsius = minTemperature,
    minFahrenheit = minTemperatureFahrenheit,
    fahrenheitStep = temperatureStepFahrenheit,
)

fun ClimateUiState.formatTemperatureValue(celsius: Float): String =
    formatTemperature(celsius).removeSuffix(temperatureUnit.symbol)

fun ClimateUiState.toDisplayTemperature(celsius: Float): Float = celsius.toDisplayTemperature(
    unit = temperatureUnit,
    minCelsius = minTemperature,
    celsiusStep = temperatureStepCelsius,
    minFahrenheit = minTemperatureFahrenheit,
    fahrenheitStep = temperatureStepFahrenheit,
)

fun snapTemperature(value: Float, min: Float, max: Float, step: Float): Float {
    if (step <= 0f) return value.coerceIn(min, max)
    val steps = ((value - min) / step).roundToInt()
    return (min + steps * step).coerceIn(min, max)
}

/**
 * Next set-point after [deltaSteps] UI steps.
 * CarSystemUI always adjusts the Celsius set-point by [celsiusStep], even when
 * the bar shows Fahrenheit (each C step maps to one F step on the VHAL ladder).
 */
@Suppress("UNUSED_PARAMETER")
fun stepTemperature(
    currentCelsius: Float,
    deltaSteps: Int,
    unit: TemperatureUnit,
    minCelsius: Float,
    maxCelsius: Float,
    celsiusStep: Float,
    fahrenheitStep: Float = 1f,
): Float {
    if (deltaSteps == 0) return currentCelsius.coerceIn(minCelsius, maxCelsius)
    return snapTemperature(
        currentCelsius + deltaSteps * celsiusStep,
        minCelsius,
        maxCelsius,
        celsiusStep,
    )
}

/**
 * Which climate controls the vehicle VHAL actually exposes.
 * When [isLive] is false the UI keeps the full simulated surface.
 */
data class ClimateCapabilities(
    val hasDriverTemp: Boolean = true,
    val hasPassengerTemp: Boolean = true,
    val hasFanSpeed: Boolean = true,
    val hasFanDirection: Boolean = true,
    val hasAuto: Boolean = true,
    val hasAc: Boolean = true,
    val hasSync: Boolean = true,
    val hasRecirculation: Boolean = true,
    val hasFrontDefrost: Boolean = true,
    val hasRearDefrost: Boolean = true,
    val hasSeatHeat: Boolean = true,
    val hasPassengerSeatHeat: Boolean = true,
    val hasSteeringHeat: Boolean = true,
    val hasSeatVent: Boolean = true,
    val hasPower: Boolean = true,
    val hasTemperatureUnit: Boolean = false,
) {
    val hasAnyHvacControl: Boolean
        get() = hasDriverTemp || hasPassengerTemp || hasFanSpeed || hasFanDirection ||
            hasAuto || hasAc || hasSync || hasRecirculation || hasFrontDefrost ||
            hasRearDefrost || hasSeatHeat || hasPassengerSeatHeat || hasSteeringHeat ||
            hasSeatVent || hasPower

    val hasAirflowControls: Boolean
        get() = hasFanDirection || hasAuto

    val availableAirflowModes: List<AirflowMode>
        get() = buildList {
            if (hasFanDirection) {
                add(AirflowMode.Face)
                add(AirflowMode.BiLevel)
                add(AirflowMode.Feet)
                add(AirflowMode.FeetDefrost)
            }
        }

    val hasComfortControls: Boolean
        get() = hasSeatHeat || hasPassengerSeatHeat || hasSteeringHeat || hasSeatVent ||
            hasFrontDefrost || hasRearDefrost || hasRecirculation || hasAc || hasSync ||
            hasPower || hasAuto
}

data class ClimateUiState(
    val temperatureCelsius: Float = 22f,
    val passengerTemperatureCelsius: Float = 21f,
    val minTemperature: Float = 16f,
    val maxTemperature: Float = 30f,
    /** VHAL Celsius increment (configArray[2] / 10), typically 0.5. */
    val temperatureStepCelsius: Float = 1f,
    /** VHAL Fahrenheit increment (configArray[5] / 10), typically 1. */
    val temperatureStepFahrenheit: Float = 1f,
    /**
     * VHAL Fahrenheit ladder minimum (configArray[3] / 10).
     * Null falls back to `minC×9/5+32` for simulated (non-live) mode.
     */
    val minTemperatureFahrenheit: Float? = null,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
    val airflowMode: AirflowMode = AirflowMode.Face,
    val fanSpeed: Int = 3,
    val maxFanSpeed: Int = 5,
    val isAcEnabled: Boolean = true,
    val isPowerOn: Boolean = true,
    val isSyncEnabled: Boolean = true,
    val isRecirculationOn: Boolean = false,
    val isFrontDefrostOn: Boolean = false,
    val isRearDefrostOn: Boolean = false,
    val activeZone: ClimateZone = ClimateZone.Driver,
    val seatHeatLevel: Int = 1,
    val passengerSeatHeatLevel: Int = 0,
    val maxSeatHeatLevel: Int = 3,
    val steeringHeatLevel: Int = 0,
    val maxSteeringHeatLevel: Int = 3,
    val seatVentLevel: Int = 0,
    val maxSeatVentLevel: Int = 3,
    /** True when values are driven by [android.car.hardware.property.CarPropertyManager]. */
    val isLive: Boolean = false,
    val capabilities: ClimateCapabilities = ClimateCapabilities(),
)

sealed interface ClimateEvent {
    data object IncreaseTemperature : ClimateEvent
    data object DecreaseTemperature : ClimateEvent
    data class AdjustZoneTemperature(val zone: ClimateZone, val delta: Int) : ClimateEvent
    data class SelectAirflow(val mode: AirflowMode) : ClimateEvent
    data object ToggleAc : ClimateEvent
    data object TogglePower : ClimateEvent
    data object ToggleSync : ClimateEvent
    data object ToggleRecirculation : ClimateEvent
    data object ToggleFrontDefrost : ClimateEvent
    data object ToggleRearDefrost : ClimateEvent
    data object ToggleTemperatureUnit : ClimateEvent
    data class SelectZone(val zone: ClimateZone) : ClimateEvent
    data class SetFanSpeed(val speed: Int) : ClimateEvent
    data object CycleSeatHeat : ClimateEvent
    data object CyclePassengerSeatHeat : ClimateEvent
    data object CycleSteeringHeat : ClimateEvent
    data object CycleSeatVent : ClimateEvent
    data object IncreaseSeatHeat : ClimateEvent
    data object DecreaseSeatHeat : ClimateEvent
}
