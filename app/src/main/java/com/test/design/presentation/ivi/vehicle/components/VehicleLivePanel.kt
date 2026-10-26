package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.vehicle.VehicleUiState
import com.test.design.presentation.ivi.vehicle.toTireWireframe
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.ExpressiveShapes

/**
 * Tire pressure card: top-down wireframe with a readout at each corner.
 */
@Composable
fun VehicleLivePanel(
    state: VehicleUiState,
    modifier: Modifier = Modifier,
) {
    val shape = ExpressiveShapes.large
    Surface(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
        shape = shape,
        color = Color(0xFF12161F),
        contentColor = Color.White,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        TireWireframeGraphic(
            wireframe = state.toTireWireframe(),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
        )
    }
}

@Composable
fun VehicleLiveSummary(
    state: VehicleUiState,
    modifier: Modifier = Modifier,
) {
    val rows = liveRows(state)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val bounded = maxHeight != Dp.Infinity
        Surface(
        modifier = if (bounded) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (bounded) Modifier.fillMaxSize() else Modifier)
                .padding(CarDesignTokens.TouchTargetSpacing),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OdometerGraphic(
                label = state.odometerLabel,
                fallbackMiles = state.odometerMiles,
                missingLabelIsUnavailable = true,
            )
            if (state.speedLabel != null || state.gearLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.speedLabel?.let { speed ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Speed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = speed,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                    state.gearLabel?.let { gear ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = gear,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            RangeGraphic(
                label = state.rangeLabel,
                miles = state.rangeMiles,
                modifier = if (bounded) Modifier.weight(1f) else Modifier,
            )
            rows.forEach { (label, value) ->
                LiveRow(label = label, value = value)
            }
        }
        }
    }
}

@Composable
private fun LiveRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private fun liveRows(state: VehicleUiState): List<Pair<String, String>> = buildList {
    state.outsideTempLabel?.let { add("Outside" to it) }
    state.parkingBrakeLabel?.let { add("Park brake" to it) }
    state.chargePortLabel?.let { add("Charge port" to it) }
}
