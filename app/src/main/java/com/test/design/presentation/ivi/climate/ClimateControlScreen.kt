package com.test.design.presentation.ivi.climate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.test.design.presentation.common.ScreenBackground
import com.test.design.presentation.ivi.climate.components.CabinWeatherLayer
import com.test.design.presentation.ivi.climate.components.ClimateAirflowGlyphs
import com.test.design.presentation.ivi.climate.components.ClimateFanTrack
import com.test.design.presentation.ivi.climate.components.ClimateHvacIcons
import com.test.design.presentation.ivi.climate.components.ClimateMonument
import com.test.design.presentation.ivi.climate.components.QuietSymbol
import com.test.design.presentation.ivi.climate.components.seatHeatTint
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContentSharedElement
import com.test.design.presentation.ivi.dashboard.widgetControlsSharedElement
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.theme.AdaptiveLayout
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget
import com.test.design.theme.climateAmbientColor
import com.test.design.theme.climateColorScheme
import com.test.design.theme.temperatureToFraction
import com.test.design.theme.zoneCoolIntensity

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ClimateControlScreen(
    uiState: ClimateUiState,
    activeTemperature: Float,
    onEvent: (ClimateEvent) -> Unit,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val driverFraction = temperatureToFraction(
        uiState.temperatureCelsius,
        uiState.minTemperature,
        uiState.maxTemperature,
    )
    val passengerFraction = temperatureToFraction(
        uiState.passengerTemperatureCelsius,
        uiState.minTemperature,
        uiState.maxTemperature,
    )
    val blendFraction = (driverFraction + passengerFraction) / 2f
    val driverCoolIntensity = zoneCoolIntensity(driverFraction, passengerFraction)
    val passengerCoolIntensity = zoneCoolIntensity(passengerFraction, driverFraction)
    val dynamicScheme = climateColorScheme(blendFraction, MaterialTheme.colorScheme)
    val motionSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val driverGlow by animateColorAsState(
        climateAmbientColor(driverFraction),
        animationSpec = motionSpec,
        label = "driver_glow",
    )
    val passengerGlow by animateColorAsState(
        climateAmbientColor(passengerFraction),
        animationSpec = motionSpec,
        label = "passenger_glow",
    )
    val haptics = rememberClimateHaptics()
    val autoOn = uiState.airflowMode == AirflowMode.Auto
    val powerDim = if (uiState.isPowerOn) 1f else 0.38f

    MaterialTheme(
        colorScheme = dynamicScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        motionScheme = MaterialTheme.motionScheme,
    ) {
        CompositionLocalProvider(LocalContentColor provides dynamicScheme.onBackground) {
            Box(
                modifier = widgetContainerTransform(
                    widget = DashboardWidget.Climate,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = modifier.fillMaxSize(),
                ),
            ) {
                ScreenBackground()
                CabinWeatherLayer(
                    driverFraction = driverFraction,
                    passengerFraction = passengerFraction,
                    driverGlow = driverGlow,
                    passengerGlow = passengerGlow,
                    driverCool = driverCoolIntensity,
                    passengerCool = passengerCoolIntensity,
                    fanSpeed = uiState.fanSpeed,
                    maxFanSpeed = uiState.maxFanSpeed,
                    airflowMode = uiState.airflowMode,
                    isRecirculationOn = uiState.isRecirculationOn,
                    isFrontDefrostOn = uiState.isFrontDefrostOn,
                    isAcEnabled = uiState.isAcEnabled,
                    isPowerOn = uiState.isPowerOn,
                    isSyncEnabled = uiState.isSyncEnabled,
                    isAutoOn = autoOn,
                    modifier = Modifier.fillMaxSize(),
                )
                AdaptiveLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = CarDesignTokens.ContentPadding, vertical = 16.dp),
                ) { layout ->
                    val portrait = !layout.useSideBySide
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (portrait) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(CarDesignTokens.MinTouchTarget)
                                .carTouchTarget(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (portrait) Modifier else Modifier.weight(1f))
                                .graphicsLayer { alpha = powerDim },
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (uiState.capabilities.hasDriverTemp ||
                                uiState.capabilities.hasPassengerTemp
                            ) {
                                TemperaturePlane(
                                    uiState = uiState,
                                    compact = portrait,
                                    onEvent = onEvent,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            if (uiState.capabilities.hasFanSpeed) {
                                ClimateFanTrack(
                                    fanSpeed = uiState.fanSpeed,
                                    maxFanSpeed = uiState.maxFanSpeed,
                                    onSpeedSelected = { onEvent(ClimateEvent.SetFanSpeed(it)) },
                                    powerOn = uiState.isPowerOn,
                                    modifier = widgetControlsSharedElement(
                                        widget = DashboardWidget.Climate,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = if (portrait) 0.dp else 48.dp),
                                    ),
                                )
                            }

                            AnimatedVisibility(
                                visible = uiState.capabilities.hasAirflowControls && !autoOn,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ClimateAirflowGlyphs(
                                    selectedMode = uiState.airflowMode,
                                    modes = uiState.capabilities.availableAirflowModes
                                        .filter { it != AirflowMode.Auto },
                                    onModeSelected = { onEvent(ClimateEvent.SelectAirflow(it)) },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        ClimateSymbolRow(
                            uiState = uiState,
                            onEvent = onEvent,
                            hapticsTick = { haptics.tick() },
                            hapticsToggle = { haptics.toggle(it) },
                            compact = portrait,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.TemperaturePlane(
    uiState: ClimateUiState,
    compact: Boolean,
    onEvent: (ClimateEvent) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val showDriver = uiState.capabilities.hasDriverTemp
    val showPassenger = uiState.capabilities.hasPassengerTemp
    val synced = uiState.isSyncEnabled && showDriver && showPassenger
    val gap by animateDpAsState(
        targetValue = if (synced) 8.dp else 40.dp,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "zone_gap",
    )

    val driver = @Composable { extra: Modifier ->
        ClimateMonument(
            temperatureCelsius = uiState.temperatureCelsius,
            unit = uiState.temperatureUnit,
            onDecrease = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, -1)) },
            onIncrease = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, +1)) },
            onTemperatureSteps = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, it)) },
            onToggleUnit = { onEvent(ClimateEvent.ToggleTemperatureUnit) },
            minTemperature = uiState.minTemperature,
            maxTemperature = uiState.maxTemperature,
            temperatureStepCelsius = uiState.temperatureStepCelsius,
            temperatureStepFahrenheit = uiState.temperatureStepFahrenheit,
            minTemperatureFahrenheit = uiState.minTemperatureFahrenheit,
            compact = compact,
            modifier = extra,
        )
    }
    val passenger = @Composable {
        ClimateMonument(
            temperatureCelsius = uiState.passengerTemperatureCelsius,
            unit = uiState.temperatureUnit,
            onDecrease = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Passenger, -1)) },
            onIncrease = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Passenger, +1)) },
            onTemperatureSteps = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Passenger, it)) },
            onToggleUnit = { onEvent(ClimateEvent.ToggleTemperatureUnit) },
            minTemperature = uiState.minTemperature,
            maxTemperature = uiState.maxTemperature,
            temperatureStepCelsius = uiState.temperatureStepCelsius,
            temperatureStepFahrenheit = uiState.temperatureStepFahrenheit,
            minTemperatureFahrenheit = uiState.minTemperatureFahrenheit,
            showAdjust = !synced,
            compact = compact,
        )
    }

    if (showDriver && showPassenger) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            driver(
                widgetContentSharedElement(
                    widget = DashboardWidget.Climate,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier,
                ),
            )
            Box(
                modifier = Modifier
                    .width(gap)
                    .height(CarDesignTokens.MinTouchTarget)
                    .carTouchTarget()
                    .semantics {
                        role = Role.Switch
                        contentDescription = if (synced) "Zones synced" else "Sync zones"
                    }
                    .clickable { onEvent(ClimateEvent.ToggleSync) },
            )
            passenger()
        }
    } else if (showDriver) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            driver(
                widgetContentSharedElement(
                    widget = DashboardWidget.Climate,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier,
                ),
            )
        }
    } else {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            passenger()
        }
    }
}

