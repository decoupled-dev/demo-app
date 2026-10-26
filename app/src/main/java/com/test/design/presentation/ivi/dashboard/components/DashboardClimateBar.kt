package com.test.design.presentation.ivi.dashboard.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.climate.ClimateEvent
import com.test.design.presentation.ivi.climate.ClimateUiState
import com.test.design.presentation.ivi.climate.components.ClimateHvacIcons
import com.test.design.presentation.ivi.climate.formatTemperature
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.glassSurfaceColor

/**
 * Compact climate capsule — AAOS SystemUI already owns temp +/- and app launcher,
 * so this only exposes HVAC shortcuts that open the full climate sheet.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DashboardClimateBar(
    climateState: ClimateUiState,
    climateTemperature: Float,
    onClimateEvent: (ClimateEvent) -> Unit,
    onExpandClimate: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onOpenWidgetDashboard: (() -> Unit)? = null,
) {
    val barShape = RoundedCornerShape(
        topStart = 40.dp,
        topEnd = 16.dp,
        bottomEnd = 40.dp,
        bottomStart = 16.dp,
    )

    Surface(
        modifier = widgetContainerTransform(
            widget = DashboardWidget.Climate,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = modifier.widthIn(min = 280.dp),
        ),
        shape = barShape,
        color = glassSurfaceColor(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val caps = climateState.capabilities
            if (caps.hasFrontDefrost) {
                FilledIconToggleButton(
                    checked = climateState.isFrontDefrostOn,
                    onCheckedChange = { onClimateEvent(ClimateEvent.ToggleFrontDefrost) },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        painter = painterResource(ClimateHvacIcons.FrontDefrost),
                        contentDescription = "Front defrost",
                        modifier = Modifier.size(CarDesignTokens.TertiaryIcon),
                    )
                }
            }
            if (caps.hasSeatHeat) {
                IconButton(
                    onClick = { onClimateEvent(ClimateEvent.CycleSeatHeat) },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        painter = painterResource(ClimateHvacIcons.SeatHeat),
                        contentDescription = "Seat heat",
                        tint = if (climateState.seatHeatLevel > 0) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(CarDesignTokens.TertiaryIcon),
                    )
                }
            }
            TextButton(onClick = onExpandClimate) {
                Text(
                    text = climateState.formatTemperature(climateTemperature),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (caps.hasAc) {
                FilledIconToggleButton(
                    checked = climateState.isAcEnabled,
                    onCheckedChange = { onClimateEvent(ClimateEvent.ToggleAc) },
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        painter = painterResource(ClimateHvacIcons.Ac),
                        contentDescription = "A/C",
                        modifier = Modifier.size(CarDesignTokens.TertiaryIcon),
                    )
                }
            }
            if (onOpenWidgetDashboard != null) {
                IconButton(
                    onClick = onOpenWidgetDashboard,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Widget dashboard",
                        modifier = Modifier.size(CarDesignTokens.TertiaryIcon),
                    )
                }
            }
        }
    }
}
