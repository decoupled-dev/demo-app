package com.test.design.presentation.ivi.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.presentation.ivi.navigation.NavigationUiState
import com.test.design.presentation.ivi.vehicle.VehicleUiState
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.ExpressiveShapes
import com.test.design.theme.glassPanelColor
import com.test.design.theme.navigationGlassPanelColor

private val RailCardShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 14.dp,
    bottomEnd = 28.dp,
    bottomStart = 14.dp,
)

/**
 * Floating left driving panel — wraps content height so the map stays dominant.
 * AAOS system bars own status / launcher; this rail is speed + turn guidance only.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DrivingStatusRail(
    vehicleState: VehicleUiState,
    navigationState: NavigationUiState,
    onVehicleClick: () -> Unit,
    onNavigationClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    speedMph: Int = 20,
    gear: String = "D",
) {
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Surface(
        modifier = modifier
            .width(360.dp)
            .widthIn(max = 400.dp),
        shape = ExpressiveShapes.extraLarge,
        color = navigationGlassPanelColor(),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SpeedGearHeader(
                speedMph = speedMph,
                gear = gear,
                batteryPercent = vehicleState.batteryPercent,
                rangeLabel = vehicleState.rangeLabel,
                driveMode = vehicleState.driveMode.label,
                isCharging = vehicleState.isCharging,
                onVehicleClick = onVehicleClick,
                animatedVisibilityScope = animatedVisibilityScope,
            )

            Surface(
                onClick = onNavigationClick,
                modifier = widgetContainerTransform(
                    widget = DashboardWidget.Navigation,
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
                shape = ExpressiveShapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = navigationState.maneuverIcon,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = navigationState.currentInstruction,
                            transitionSpec = {
                                fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec)
                            },
                            label = "rail_turn",
                        ) { instruction ->
                            Text(
                                text = instruction,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "${navigationState.distanceRemaining} · ${navigationState.etaMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(CarDesignTokens.SecondaryIcon),
                    )
                }
            }

            navigationState.routeSteps.firstOrNull()?.let { step ->
                Surface(
                    onClick = onNavigationClick,
                    shape = ExpressiveShapes.medium,
                    color = glassPanelColor(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = step.instruction,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = step.distanceLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.SpeedGearHeader(
    speedMph: Int,
    gear: String,
    batteryPercent: Int,
    rangeLabel: String,
    driveMode: String,
    isCharging: Boolean,
    onVehicleClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "$speedMph",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "MPH",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GearSelector(active = gear)
        }

        Surface(
            onClick = onVehicleClick,
            modifier = widgetContainerTransform(
                widget = DashboardWidget.Vehicle,
                animatedVisibilityScope = animatedVisibilityScope,
            ),
            shape = RailCardShape,
            color = glassPanelColor(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (isCharging) {
                        Icons.Default.BatteryChargingFull
                    } else {
                        Icons.Default.Bolt
                    },
                    contentDescription = null,
                    modifier = Modifier.size(CarDesignTokens.TertiaryIcon),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "$batteryPercent% · $rangeLabel",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = driveMode,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Charging",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(CarDesignTokens.TertiaryIcon),
                    )
                }
            }
        }
    }
}

@Composable
private fun GearSelector(active: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("P", "R", "N", "D").forEach { gear ->
            val selected = gear == active
            Text(
                text = gear,
                style = if (selected) {
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
        }
    }
}
