package com.test.design.presentation.ivi.climate.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.climate.rememberClimateHaptics
import com.test.design.presentation.ivi.climate.seatHeatColors
import com.test.design.presentation.ivi.climate.seatHeatStateLabel
import com.test.design.theme.CarDesignTokens
import com.test.design.presentation.ivi.common.MorphingDetailSurfaceCard
import com.test.design.theme.ClimateCardActiveRadii
import com.test.design.theme.ClimateCardRestRadii

private val ComfortButtonSize = 52.dp
private val ComfortIconSize = 28.dp
private val SeatHeatButtonSize = CarDesignTokens.MinTouchTarget
private val SeatHeatIconSize = 48.dp

/**
 * Comfort strip — labeled climate controls with active and inactive states.
 * Seat heaters sit outside the card, driver on the left and passenger on the right.
 */
@Composable
fun ClimateComfortControlsCard(
    seatHeatLevel: Int,
    maxSeatHeatLevel: Int,
    passengerSeatHeatLevel: Int,
    steeringHeatLevel: Int,
    maxSteeringHeatLevel: Int,
    seatVentLevel: Int,
    maxSeatVentLevel: Int,
    isFrontDefrostOn: Boolean,
    isRearDefrostOn: Boolean,
    isRecirculationOn: Boolean,
    isSyncEnabled: Boolean,
    isAcEnabled: Boolean,
    isPowerOn: Boolean,
    isAutoOn: Boolean,
    onCycleSeatHeat: () -> Unit,
    onCyclePassengerSeatHeat: () -> Unit,
    onCycleSteeringHeat: () -> Unit,
    onCycleSeatVent: () -> Unit,
    onToggleFrontDefrost: () -> Unit,
    onToggleRearDefrost: () -> Unit,
    onToggleRecirculation: () -> Unit,
    onToggleSync: () -> Unit,
    onTogglePower: () -> Unit,
    onToggleAuto: () -> Unit,
    modifier: Modifier = Modifier,
    showSeatHeat: Boolean = true,
    showPassengerSeatHeat: Boolean = true,
    showSteeringHeat: Boolean = true,
    showSeatVent: Boolean = true,
    showFrontDefrost: Boolean = true,
    showRearDefrost: Boolean = true,
    showRecirculation: Boolean = true,
    showSync: Boolean = true,
    showPower: Boolean = true,
    showAuto: Boolean = true,
) {
    val showCabin = showSteeringHeat || showSeatVent || showFrontDefrost || showRearDefrost ||
        showRecirculation || showSync || showPower || showAuto
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSeatHeat) {
            SeatHeatButton(
                icon = ClimateHvacIcons.SeatHeat,
                label = "Driver",
                contentDescription = "Driver seat heat",
                level = seatHeatLevel,
                maxLevel = maxSeatHeatLevel,
                onClick = onCycleSeatHeat,
            )
        }
        if (showCabin) {
            MorphingDetailSurfaceCard(
                morphExpanded = isAcEnabled,
                compactRadii = ClimateCardRestRadii,
                expandedRadii = ClimateCardActiveRadii,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
            if (showSteeringHeat) {
                ComfortIconLevelButton(
                    icon = ClimateHvacIcons.SteeringHeat,
                    label = "Wheel",
                    contentDescription = "Wheel heat",
                    level = steeringHeatLevel,
                    maxLevel = maxSteeringHeatLevel,
                    onClick = onCycleSteeringHeat,
                )
            }
            if (showSeatVent) {
                ComfortIconLevelButton(
                    icon = ClimateHvacIcons.SeatVent,
                    label = "Vent",
                    contentDescription = "Seat vent",
                    level = seatVentLevel,
                    maxLevel = maxSeatVentLevel,
                    onClick = onCycleSeatVent,
                )
            }
            if (showFrontDefrost) {
                ComfortIconToggle(
                    icon = ClimateHvacIcons.FrontDefrost,
                    label = "Front",
                    contentDescription = "Front defrost",
                    active = isFrontDefrostOn,
                    onClick = onToggleFrontDefrost,
                )
            }
            if (showRearDefrost) {
                ComfortIconToggle(
                    icon = ClimateHvacIcons.RearDefrost,
                    label = "Rear",
                    contentDescription = "Rear defrost",
                    active = isRearDefrostOn,
                    onClick = onToggleRearDefrost,
                )
            }
            if (showRecirculation) {
                ComfortIconToggle(
                    icon = ClimateHvacIcons.FreshAir,
                    label = "Fresh",
                    contentDescription = "Fresh air",
                    active = !isRecirculationOn,
                    onClick = {
                        if (isRecirculationOn) onToggleRecirculation()
                    },
                )
                ComfortIconToggle(
                    icon = ClimateHvacIcons.Recirculation,
                    label = "Recirc",
                    contentDescription = "Recirculation",
                    active = isRecirculationOn,
                    onClick = onToggleRecirculation,
                )
            }
            if (showSync) {
                ComfortIconToggle(
                    icon = ClimateHvacIcons.SyncZones,
                    label = "Sync",
                    contentDescription = "Sync zones",
                    active = isSyncEnabled,
                    onClick = onToggleSync,
                )
            }
            if (showPower) {
                ComfortIconToggle(
                    icon = ClimateHvacIcons.Power,
                    label = "Power",
                    contentDescription = "Climate power",
                    active = isPowerOn,
                    onClick = onTogglePower,
                )
            }
            if (showAuto) {
                ComfortIconToggle(
                    icon = ClimateHvacIcons.Auto,
                    label = "Auto",
                    contentDescription = "Auto climate",
                    active = isAutoOn,
                    onClick = onToggleAuto,
                )
            }
                }
            }
        }
        if (showPassengerSeatHeat) {
            SeatHeatButton(
                icon = ClimateHvacIcons.SeatHeatPassenger,
                label = "Passenger",
                contentDescription = "Passenger seat heat",
                level = passengerSeatHeatLevel,
                maxLevel = maxSeatHeatLevel,
                onClick = onCyclePassengerSeatHeat,
            )
        }
    }
}

