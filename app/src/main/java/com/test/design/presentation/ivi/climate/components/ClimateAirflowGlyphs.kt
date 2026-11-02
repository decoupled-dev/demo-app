package com.test.design.presentation.ivi.climate.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.test.design.presentation.ivi.climate.AirflowMode
import com.test.design.presentation.ivi.climate.rememberClimateHaptics
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget

@Composable
fun ClimateAirflowGlyphs(
    selectedMode: AirflowMode,
    modes: List<AirflowMode>,
    onModeSelected: (AirflowMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (modes.isEmpty()) return
    val haptics = rememberClimateHaptics()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        modes.forEach { mode ->
            QuietSymbol(
                icon = mode.glyph,
                contentDescription = mode.label,
                selected = mode == selectedMode,
                onClick = {
                    if (mode != selectedMode) haptics.confirm()
                    onModeSelected(mode)
                },
            )
        }
    }
}

@Composable
fun QuietSymbol(
    @DrawableRes icon: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val motion = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val fill by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = motion,
        label = "symbol_fill_$contentDescription",
    )
    val iconTint by animateColorAsState(
        targetValue = tint ?: if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
        },
        animationSpec = motion,
        label = "symbol_tint_$contentDescription",
    )
    Box(
        modifier = modifier
            .size(CarDesignTokens.MinTouchTarget)
            .clip(CircleShape)
            .background(fill)
            .carTouchTarget()
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(CarDesignTokens.SecondaryIcon),
        )
    }
}

fun seatHeatTint(level: Int, maxLevel: Int, rest: Color): Color {
    if (maxLevel <= 0 || level <= 0) return rest.copy(alpha = 0.42f)
    val amount = (level / maxLevel.toFloat()).coerceIn(0f, 1f)
    return lerp(rest.copy(alpha = 0.55f), Color(0xFFE8A07A), amount)
}

@get:DrawableRes
private val AirflowMode.glyph: Int
    get() = when (this) {
        AirflowMode.Face -> ClimateHvacIcons.AirflowFace
        AirflowMode.BiLevel -> ClimateHvacIcons.AirflowBiLevel
        AirflowMode.Feet -> ClimateHvacIcons.AirflowFeet
        AirflowMode.FeetDefrost -> ClimateHvacIcons.AirflowFeetDefrost
        AirflowMode.Auto -> ClimateHvacIcons.Auto
    }
