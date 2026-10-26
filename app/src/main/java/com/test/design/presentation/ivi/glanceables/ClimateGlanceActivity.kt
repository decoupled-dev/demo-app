package com.test.design.presentation.ivi.glanceables

import android.content.Intent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.presentation.ivi.climate.ClimateEvent
import com.test.design.presentation.ivi.climate.ClimateUiState
import com.test.design.presentation.ivi.climate.ClimateViewModel
import com.test.design.presentation.ivi.climate.components.ClimateHvacIcons
import com.test.design.presentation.ivi.climate.formatTemperature
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.climateAmbientColor
import com.test.design.theme.climateContentAccent
import com.test.design.theme.temperatureToFraction

/** Scalable UI `climate_glance` TaskPanel — HVAC capsule. */
class ClimateGlanceActivity : GlanceableActivity() {

    private val climateViewModel: ClimateViewModel by viewModels()

    @Composable
    override fun GlanceContent() {
        val climateState by climateViewModel.state.collectAsStateWithLifecycle()
        GlanceRoot {
            ClimateGlanceBar(
                climateState = climateState,
                climateTemperature = climateState.temperatureCelsius,
                onClimateEvent = climateViewModel::onEvent,
                onExpandClimate = {
                    startActivity(
                        Intent(this, ClimatePanelActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun ClimateGlanceBar(
    climateState: ClimateUiState,
    climateTemperature: Float,
    onClimateEvent: (ClimateEvent) -> Unit,
    onExpandClimate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tempFraction = temperatureToFraction(
        celsius = climateTemperature,
        min = climateState.minTemperature,
        max = climateState.maxTemperature,
    )
    val colors = glancePalette()
    val ambient by animateColorAsState(
        targetValue = climateAmbientColor(tempFraction),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "hvac_ambient",
    )
    val accent = climateContentAccent(tempFraction, colors.light)
    val seatHeatTint = if (colors.light) Color(0xFFE64A19) else Color(0xFFFF8A65)
    val shape = RoundedCornerShape(24.dp)

    Surface(
        onClick = onExpandClimate,
        modifier = modifier,
        shape = shape,
        color = colors.card,
        border = colors.cardBorder(),
        shadowElevation = if (colors.light) 2.dp else 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ambient.copy(alpha = if (colors.light) 0.12f else 0.18f),
                            Color.Transparent,
                            ambient.copy(alpha = if (colors.light) 0.07f else 0.10f),
                        ),
                    ),
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val caps = climateState.capabilities
            if (caps.hasSeatHeat) {
                HvacLevelControl(
                    icon = ClimateHvacIcons.SeatHeat,
                    contentDescription = "Seat heat",
                    level = climateState.seatHeatLevel,
                    maxLevel = climateState.maxSeatHeatLevel,
                    activeColor = seatHeatTint,
                    idleColor = colors.inactive,
                    onClick = { onClimateEvent(ClimateEvent.CycleSeatHeat) },
                )
            }
            if (caps.hasFrontDefrost) {
                HvacToggleControl(
                    icon = ClimateHvacIcons.FrontDefrost,
                    contentDescription = "Front defrost",
                    active = climateState.isFrontDefrostOn,
                    activeColor = if (colors.light) colors.title else Color.White,
                    idleColor = colors.inactive,
                    onClick = { onClimateEvent(ClimateEvent.ToggleFrontDefrost) },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onExpandClimate)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = climateState.formatTemperature(climateTemperature),
                    color = accent,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = buildString {
                        if (caps.hasAc && climateState.isAcEnabled) append("A/C")
                        if (caps.hasFanSpeed) {
                            if (isNotEmpty()) append(" · ")
                            append("Fan ${climateState.fanSpeed}")
                        }
                        if (isEmpty()) append(if (climateState.isLive) "Live" else "Climate")
                    },
                    color = colors.muted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (caps.hasAc) {
                HvacToggleControl(
                    icon = ClimateHvacIcons.Ac,
                    contentDescription = "A/C",
                    active = climateState.isAcEnabled,
                    activeColor = if (colors.light) Color(0xFF1565C0) else Color(0xFF4EA1FF),
                    idleColor = colors.inactive,
                    onClick = { onClimateEvent(ClimateEvent.ToggleAc) },
                )
            }
            if (caps.hasFanSpeed) {
                CompactFanBars(
                    fanSpeed = climateState.fanSpeed,
                    maxFanSpeed = climateState.maxFanSpeed,
                    activeColor = accent,
                    idleColor = colors.inactive,
                    onSpeedSelected = { onClimateEvent(ClimateEvent.SetFanSpeed(it)) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            IconButton(
                onClick = onExpandClimate,
                modifier = Modifier.size(CarDesignTokens.MinTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = "Open climate",
                    tint = colors.muted,
                    modifier = Modifier.size(CarDesignTokens.SecondaryIcon),
                )
            }
        }
    }
}

@Composable
private fun HvacToggleControl(
    @DrawableRes icon: Int,
    contentDescription: String,
    active: Boolean,
    activeColor: Color,
    idleColor: Color,
    onClick: () -> Unit,
) {
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val tint by animateColorAsState(
        targetValue = if (active) activeColor else idleColor,
        animationSpec = effectsSpec,
        label = "hvac_toggle_tint_$contentDescription",
    )
    val container by animateColorAsState(
        targetValue = if (active) activeColor.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = effectsSpec,
        label = "hvac_toggle_bg_$contentDescription",
    )

    Box(
        modifier = Modifier
            .size(CarDesignTokens.MinTouchTarget)
            .clip(CircleShape)
            .background(container)
            .semantics {
                role = Role.Switch
                this.contentDescription =
                    "$contentDescription ${if (active) "on" else "off"}"
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(CarDesignTokens.SecondaryIcon),
        )
    }
}

@Composable
private fun HvacLevelControl(
    @DrawableRes icon: Int,
    contentDescription: String,
    level: Int,
    maxLevel: Int,
    activeColor: Color,
    idleColor: Color,
    onClick: () -> Unit,
) {
    val active = level > 0
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val tint by animateColorAsState(
        targetValue = if (active) activeColor else idleColor,
        animationSpec = effectsSpec,
        label = "hvac_level_tint_$contentDescription",
    )
    val container by animateColorAsState(
        targetValue = if (active) activeColor.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = effectsSpec,
        label = "hvac_level_bg_$contentDescription",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .sizeIn(minWidth = CarDesignTokens.MinTouchTarget, minHeight = CarDesignTokens.MinTouchTarget)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .semantics {
                role = Role.Button
                this.contentDescription =
                    "$contentDescription, ${if (active) "level $level" else "off"}"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(CarDesignTokens.SecondaryIcon),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(maxLevel) { index ->
                val lit = index < level
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(if (lit) tint else idleColor.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

@Composable
private fun CompactFanBars(
    fanSpeed: Int,
    maxFanSpeed: Int,
    activeColor: Color,
    idleColor: Color,
    onSpeedSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(CarDesignTokens.MinTouchTarget)
            .semantics {
                contentDescription = "Fan speed $fanSpeed of $maxFanSpeed"
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(maxFanSpeed) { index ->
            val level = index + 1
            val active = level <= fanSpeed
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(
                        lerp(
                            16.dp,
                            CarDesignTokens.MinTouchTarget - 4.dp,
                            level / maxFanSpeed.coerceAtLeast(1).toFloat(),
                        ),
                    )
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (active) activeColor else idleColor)
                    .clickable { onSpeedSelected(level) },
            )
        }
    }
}
