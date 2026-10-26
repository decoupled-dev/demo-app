package com.test.design.presentation.ivi.climate.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.climate.TemperatureUnit
import com.test.design.presentation.ivi.climate.temperatureDialMetrics
import com.test.design.presentation.ivi.climate.formatTemperature
import com.test.design.presentation.ivi.climate.rememberClimateHaptics
import com.test.design.presentation.ivi.climate.toDisplayTemperature
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget
import com.test.design.theme.softCardShadow
import com.test.design.theme.climateColorScheme
import com.test.design.theme.temperatureToFraction
import com.test.design.theme.zoneCoolIntensity

@Composable
fun ClimateTemperatureSection(
    temperature: Float,
    dialShape: Shape,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    contentModifier: Modifier = Modifier,
    minTemperature: Float = 16f,
    maxTemperature: Float = 30f,
    temperatureStepCelsius: Float = 0.5f,
    temperatureStepFahrenheit: Float = 1f,
    minTemperatureFahrenheit: Float? = null,
    zoneLabel: String? = null,
    useZoneColorScheme: Boolean = false,
    temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
    onTemperatureSteps: ((Int) -> Unit)? = null,
    /** When set, snow only appears if this zone is cooler than (or equal to) the other. */
    otherZoneTemperature: Float? = null,
) {
    val fraction = temperatureToFraction(temperature, minTemperature, maxTemperature)
    val otherFraction = otherZoneTemperature?.let {
        temperatureToFraction(it, minTemperature, maxTemperature)
    } ?: fraction
    val coolIntensity = zoneCoolIntensity(fraction, otherFraction)
    val dialContent: @Composable () -> Unit = {
        ClimateTemperatureDialContent(
            temperature = temperature,
            dialShape = dialShape,
            onDecrease = onDecrease,
            onIncrease = onIncrease,
            compact = compact,
            contentModifier = contentModifier,
            minTemperature = minTemperature,
            maxTemperature = maxTemperature,
            temperatureStepCelsius = temperatureStepCelsius,
            temperatureStepFahrenheit = temperatureStepFahrenheit,
            minTemperatureFahrenheit = minTemperatureFahrenheit,
            zoneLabel = zoneLabel,
            temperatureUnit = temperatureUnit,
            onTemperatureSteps = onTemperatureSteps,
            coolIntensity = coolIntensity,
            modifier = modifier,
        )
    }

    if (useZoneColorScheme && !compact) {
        val zoneScheme = climateColorScheme(fraction, MaterialTheme.colorScheme)
        val motionSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
        val animatedContainer by animateColorAsState(
            targetValue = zoneScheme.primaryContainer,
            animationSpec = motionSpec,
            label = "zone_dial_container",
        )
        MaterialTheme(
            colorScheme = zoneScheme.copy(
                primaryContainer = animatedContainer,
            ),
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            motionScheme = MaterialTheme.motionScheme,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides zoneScheme.onBackground,
            ) {
                dialContent()
            }
        }
    } else {
        dialContent()
    }
}

