package com.test.design.presentation.ivi.dashboard.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.climate.ClimateEvent
import com.test.design.presentation.ivi.climate.ClimateUiState
import com.test.design.presentation.ivi.dashboard.liveStatus
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.presentation.ivi.dashboard.widgetIconSharedElement
import com.test.design.presentation.ivi.dashboard.widgetTitleSharedElement
import com.test.design.presentation.ivi.media.MediaEvent
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.navigation.NavigationEvent
import com.test.design.presentation.ivi.navigation.NavigationUiState
import com.test.design.presentation.ivi.vehicle.VehicleEvent
import com.test.design.presentation.ivi.vehicle.VehicleUiState
import com.test.design.theme.CarBackgroundTokens
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.WidgetCardShape

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DashboardWidgetCard(
    widget: DashboardWidget,
    onClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    mediaState: MediaUiState? = null,
    onMediaEvent: ((MediaEvent) -> Unit)? = null,
    climateState: ClimateUiState? = null,
    climateTemperature: Float? = null,
    onClimateEvent: ((ClimateEvent) -> Unit)? = null,
    navigationState: NavigationUiState? = null,
    onNavigationEvent: ((NavigationEvent) -> Unit)? = null,
    vehicleState: VehicleUiState? = null,
    onVehicleEvent: ((VehicleEvent) -> Unit)? = null,
) {
    val baseColor = when (widget) {
        DashboardWidget.AdaptiveSpace -> MaterialTheme.colorScheme.primaryContainer
        DashboardWidget.DualZone -> MaterialTheme.colorScheme.secondaryContainer
        DashboardWidget.Media -> MaterialTheme.colorScheme.secondaryContainer
        DashboardWidget.Climate -> MaterialTheme.colorScheme.tertiaryContainer
        DashboardWidget.Navigation -> MaterialTheme.colorScheme.primaryContainer
        DashboardWidget.Vehicle -> MaterialTheme.colorScheme.surfaceContainerHigh
        DashboardWidget.VirtualAssistant -> MaterialTheme.colorScheme.primaryContainer
        DashboardWidget.AssistantGallery -> MaterialTheme.colorScheme.tertiaryContainer
        DashboardWidget.Settings -> MaterialTheme.colorScheme.surfaceContainerHighest
        DashboardWidget.MaterialComponents -> MaterialTheme.colorScheme.secondaryContainer
        DashboardWidget.CustomizedMaterial -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val accent = when (widget) {
        DashboardWidget.AdaptiveSpace -> MaterialTheme.colorScheme.primary
        DashboardWidget.DualZone -> MaterialTheme.colorScheme.secondary
        DashboardWidget.Media -> MaterialTheme.colorScheme.secondary
        DashboardWidget.Climate -> MaterialTheme.colorScheme.tertiary
        DashboardWidget.Navigation -> MaterialTheme.colorScheme.primary
        DashboardWidget.Vehicle -> MaterialTheme.colorScheme.primary
        DashboardWidget.VirtualAssistant -> MaterialTheme.colorScheme.tertiary
        DashboardWidget.AssistantGallery -> MaterialTheme.colorScheme.primary
        DashboardWidget.Settings -> MaterialTheme.colorScheme.outline
        DashboardWidget.MaterialComponents -> MaterialTheme.colorScheme.secondary
        DashboardWidget.CustomizedMaterial -> MaterialTheme.colorScheme.tertiary
    }
    val contentColor = when (widget) {
        DashboardWidget.AdaptiveSpace -> MaterialTheme.colorScheme.onPrimaryContainer
        DashboardWidget.DualZone -> MaterialTheme.colorScheme.onSecondaryContainer
        DashboardWidget.Media -> MaterialTheme.colorScheme.onSecondaryContainer
        DashboardWidget.Climate -> MaterialTheme.colorScheme.onTertiaryContainer
        DashboardWidget.Navigation -> MaterialTheme.colorScheme.onPrimaryContainer
        DashboardWidget.VirtualAssistant -> MaterialTheme.colorScheme.onPrimaryContainer
        DashboardWidget.AssistantGallery -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val cardShape: Shape = WidgetCardShape
    val status = widget.liveStatus(
        mediaState = mediaState,
        climateState = climateState,
        climateTemperature = climateTemperature,
        navigationState = navigationState,
        vehicleState = vehicleState,
    )

    Box(
        modifier = widgetContainerTransform(
            widget = widget,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = modifier
                .clip(cardShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            baseColor.copy(alpha = CarBackgroundTokens.GlassSurfaceAlpha),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.45f),
                            Color.Transparent,
                            accent.copy(alpha = 0.18f),
                        ),
                    ),
                    shape = cardShape,
                )
                .clickable(
                    enabled = !isExpanded,
                    indication = null,
                    interactionSource = null,
                    onClick = onClick,
                )
                .padding(CarDesignTokens.SectionPadding),
        ),
    ) {
        if (!isExpanded) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(CarDesignTokens.TouchTargetSpacing),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(accent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = widget.icon,
                            contentDescription = null,
                            modifier = widgetIconSharedElement(
                                widget = widget,
                                animatedVisibilityScope = animatedVisibilityScope,
                                modifier = Modifier.size(CarDesignTokens.PrimaryIcon),
                            ),
                            tint = contentColor,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = widget.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = widgetTitleSharedElement(
                                widget = widget,
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    WidgetEmbeddedContent(
                        widget = widget,
                        animatedVisibilityScope = animatedVisibilityScope,
                        contentColor = contentColor,
                        mediaState = mediaState,
                        onMediaEvent = onMediaEvent,
                        climateState = climateState,
                        climateTemperature = climateTemperature,
                        onClimateEvent = onClimateEvent,
                        navigationState = navigationState,
                        onNavigationEvent = onNavigationEvent,
                        vehicleState = vehicleState,
                        onVehicleEvent = onVehicleEvent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
