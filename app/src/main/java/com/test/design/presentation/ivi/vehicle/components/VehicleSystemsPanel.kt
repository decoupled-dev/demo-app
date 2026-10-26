package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.vehicle.RegenLevel
import com.test.design.presentation.ivi.vehicle.SystemHealth
import com.test.design.presentation.ivi.vehicle.VehicleSystemMetric
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.ExpressiveShapes
import com.test.design.theme.carTouchTarget

@Composable
fun VehicleSystemsPanel(
    systems: List<VehicleSystemMetric>,
    regenLevel: RegenLevel,
    selectedSystemId: String?,
    isCharging: Boolean,
    onRegenClick: () -> Unit,
    onSystemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val overallHealth = remember(systems) { systems.map { it.valuePercent }.average().toInt() }
    val alertCount = systems.count { it.health != SystemHealth.Good }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CarDesignTokens.TouchTargetSpacing),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Systems",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (alertCount == 0) "All clear" else "$alertCount need attention",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HealthScoreBadge(score = overallHealth)
            }

            PowerFlowStrip(
                regenLevel = regenLevel,
                isCharging = isCharging,
                onRegenClick = onRegenClick,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                systems.forEach { system ->
                    SystemMetricRow(
                        system = system,
                        selected = selectedSystemId == system.id,
                        onClick = { onSystemClick(system.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthScoreBadge(score: Int) {
    val color by animateColorAsState(
        targetValue = when {
            score >= 80 -> MaterialTheme.colorScheme.primary
            score >= 60 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "health_score_color",
    )
    Text(
        text = "$score",
        style = MaterialTheme.typography.headlineMedium,
        color = color,
    )
}

@Composable
private fun PowerFlowStrip(
    regenLevel: RegenLevel,
    isCharging: Boolean,
    onRegenClick: () -> Unit,
) {
    val flowStrength by animateFloatAsState(
        targetValue = when {
            isCharging -> 1f
            regenLevel == RegenLevel.Off -> 0.15f
            else -> regenLevel.strength
        },
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "power_flow_strength",
    )
    val flowColor by animateColorAsState(
        targetValue = if (isCharging) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "power_flow_color",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .carTouchTarget()
            .clickable(onClick = onRegenClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isCharging) "Charge flow" else "Regen",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isCharging) "AC" else regenLevel.label,
                style = MaterialTheme.typography.titleSmall,
                color = flowColor,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowNode(icon = Icons.Default.BatteryChargingFull, active = isCharging, tint = flowColor)
            FlowConnector(strength = flowStrength, color = flowColor, modifier = Modifier.weight(1f))
            FlowNode(icon = Icons.Default.ElectricBolt, active = true, tint = flowColor)
            FlowConnector(strength = flowStrength * 0.85f, color = flowColor, modifier = Modifier.weight(1f))
            FlowNode(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                active = !isCharging && regenLevel != RegenLevel.Off,
                tint = flowColor,
            )
        }
    }
}

@Composable
private fun FlowNode(
    icon: ImageVector,
    active: Boolean,
    tint: Color,
) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.35f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "flow_node_alpha",
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f * alpha))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = alpha),
        )
    }
}

@Composable
private fun FlowConnector(
    strength: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shimmer = rememberInfiniteTransition(label = "flow_shimmer")
    val shift by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flow_shift",
    )
    val peak = 0.18f + shift * 0.64f
    Box(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(strength.coerceIn(0.08f, 1f))
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to color.copy(alpha = 0.15f),
                            (peak - 0.1f).coerceIn(0.02f, 0.9f) to color,
                            peak.coerceIn(0.08f, 0.94f) to Color.White.copy(alpha = 0.85f),
                            (peak + 0.1f).coerceIn(0.12f, 0.98f) to color,
                            1f to color.copy(alpha = 0.9f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun SystemMetricRow(
    system: VehicleSystemMetric,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = system.valuePercent / 100f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "system_progress_${system.id}",
    )
    val healthColor by animateColorAsState(
        targetValue = system.health.color(),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "system_health_${system.id}",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            Color.Transparent
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "system_row_bg_${system.id}",
    )
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .carTouchTarget()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = system.icon(),
                contentDescription = null,
                tint = healthColor,
            )
            Text(
                system.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${system.valuePercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = healthColor,
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = healthColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(effectsSpec) + expandVertically(),
            exit = fadeOut(effectsSpec) + shrinkVertically(),
        ) {
            Text(
                system.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SystemHealth.color(): Color = when (this) {
    SystemHealth.Good -> MaterialTheme.colorScheme.primary
    SystemHealth.Caution -> MaterialTheme.colorScheme.tertiary
    SystemHealth.Warning -> MaterialTheme.colorScheme.error
}

private fun VehicleSystemMetric.icon(): ImageVector = when (id) {
    "motor" -> Icons.Default.ElectricBolt
    "brakes" -> Icons.Default.DiscFull
    "battery" -> Icons.Default.BatteryChargingFull
    "cabin" -> Icons.Default.AcUnit
    else -> Icons.Default.ElectricBolt
}