@Composable
private fun ClimateSymbolRow(
    uiState: ClimateUiState,
    onEvent: (ClimateEvent) -> Unit,
    hapticsTick: () -> Unit,
    hapticsToggle: (Boolean) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val caps = uiState.capabilities
    val rest = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.then(
            if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier,
        ),
        horizontalArrangement = if (compact) {
            Arrangement.spacedBy(4.dp)
        } else {
            Arrangement.SpaceEvenly
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (caps.hasSeatHeat) {
            QuietSymbol(
                icon = ClimateHvacIcons.SeatHeat,
                contentDescription = "Driver seat heat",
                selected = uiState.seatHeatLevel > 0,
                tint = seatHeatTint(uiState.seatHeatLevel, uiState.maxSeatHeatLevel, rest),
                onClick = {
                    hapticsTick()
                    onEvent(ClimateEvent.CycleSeatHeat)
                },
            )
        }
        if (caps.hasPassengerSeatHeat) {
            QuietSymbol(
                icon = ClimateHvacIcons.SeatHeatPassenger,
                contentDescription = "Passenger seat heat",
                selected = uiState.passengerSeatHeatLevel > 0,
                tint = seatHeatTint(
                    uiState.passengerSeatHeatLevel,
                    uiState.maxSeatHeatLevel,
                    rest,
                ),
                onClick = {
                    hapticsTick()
                    onEvent(ClimateEvent.CyclePassengerSeatHeat)
                },
            )
        }
        if (caps.hasSteeringHeat) {
            QuietSymbol(
                icon = ClimateHvacIcons.SteeringHeat,
                contentDescription = "Wheel heat",
                selected = uiState.steeringHeatLevel > 0,
                tint = seatHeatTint(uiState.steeringHeatLevel, uiState.maxSteeringHeatLevel, rest),
                onClick = {
                    hapticsTick()
                    onEvent(ClimateEvent.CycleSteeringHeat)
                },
            )
        }
        if (caps.hasSeatVent) {
            QuietSymbol(
                icon = ClimateHvacIcons.SeatVent,
                contentDescription = "Seat vent",
                selected = uiState.seatVentLevel > 0,
                onClick = {
                    hapticsTick()
                    onEvent(ClimateEvent.CycleSeatVent)
                },
            )
        }
        if (caps.hasFrontDefrost) {
            QuietSymbol(
                icon = ClimateHvacIcons.FrontDefrost,
                contentDescription = "Front defrost",
                selected = uiState.isFrontDefrostOn,
                onClick = {
                    hapticsToggle(!uiState.isFrontDefrostOn)
                    onEvent(ClimateEvent.ToggleFrontDefrost)
                },
            )
        }
        if (caps.hasRearDefrost) {
            QuietSymbol(
                icon = ClimateHvacIcons.RearDefrost,
                contentDescription = "Rear defrost",
                selected = uiState.isRearDefrostOn,
                onClick = {
                    hapticsToggle(!uiState.isRearDefrostOn)
                    onEvent(ClimateEvent.ToggleRearDefrost)
                },
            )
        }
        if (caps.hasRecirculation) {
            QuietSymbol(
                icon = ClimateHvacIcons.Recirculation,
                contentDescription = "Recirculation",
                selected = uiState.isRecirculationOn,
                onClick = {
                    hapticsToggle(!uiState.isRecirculationOn)
                    onEvent(ClimateEvent.ToggleRecirculation)
                },
            )
        }
        if (caps.hasAc) {
            QuietSymbol(
                icon = ClimateHvacIcons.Ac,
                contentDescription = "A/C",
                selected = uiState.isAcEnabled,
                onClick = {
                    hapticsToggle(!uiState.isAcEnabled)
                    onEvent(ClimateEvent.ToggleAc)
                },
            )
        }
        if (caps.hasAuto) {
            QuietSymbol(
                icon = ClimateHvacIcons.Auto,
                contentDescription = "Auto climate",
                selected = uiState.airflowMode == AirflowMode.Auto,
                onClick = {
                    val next = if (uiState.airflowMode == AirflowMode.Auto) {
                        AirflowMode.Face
                    } else {
                        AirflowMode.Auto
                    }
                    hapticsToggle(next == AirflowMode.Auto)
                    onEvent(ClimateEvent.SelectAirflow(next))
                },
            )
        }
        if (caps.hasPower) {
            QuietSymbol(
                icon = ClimateHvacIcons.Power,
                contentDescription = "Climate power",
                selected = uiState.isPowerOn,
                onClick = {
                    hapticsToggle(!uiState.isPowerOn)
                    onEvent(ClimateEvent.TogglePower)
                },
            )
        }
    }
}