@Composable
private fun ClimateTemperatureDialContent(
    temperature: Float,
    dialShape: Shape,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    compact: Boolean,
    contentModifier: Modifier,
    minTemperature: Float,
    maxTemperature: Float,
    temperatureStepCelsius: Float,
    temperatureStepFahrenheit: Float,
    minTemperatureFahrenheit: Float?,
    zoneLabel: String?,
    temperatureUnit: TemperatureUnit,
    onTemperatureSteps: ((Int) -> Unit)?,
    coolIntensity: Float,
    modifier: Modifier,
) {
    val dialWidth = 132.dp
    val dialHeight = 56.dp
    val compactButtonSize = 36.dp
    val compactIconSize = 18.dp
    val compactSpacing = 4.dp
    val displayLabel = formatTemperature(
        celsius = temperature,
        unit = temperatureUnit,
        celsiusStep = temperatureStepCelsius,
        minCelsius = minTemperature,
        minFahrenheit = minTemperatureFahrenheit,
        fahrenheitStep = temperatureStepFahrenheit,
    )
    val displaySortKey = temperature.toDisplayTemperature(
        unit = temperatureUnit,
        minCelsius = minTemperature,
        celsiusStep = temperatureStepCelsius,
        minFahrenheit = minTemperatureFahrenheit,
        fahrenheitStep = temperatureStepFahrenheit,
    )
    val canIncrease = temperature < maxTemperature - 0.01f
    val canDecrease = temperature > minTemperature + 0.01f
    val stepHandler = onTemperatureSteps ?: { steps ->
        repeat(abs(steps)) {
            if (steps > 0) onIncrease() else onDecrease()
        }
    }

    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(compactSpacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TemperatureAdjustButton(
                icon = Icons.Default.Remove,
                contentDescription = "Decrease temperature",
                onClick = onDecrease,
                enabled = canDecrease,
                size = compactButtonSize,
                iconSize = compactIconSize,
            )
            TemperatureDial(
                dialWidth = dialWidth,
                dialHeight = dialHeight,
                dialShape = dialShape,
                contentModifier = contentModifier,
                dragEnabled = false,
                onTemperatureSteps = stepHandler,
                coolIntensity = coolIntensity,
                showSnow = false,
            ) {
                AnimatedTemperatureCounter(
                    temperatureLabel = displayLabel,
                    compact = true,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    sortKey = displaySortKey,
                )
            }
            TemperatureAdjustButton(
                icon = Icons.Default.Add,
                contentDescription = "Increase temperature",
                onClick = onIncrease,
                enabled = canIncrease,
                size = compactButtonSize,
                iconSize = compactIconSize,
            )
        }
    } else {
        BoxWithConstraints(modifier = modifier) {
            val metrics = temperatureDialMetrics(
                zoneWidthDp = maxWidth.value,
                zoneHeightDp = maxHeight.value,
                showZoneLabel = zoneLabel != null,
            )
            val fillsHeight = maxHeight != Dp.Infinity
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillsHeight) Modifier.fillMaxHeight() else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                if (zoneLabel != null) {
                    Text(
                        text = zoneLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        metrics.spacing.dp,
                        Alignment.CenterHorizontally,
                    ),
                ) {
                    TemperatureAdjustButton(
                        icon = Icons.Default.Remove,
                        contentDescription = "Decrease temperature",
                        onClick = onDecrease,
                        enabled = canDecrease,
                        size = metrics.buttonSize.dp,
                        iconSize = metrics.iconSize.dp,
                    )
                    Box(
                        modifier = Modifier
                            .width(metrics.cardWidth.dp)
                            .softCardShadow(dialShape)
                            .clip(dialShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                horizontal = metrics.sidePadding.dp,
                                vertical = 0.dp,
                            )
                            .padding(
                                top = metrics.topPadding.dp,
                                bottom = metrics.bottomPadding.dp,
                            ),
                    ) {
                        TemperatureScale(
                            temperature = temperature,
                            temperatureLabel = displayLabel,
                            sortKey = displaySortKey,
                            minTemperature = minTemperature,
                            maxTemperature = maxTemperature,
                            temperatureStepCelsius = temperatureStepCelsius,
                            temperatureStepFahrenheit = temperatureStepFahrenheit,
                            minTemperatureFahrenheit = minTemperatureFahrenheit,
                            temperatureUnit = temperatureUnit,
                            onTemperatureSteps = stepHandler,
                            canvasHeight = metrics.canvasHeight.dp,
                            readoutFontSize = metrics.readoutFontSp.sp,
                            readoutLineHeight = metrics.readoutLineSp.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    TemperatureAdjustButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Increase temperature",
                        onClick = onIncrease,
                        enabled = canIncrease,
                        size = metrics.buttonSize.dp,
                        iconSize = metrics.iconSize.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TemperatureDial(
    dialWidth: Dp,
    dialHeight: Dp,
    dialShape: Shape,
    contentModifier: Modifier,
    dragEnabled: Boolean,
    onTemperatureSteps: (Int) -> Unit,
    coolIntensity: Float,
    showSnow: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = contentModifier
            .size(dialWidth, dialHeight)
            .clip(dialShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
            .temperatureVerticalDrag(
                enabled = dragEnabled,
                onTemperatureSteps = onTemperatureSteps,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showSnow) {
            CoolSnowflakeOverlay(
                coolIntensity = coolIntensity,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxSize(),
            )
        }
        content()
    }
}

@Composable
fun TemperatureAdjustButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = CarDesignTokens.MinTouchTarget,
    iconSize: Dp = CarDesignTokens.PrimaryIcon,
) {
    val haptics = rememberClimateHaptics()
    FilledIconButton(
        onClick = {
            haptics.tick()
            onClick()
        },
        enabled = enabled,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .carTouchTarget(),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

private fun abs(value: Int): Int = if (value < 0) -value else value
