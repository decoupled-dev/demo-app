package com.test.design.presentation.ivi.climate.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.climate.TemperatureUnit
import com.test.design.presentation.ivi.climate.formatTemperature
import com.test.design.presentation.ivi.climate.rememberClimateHaptics
import com.test.design.presentation.ivi.climate.toDisplayTemperature
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget

/**
 * The temperature is the object: a monumental figure, ghost +/−, and the unit.
 */
@Composable
fun ClimateMonument(
    temperatureCelsius: Float,
    unit: TemperatureUnit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onTemperatureSteps: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier,
    minTemperature: Float = 16f,
    maxTemperature: Float = 30f,
    temperatureStepCelsius: Float = 0.5f,
    temperatureStepFahrenheit: Float = 1f,
    minTemperatureFahrenheit: Float? = null,
    showAdjust: Boolean = true,
    compact: Boolean = false,
) {
    val label = formatTemperature(
        celsius = temperatureCelsius,
        unit = unit,
        celsiusStep = temperatureStepCelsius,
        minCelsius = minTemperature,
        minFahrenheit = minTemperatureFahrenheit,
        fahrenheitStep = temperatureStepFahrenheit,
    )
    val sortKey = temperatureCelsius.toDisplayTemperature(
        unit = unit,
        minCelsius = minTemperature,
        celsiusStep = temperatureStepCelsius,
        minFahrenheit = minTemperatureFahrenheit,
        fahrenheitStep = temperatureStepFahrenheit,
    )
    val numberStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = if (compact) 56.sp else 88.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1.5).sp,
        lineHeight = if (compact) 60.sp else 92.sp,
    )

    Row(
        modifier = modifier.temperatureVerticalDrag(onTemperatureSteps = onTemperatureSteps),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
    ) {
        if (showAdjust) {
            GhostAdjustButton(
                decrease = true,
                onClick = onDecrease,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedTemperatureCounter(
                temperatureLabel = label,
                color = MaterialTheme.colorScheme.onSurface,
                sortKey = sortKey,
                textStyle = numberStyle,
            )
            Text(
                text = unit.shortLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .carTouchTarget()
                    .clickable(onClick = onToggleUnit)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Temperature unit ${unit.shortLabel}"
                    },
            )
        }
        if (showAdjust) {
            GhostAdjustButton(
                decrease = false,
                onClick = onIncrease,
            )
        }
    }
}

@Composable
private fun GhostAdjustButton(
    decrease: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberClimateHaptics()
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val content = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    Icon(
        imageVector = if (decrease) Icons.Default.Remove else Icons.Default.Add,
        contentDescription = if (decrease) "Decrease temperature" else "Increase temperature",
        tint = content,
        modifier = Modifier
            .size(CarDesignTokens.MinTouchTarget)
            .clip(CircleShape)
            .border(1.dp, outline, CircleShape)
            .carTouchTarget()
            .clickable {
                haptics.tick()
                onClick()
            },
    )
}
