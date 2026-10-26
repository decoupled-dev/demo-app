package com.test.design.presentation.ivi.vehicle

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.test.design.presentation.common.ScreenBackground
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.presentation.ivi.dashboard.widgetContentSharedElement
import com.test.design.presentation.ivi.vehicle.components.VehicleEnergyCockpit
import com.test.design.presentation.ivi.vehicle.components.VehicleInfoCard
import com.test.design.presentation.ivi.vehicle.components.VehicleLivePanel
import com.test.design.theme.CarBackgroundTokens
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.ExpressiveShapes
import com.test.design.theme.batteryToFraction
import com.test.design.theme.vehicleColorScheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun SharedTransitionScope.VehicleScreen(
    uiState: VehicleUiState,
    onEvent: (VehicleEvent) -> Unit,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    showBack: Boolean = true,
    contentPadding: Dp = CarDesignTokens.ContentPadding,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val batteryFraction = batteryToFraction(uiState.batteryPercent)
    val dynamicScheme = vehicleColorScheme(
        driveMode = uiState.driveMode,
        batteryFraction = batteryFraction,
        base = MaterialTheme.colorScheme,
        isCharging = uiState.isCharging,
    )
    MaterialTheme(
        colorScheme = dynamicScheme,
        typography = MaterialTheme.typography,
        shapes = ExpressiveShapes,
        motionScheme = MaterialTheme.motionScheme,
    ) {
        CompositionLocalProvider(LocalContentColor provides dynamicScheme.onBackground) {
            val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
            val animatedPrimary by animateColorAsState(
                targetValue = dynamicScheme.primary,
                animationSpec = effectsSpec,
                label = "vehicle_tint",
            )

            Box(
                modifier = widgetContainerTransform(
                    widget = DashboardWidget.Vehicle,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = modifier.fillMaxSize(),
                ),
            ) {
                ScreenBackground()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    animatedPrimary.copy(alpha = CarBackgroundTokens.DetailTintAlpha),
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                        alpha = CarBackgroundTokens.DetailOverlayAlpha,
                                    ),
                                ),
                            ),
                        )
                        .padding(contentPadding),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedPrimary.copy(alpha = 0.18f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.22f, size.height * 0.5f),
                                radius = size.minDimension * 0.45f,
                            ),
                            radius = size.minDimension * 0.45f,
                            center = Offset(size.width * 0.22f, size.height * 0.5f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (compact) 8.dp else CarDesignTokens.TouchTargetSpacing,
                        ),
                    ) {
                        val readout = uiState.toInfoReadout()
                        VehicleEnergyCockpit(
                            percent = uiState.batteryPercent,
                            rangeMiles = uiState.rangeMiles,
                            rangeLabel = uiState.rangeLabel,
                            maxRangeMiles = RangeCapacityMiles,
                            isCharging = uiState.isCharging,
                            chargeRateKw = uiState.chargeRateKw,
                            energyTitle = uiState.energyLabel,
                            gaugeShape = ExpressiveShapes.large,
                            summaryFraction = readout.chargeFraction,
                            summaryRemaining = "${readout.remainingValue} ${readout.remainingUnit}",
                            summaryAverage = "${readout.averageValue} ${readout.averageUnit}",
                            summaryCapacity = "${readout.capacityValue} ${readout.capacityUnit}",
                            onGaugeClick = {
                                if (!uiState.isLive) onEvent(VehicleEvent.CycleBatteryDemo)
                            },
                            contentModifier = widgetContentSharedElement(
                                widget = DashboardWidget.Vehicle,
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        VehicleLivePanel(
                            state = uiState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        VehicleInfoCard(
                            state = uiState,
                            onDriveModeSelected = { onEvent(VehicleEvent.SelectDriveMode(it)) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}