@Composable
private fun SeatHeatButton(
    @DrawableRes icon: Int,
    label: String,
    contentDescription: String,
    level: Int,
    maxLevel: Int,
    onClick: () -> Unit,
) {
    val haptics = rememberClimateHaptics()
    val active = level > 0
    val steps = maxLevel.coerceAtLeast(1)
    val lightCanvas = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val stateLabel = seatHeatStateLabel(level, steps)
    val heat = seatHeatColors(level, steps, lightCanvas)
    val motionSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val container by animateColorAsState(
        targetValue = if (active) heat.fill else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = motionSpec,
        label = "seat_heat_bg_$contentDescription",
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
        },
        animationSpec = motionSpec,
        label = "seat_heat_icon_$contentDescription",
    )
    val border by animateColorAsState(
        targetValue = if (active) {
            Color.White.copy(alpha = if (lightCanvas) 0.72f else 0.4f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        },
        animationSpec = motionSpec,
        label = "seat_heat_border_$contentDescription",
    )
    val labelColor by animateColorAsState(
        targetValue = if (active) heat.accent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motionSpec,
        label = "seat_heat_label_$contentDescription",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.86f,
        animationSpec = spatialSpec,
        label = "seat_heat_scale_$contentDescription",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(SeatHeatButtonSize)
                .clip(CircleShape)
                .background(container)
                .border(if (active) 3.dp else 1.5.dp, border, CircleShape)
                .semantics {
                    role = Role.Button
                    this.contentDescription = "$contentDescription, $stateLabel"
                }
                .clickable {
                    haptics.tick()
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(SeatHeatIconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
        }
        SeatHeatLevelMeter(
            level = level.coerceIn(0, steps),
            maxLevel = steps,
            activeColor = if (active) heat.accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
        )
        Text(
            text = stateLabel,
            style = MaterialTheme.typography.titleMedium,
            color = labelColor,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun SeatHeatLevelMeter(
    level: Int,
    maxLevel: Int,
    activeColor: Color,
    trackColor: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(maxLevel) { index ->
            val lit = index < level
            val color by animateColorAsState(
                targetValue = if (lit) activeColor else trackColor,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "seat_heat_bar_$index",
            )
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = (8 + index * 5).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun ComfortIconLevelButton(
    @DrawableRes icon: Int,
    label: String,
    contentDescription: String,
    level: Int,
    maxLevel: Int,
    onClick: () -> Unit,
) {
    val haptics = rememberClimateHaptics()
    val active = level > 0
    val motionSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = motionSpec,
        label = "comfort_level_bg_$contentDescription",
    )
    val content by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motionSpec,
        label = "comfort_level_fg_$contentDescription",
    )
    val border by animateColorAsState(
        targetValue = if (active) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        animationSpec = motionSpec,
        label = "comfort_level_border_$contentDescription",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComfortIconLabel(label)
        Box(
            modifier = Modifier
                .size(ComfortButtonSize)
                .clip(CircleShape)
                .background(container)
                .border(1.5.dp, border, CircleShape)
                .semantics {
                    role = Role.Button
                    this.contentDescription =
                        "$contentDescription, ${if (active) "level $level" else "off"}"
                }
                .clickable {
                    haptics.tick()
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(ComfortIconSize),
            )
        }
        ComfortLevelBars(
            level = level,
            maxLevel = maxLevel,
            activeColor = content,
            inactiveColor = content.copy(alpha = 0.25f),
        )
    }
}

@Composable
private fun ComfortIconToggle(
    @DrawableRes icon: Int,
    label: String,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberClimateHaptics()
    val motionSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = motionSpec,
        label = "comfort_toggle_bg_$contentDescription",
    )
    val content by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motionSpec,
        label = "comfort_toggle_fg_$contentDescription",
    )
    val border by animateColorAsState(
        targetValue = if (active) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        animationSpec = motionSpec,
        label = "comfort_toggle_border_$contentDescription",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComfortIconLabel(label)
        Box(
            modifier = Modifier
                .size(ComfortButtonSize)
                .clip(CircleShape)
                .background(container)
                .border(1.5.dp, border, CircleShape)
                .semantics {
                    role = Role.Switch
                    this.contentDescription =
                        "$contentDescription ${if (active) "on" else "off"}"
                }
                .clickable {
                    haptics.toggle(!active)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(ComfortIconSize),
            )
        }
    }
}

@Composable
private fun ComfortIconLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 16.sp,
            lineHeight = 18.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ComfortLevelBars(
    level: Int,
    maxLevel: Int,
    activeColor: Color,
    inactiveColor: Color,
) {
    val motionSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxLevel) { index ->
            val lit = index < level
            val alpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = motionSpec,
                label = "comfort_bar_$index",
            )
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background((if (lit) activeColor else inactiveColor).copy(alpha = alpha)),
            )
        }
    }
}
