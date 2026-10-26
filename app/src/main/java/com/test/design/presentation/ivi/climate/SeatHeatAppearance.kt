package com.test.design.presentation.ivi.climate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Seat heat is always a warm signal, independent of the cabin temperature theme,
 * so Off stays muted and each step reads hotter.
 */
internal data class SeatHeatColors(
    val fill: Color,
    val accent: Color,
)

internal fun seatHeatStateLabel(level: Int, maxLevel: Int): String {
    val steps = maxLevel.coerceAtLeast(1)
    val clamped = level.coerceIn(0, steps)
    if (clamped == 0) return "Off"
    if (steps == 3) {
        return when (clamped) {
            1 -> "Low"
            2 -> "Med"
            else -> "High"
        }
    }
    return clamped.toString()
}

/** 0 when off, 1 at the highest step. */
internal fun seatHeatIntensity(level: Int, maxLevel: Int): Float {
    if (level <= 0 || maxLevel <= 0) return 0f
    return (level.toFloat() / maxLevel.toFloat()).coerceIn(0f, 1f)
}

internal fun seatHeatColors(level: Int, maxLevel: Int, lightCanvas: Boolean): SeatHeatColors {
    val intensity = seatHeatIntensity(level, maxLevel)
    val lowFill = if (lightCanvas) SeatHeatLowFillLight else SeatHeatLowFillDark
    val highFill = if (lightCanvas) SeatHeatHighFillLight else SeatHeatHighFillDark
    val lowAccent = if (lightCanvas) SeatHeatLowAccentLight else SeatHeatLowAccentDark
    val highAccent = if (lightCanvas) SeatHeatHighAccentLight else SeatHeatHighAccentDark
    return SeatHeatColors(
        fill = lerp(lowFill, highFill, intensity),
        accent = lerp(lowAccent, highAccent, intensity),
    )
}

private val SeatHeatLowFillLight = Color(0xFFFF7043)
private val SeatHeatHighFillLight = Color(0xFFD84315)
private val SeatHeatLowFillDark = Color(0xFFFF7043)
private val SeatHeatHighFillDark = Color(0xFFFF3D00)

private val SeatHeatLowAccentLight = Color(0xFFE65100)
private val SeatHeatHighAccentLight = Color(0xFFBF360C)
private val SeatHeatLowAccentDark = Color(0xFFFFAB91)
private val SeatHeatHighAccentDark = Color(0xFFFF6E40)
