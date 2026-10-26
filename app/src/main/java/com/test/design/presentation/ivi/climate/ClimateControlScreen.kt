package com.test.design.presentation.ivi.climate

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.test.design.presentation.common.ScreenBackground
import com.test.design.presentation.ivi.climate.components.ClimateComfortControlsCard
import com.test.design.presentation.ivi.climate.components.ClimateBlowerCard
import com.test.design.presentation.ivi.climate.components.ClimateHvacIcons
import com.test.design.presentation.ivi.climate.components.ClimateTemperatureSection
import com.test.design.presentation.ivi.climate.components.CoolSnowflakeOverlay
import com.test.design.presentation.ivi.common.WidgetScreenHeader
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.presentation.ivi.dashboard.widgetContentSharedElement
import com.test.design.presentation.ivi.dashboard.widgetControlsSharedElement
import com.test.design.presentation.ivi.navigation.components.mapChromeLayer
import com.test.design.theme.AdaptiveLayout
import com.test.design.theme.CarBackgroundTokens
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.WindowLayoutInfo
import com.test.design.theme.climateAmbientColor
import com.test.design.theme.climateColorScheme
import com.test.design.theme.carTouchTarget
import com.test.design.theme.rememberClimateDialShape
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
    showBack: Boolean = false,
    applyWindowInsets: Boolean = true,
    contentPadding: Dp = CarDesignTokens.ContentPadding,
    compact: Boolean = false,
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
    val lightCanvas = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val dynamicScheme = climateColorScheme(blendFraction, MaterialTheme.colorScheme)
    val edgeTintAlpha = if (lightCanvas) 0.16f else CarBackgroundTokens.DetailTintAlpha
    val midOverlayAlpha = if (lightCanvas) 0.10f else CarBackgroundTokens.DetailOverlayAlpha
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
    val dialShape = rememberClimateDialShape(acEnabled = uiState.isAcEnabled)
    val haptics = rememberClimateHaptics()

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
                    modifier = modifier
                        .fillMaxSize()
                        .mapChromeLayer(),
                ),
            ) {
                ScreenBackground()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to driverGlow.copy(alpha = edgeTintAlpha),
                                    0.5f to MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                        alpha = midOverlayAlpha,
                                    ),
                                    1f to passengerGlow.copy(alpha = edgeTintAlpha),
                                ),
                            ),
                        ),
                )
                if (!compact) Row(modifier = Modifier.fillMaxSize()) {
                    CoolSnowflakeOverlay(
                        coolIntensity = driverCoolIntensity,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        flakeCount = 18,
                        sizeScale = 1.3f,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    CoolSnowflakeOverlay(
                        coolIntensity = passengerCoolIntensity,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        flakeCount = 18,
                        sizeScale = 1.3f,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (applyWindowInsets) {
                                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                            } else {
                                Modifier
                            },
                        )
                        .padding(contentPadding),
                ) {
                    AdaptiveLayout(modifier = Modifier.fillMaxSize()) { layout ->
                        val scrollPortrait = !layout.useSideBySide
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (scrollPortrait) {
                                        Modifier.verticalScroll(rememberScrollState())
                                    } else {
                                        Modifier.fillMaxSize()
                                    },
                                ),
                            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 20.dp),
                        ) {
                            WidgetScreenHeader(
                                widget = DashboardWidget.Climate,
                                onBack = onBack,
                                animatedVisibilityScope = animatedVisibilityScope,
                                showBack = showBack,
                                trailingContent = {
                                    val caps = uiState.capabilities
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        FilterChip(
                                            selected = true,
                                            onClick = {
                                                haptics.tick()
                                                onEvent(ClimateEvent.ToggleTemperatureUnit)
                                            },
                                            label = {
                                                Text(
                                                    text = uiState.temperatureUnit.shortLabel,
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            },
                                            modifier = Modifier.carTouchTarget(),
                                        )
                                        if (caps.hasAc) {
                                            Icon(
                                                painter = painterResource(ClimateHvacIcons.Ac),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(CarDesignTokens.TertiaryIcon),
                                            )
                                            Text(
                                                text = "A/C",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Switch(
                                                checked = uiState.isAcEnabled,
                                                onCheckedChange = { enabled ->
                                                    haptics.toggle(enabled)
                                                    onEvent(ClimateEvent.ToggleAc)
                                                },
                                            )
                                        }
                                    }
                                },
                            )

                            if (uiState.capabilities.hasDriverTemp ||
                                uiState.capabilities.hasPassengerTemp
                            ) {
                                DualZoneTemperatureBand(
                                    layout = layout,
                                    uiState = uiState,
                                    dialShape = dialShape,
                                    onEvent = onEvent,
                                    compact = compact,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    modifier = Modifier
                                        .then(
                                            if (scrollPortrait) {
                                                Modifier.fillMaxWidth()
                                            } else {
                                                Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                            },
                                        ),
                                )
                            }

                            if (uiState.capabilities.hasFanSpeed ||
                                uiState.capabilities.hasAirflowControls
                            ) {
                                ClimateAirBand(
                                    layout = layout,
                                    uiState = uiState,
                                    onEvent = onEvent,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }

                            if (uiState.capabilities.hasComfortControls) {
                                val caps = uiState.capabilities
                                ClimateComfortControlsCard(
                                    seatHeatLevel = uiState.seatHeatLevel,
                                    maxSeatHeatLevel = uiState.maxSeatHeatLevel,
                                    passengerSeatHeatLevel = uiState.passengerSeatHeatLevel,
                                    steeringHeatLevel = uiState.steeringHeatLevel,
                                    maxSteeringHeatLevel = uiState.maxSteeringHeatLevel,
                                    seatVentLevel = uiState.seatVentLevel,
                                    maxSeatVentLevel = uiState.maxSeatVentLevel,
                                    isFrontDefrostOn = uiState.isFrontDefrostOn,
                                    isRearDefrostOn = uiState.isRearDefrostOn,
                                    isRecirculationOn = uiState.isRecirculationOn,
                                    isSyncEnabled = uiState.isSyncEnabled,
                                    isAcEnabled = uiState.isAcEnabled,
                                    isPowerOn = uiState.isPowerOn,
                                    isAutoOn = uiState.airflowMode == AirflowMode.Auto,
                                    onCycleSeatHeat = { onEvent(ClimateEvent.CycleSeatHeat) },
                                    onCyclePassengerSeatHeat = {
                                        onEvent(ClimateEvent.CyclePassengerSeatHeat)
                                    },
                                    onCycleSteeringHeat = { onEvent(ClimateEvent.CycleSteeringHeat) },
                                    onCycleSeatVent = { onEvent(ClimateEvent.CycleSeatVent) },
                                    onToggleFrontDefrost = { onEvent(ClimateEvent.ToggleFrontDefrost) },
                                    onToggleRearDefrost = { onEvent(ClimateEvent.ToggleRearDefrost) },
                                    onToggleRecirculation = { onEvent(ClimateEvent.ToggleRecirculation) },
                                    onToggleSync = { onEvent(ClimateEvent.ToggleSync) },
                                    onTogglePower = { onEvent(ClimateEvent.TogglePower) },
                                    onToggleAuto = {
                                        val next = if (uiState.airflowMode == AirflowMode.Auto) {
                                            AirflowMode.Face
                                        } else {
                                            AirflowMode.Auto
                                        }
                                        onEvent(ClimateEvent.SelectAirflow(next))
                                    },
                                    showSeatHeat = caps.hasSeatHeat,
                                    showPassengerSeatHeat = caps.hasPassengerSeatHeat,
                                    showSteeringHeat = caps.hasSteeringHeat,
                                    showSeatVent = caps.hasSeatVent,
                                    showFrontDefrost = caps.hasFrontDefrost,
                                    showRearDefrost = caps.hasRearDefrost,
                                    showRecirculation = caps.hasRecirculation,
                                    showSync = caps.hasSync,
                                    showPower = caps.hasPower,
                                    showAuto = caps.hasAuto,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.DualZoneTemperatureBand(
    layout: WindowLayoutInfo,
    uiState: ClimateUiState,
    dialShape: androidx.compose.ui.graphics.Shape,
    onEvent: (ClimateEvent) -> Unit,
    compact: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val showDriver = uiState.capabilities.hasDriverTemp
    val showPassenger = uiState.capabilities.hasPassengerTemp
    val driverDial = @Composable { dialModifier: Modifier ->
        ClimateTemperatureSection(
            temperature = uiState.temperatureCelsius,
            dialShape = dialShape,
            onDecrease = {
                onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, -1))
            },
            onIncrease = {
                onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, +1))
            },
            onTemperatureSteps = { steps ->
                onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, steps))
            },
            minTemperature = uiState.minTemperature,
            maxTemperature = uiState.maxTemperature,
            temperatureStepCelsius = uiState.temperatureStepCelsius,
            temperatureStepFahrenheit = uiState.temperatureStepFahrenheit,
            minTemperatureFahrenheit = uiState.minTemperatureFahrenheit,
            zoneLabel = ClimateZone.Driver.label,
            compact = compact,
            useZoneColorScheme = true,
            temperatureUnit = uiState.temperatureUnit,
            otherZoneTemperature = uiState.passengerTemperatureCelsius,
            modifier = dialModifier,
        )
    }
    val passengerDial = @Composable { dialModifier: Modifier ->
        ClimateTemperatureSection(
            temperature = uiState.passengerTemperatureCelsius,
            dialShape = dialShape,
            onDecrease = {
                onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Passenger, -1))
            },
            onIncrease = {
                onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Passenger, +1))
            },
            onTemperatureSteps = { steps ->
                onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Passenger, steps))
            },
            minTemperature = uiState.minTemperature,
            maxTemperature = uiState.maxTemperature,
            temperatureStepCelsius = uiState.temperatureStepCelsius,
            temperatureStepFahrenheit = uiState.temperatureStepFahrenheit,
            minTemperatureFahrenheit = uiState.minTemperatureFahrenheit,
            zoneLabel = ClimateZone.Passenger.label,
            compact = compact,
            useZoneColorScheme = true,
            temperatureUnit = uiState.temperatureUnit,
            otherZoneTemperature = uiState.temperatureCelsius,
            modifier = dialModifier,
        )
    }

    if (layout.useSideBySide && showDriver && showPassenger) {
        Row(
            modifier = modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(CarDesignTokens.TouchTargetSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            driverDial(
                widgetContentSharedElement(
                    widget = DashboardWidget.Climate,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ),
            )
            passengerDial(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CarDesignTokens.TouchTargetSpacing),
        ) {
            if (showDriver) {
                driverDial(
                    widgetContentSharedElement(
                        widget = DashboardWidget.Climate,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.fillMaxWidth(),
                    ),
                )
            }
            if (showPassenger) {
                passengerDial(Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ClimateAirBand(
    layout: WindowLayoutInfo,
    uiState: ClimateUiState,
    onEvent: (ClimateEvent) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    ClimateBlowerCard(
        fanSpeed = uiState.fanSpeed,
        maxFanSpeed = uiState.maxFanSpeed,
        isAcEnabled = uiState.isAcEnabled,
        onSpeedSelected = { onEvent(ClimateEvent.SetFanSpeed(it)) },
        showFan = uiState.capabilities.hasFanSpeed,
        showAirflow = uiState.capabilities.hasAirflowControls,
        airflowMode = uiState.airflowMode,
        airflowModes = uiState.capabilities.availableAirflowModes,
        onAirflowSelected = { onEvent(ClimateEvent.SelectAirflow(it)) },
        sideBySide = layout.useSideBySide,
        modifier = widgetControlsSharedElement(
            widget = DashboardWidget.Climate,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.fillMaxWidth(),
        ),
    )
}
