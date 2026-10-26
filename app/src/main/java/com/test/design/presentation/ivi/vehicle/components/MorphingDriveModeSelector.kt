package com.test.design.presentation.ivi.vehicle.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.vehicle.DriveMode
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget

@Composable
fun MorphingDriveModeSelector(
    selected: DriveMode,
    onSelected: (DriveMode) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    val modes = DriveMode.entries
    val selectedIndex = modes.indexOf(selected)
    val motionSpec = MaterialTheme.motionScheme.slowSpatialSpec<Dp>()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!embedded) {
            Text(
                "Drive mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (embedded) 76.dp else 88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (embedded) {
                        Color.Black.copy(alpha = 0.28f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                    },
                ),
        ) {
            val segmentWidth = maxWidth / modes.size
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = motionSpec,
                label = "drive_indicator_offset",
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(26.dp),
                    ),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                modes.forEach { mode ->
                    val isSelected = mode == selected
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 0.92f,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        label = "drive_mode_icon_${mode.name}",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .carTouchTarget()
                            .clickable { onSelected(mode) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = mode.icon(),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        if (!embedded) {
            val subtitleSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(subtitleSpec) togetherWith fadeOut(subtitleSpec) },
                label = "drive_mode_subtitle",
            ) { mode ->
                Text(
                    text = mode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun DriveMode.icon(): ImageVector = when (this) {
    DriveMode.Eco -> Icons.Default.Eco
    DriveMode.Comfort -> Icons.Outlined.DirectionsCar
    DriveMode.Sport -> Icons.Default.Speed
}
