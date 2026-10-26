package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.vehicle.DriveMode
import com.test.design.presentation.ivi.vehicle.RegenLevel
import com.test.design.theme.ExpressiveShapes

@Composable
fun VehicleDriveInsightsCard(
    driveMode: DriveMode,
    regenLevel: RegenLevel,
    efficiencyMpkWh: Float,
    rangeMiles: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = driveMode.insightsTitle(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                driveMode.insightRows(
                    regenLevel = regenLevel,
                    efficiencyMpkWh = efficiencyMpkWh,
                    rangeMiles = rangeMiles,
                ).forEach { (label, value) ->
                    InsightTile(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun DriveMode.insightsTitle(): String = when (this) {
    DriveMode.Eco -> "Range coach"
    DriveMode.Comfort -> "Assists"
    DriveMode.Sport -> "Performance"
}

private fun DriveMode.insightRows(
    regenLevel: RegenLevel,
    efficiencyMpkWh: Float,
    rangeMiles: Int,
): List<Pair<String, String>> = when (this) {
    DriveMode.Eco -> listOf(
        "Efficiency" to "$efficiencyMpkWh mi/kWh",
        "Range" to "$rangeMiles mi",
        "Regen" to regenLevel.label,
    )
    DriveMode.Comfort -> listOf(
        "Cruise" to "On",
        "Lane keep" to "Ready",
        "Blind spot" to "Clear",
    )
    DriveMode.Sport -> listOf(
        "Power" to "${peakPowerKw()} kW",
        "Torque" to "${peakTorqueNm()} Nm",
        "0–60" to "${zeroToSixtySeconds()} s",
    )
}

private fun DriveMode.peakPowerKw(): Int = when (this) {
    DriveMode.Eco -> 120
    DriveMode.Comfort -> 180
    DriveMode.Sport -> 285
}

private fun DriveMode.peakTorqueNm(): Int = when (this) {
    DriveMode.Eco -> 240
    DriveMode.Comfort -> 320
    DriveMode.Sport -> 420
}

private fun DriveMode.zeroToSixtySeconds(): String = when (this) {
    DriveMode.Eco -> "6.8"
    DriveMode.Comfort -> "5.4"
    DriveMode.Sport -> "4.1"
}
