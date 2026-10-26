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

/** Index of [celsius] on a scale that starts at [minCelsius] and moves by [step]. */
fun temperatureScaleIndex(celsius: Float, minCelsius: Float, step: Float): Int {
    if (step <= 0f) return 0
    return ((celsius - minCelsius) / step).roundToInt()
}

/**
 * Set-point steps for a horizontal scale drag.
 * Positive [dragPx] means the finger moved right, so a cooler value slides under the needle.
 */
fun temperatureScaleDragSteps(dragPx: Float, spacingPx: Float): Int {
    if (spacingPx <= 0f) return 0
    return -(dragPx / spacingPx).toInt()
}

/** Maps a horizontal touch on the fan track to a discrete blower step. */
fun fanSpeedFromTrackPosition(positionPx: Float, widthPx: Float, maxFanSpeed: Int): Int {
    if (maxFanSpeed <= 0 || widthPx <= 0f) return 0
    return (positionPx / widthPx * maxFanSpeed).roundToInt().coerceIn(0, maxFanSpeed)
}

/** Degrees between labeled ticks. Fahrenheit numbers are wider, so they sit further apart. */
fun temperatureScaleLabelGap(unit: TemperatureUnit): Int = when (unit) {
    TemperatureUnit.Celsius -> 4
    TemperatureUnit.Fahrenheit -> 8
}

/** Labeled ticks sit on whole degrees. Half-steps stay unmarked. */
fun temperatureScaleIsMajorTick(displayValue: Float, unit: TemperatureUnit): Boolean {
    val rounded = displayValue.roundToInt()
    if (kotlin.math.abs(displayValue - rounded) >= 0.05f) return false
    val gap = temperatureScaleLabelGap(unit)
    if (gap <= 0) return false
    return rounded % gap == 0
}

/**
 * Celsius is marked every 0.5°. Fahrenheit is marked in whole degrees,
 * one tick per VHAL ladder step.
 */
fun temperatureScaleSegmentStep(unit: TemperatureUnit, celsiusStep: Float): Float = when (unit) {
    TemperatureUnit.Celsius -> 0.5f
    TemperatureUnit.Fahrenheit -> celsiusStep.coerceAtLeast(0.1f)
}

/**
 * Tick height across the scale: full height under the needle, shorter toward
 * the corners, like the edge of a round knob. Returns a fraction in about 0.7–1.
 */
fun temperatureScaleTickHeightFraction(distanceFromCenter: Float, halfWidth: Float): Float {
    if (halfWidth <= 0f) return 1f
    val nx = (distanceFromCenter / halfWidth).coerceIn(0f, 1f)
    return kotlin.math.sqrt(1f - nx * nx * 0.5f)
}

/**
 * Brightness relative to the selected tick. Ticks left of the needle stay
 * fully lit. Ticks to the right are dimmer, but stay readable.
 */
fun temperatureScaleSideBrightness(offsetFromCenter: Float, halfWidth: Float): Float {
    if (halfWidth <= 0f) return 1f
    val t = (offsetFromCenter / halfWidth).coerceIn(-1f, 1f)
    if (t <= 0f) return 1f
    return (0.78f - t * 0.16f).coerceIn(0.58f, 1f)
}

/**
 * Sharp through the middle of the scale, then a smooth falloff to nothing
 * at the corners.
 */
fun temperatureScaleEdgeFade(
    distanceFromCenter: Float,
    halfWidth: Float,
    fadeStart: Float = 0.55f,
): Float {
    if (halfWidth <= 0f) return 1f
    val t = (distanceFromCenter / halfWidth).coerceIn(0f, 1f)
    val start = fadeStart.coerceIn(0f, 0.98f)
    if (t <= start) return 1f
    val u = (t - start) / (1f - start)
    val smooth = u * u * (3f - 2f * u)
    return (1f - smooth).coerceIn(0f, 1f)
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
            hasFrontDefrost || hasRearDefrost || hasRecirculation || hasSync || hasPower ||
            hasAuto
}

data class ClimateUiState(
    val temperatureCelsius: Float = 22f,
    val passengerTemperatureCelsius: Float = 21f,
    val minTemperature: Float = 16f,
    val maxTemperature: Float = 30f,
    /** VHAL Celsius increment (configArray[2] / 10), typically 0.5. */
    val temperatureStepCelsius: Float = 0.5f,
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
