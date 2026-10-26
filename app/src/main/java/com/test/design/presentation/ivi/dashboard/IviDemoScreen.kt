package com.test.design.presentation.ivi.dashboard

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.design.core.DrivingUxState
import com.test.design.core.LocalDrivingUxState
import com.test.design.presentation.activityViewModel
import com.test.design.presentation.assistant.AssistantGradientBlurHost
import com.test.design.presentation.assistant.AssistantPresentation
import com.test.design.presentation.assistant.VirtualAssistantScreen
import com.test.design.presentation.assistant.gallery.AssistantUiGalleryScreen
import com.test.design.presentation.ivi.IviExpressiveTheme
import com.test.design.presentation.ivi.adaptivespace.AdaptiveSpaceScreen
import com.test.design.presentation.ivi.climate.ClimateControlScreen
import com.test.design.presentation.ivi.climate.ClimateViewModel
import com.test.design.presentation.ivi.dashboard.components.DashboardWidgetCard
import com.test.design.presentation.ivi.dashboard.components.DashboardWidgetGrid
import com.test.design.presentation.ivi.dashboard.components.floatingSystemChromePadding
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dualzone.DualZoneScreen
import com.test.design.presentation.ivi.media.MediaPlayerScreen
import com.test.design.presentation.ivi.media.MediaViewModel
import com.test.design.presentation.ivi.navigation.NavigationScreen
import com.test.design.presentation.ivi.navigation.NavigationViewModel
import com.test.design.presentation.ivi.vehicle.VehicleScreen
import com.test.design.presentation.ivi.vehicle.VehicleViewModel
import com.test.design.presentation.material.CustomizedMaterialComponentsScreen
import com.test.design.presentation.material.MaterialComponentsScreen
import com.test.design.presentation.settings.SettingsScreen
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget

/**
 * Original widget-list dashboard hub with container transforms into feature screens.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun IviDemoScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    dashboardViewModel: DashboardViewModel = activityViewModel(),
    climateViewModel: ClimateViewModel = activityViewModel(),
    mediaViewModel: MediaViewModel = viewModel(),
    navigationViewModel: NavigationViewModel = viewModel(),
    vehicleViewModel: VehicleViewModel = viewModel(),
) {
    val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()
    val climateState by climateViewModel.state.collectAsStateWithLifecycle()
    val mediaState by mediaViewModel.state.collectAsStateWithLifecycle()
    val navigationState by navigationViewModel.state.collectAsStateWithLifecycle()
    val vehicleState by vehicleViewModel.state.collectAsStateWithLifecycle()

    IviExpressiveTheme {
        val collapseWidget = { dashboardViewModel.onEvent(DashboardEvent.CollapseWidget) }
        val assistantOpen = dashboardState.expandedWidget == DashboardWidget.VirtualAssistant
        val galleryOpen = dashboardState.expandedWidget == DashboardWidget.AssistantGallery
        var assistantPresentation by remember {
            mutableStateOf(AssistantPresentation.Compact)
        }
        LaunchedEffect(assistantOpen) {
            assistantPresentation = if (assistantOpen) {
                AssistantPresentation.Immersive
            } else {
                AssistantPresentation.Compact
            }
        }
        val hostBlurred = galleryOpen ||
            (assistantOpen && assistantPresentation == AssistantPresentation.Immersive)
        val pageWidget = dashboardState.expandedWidget.takeUnless {
            it == DashboardWidget.VirtualAssistant || it == DashboardWidget.AssistantGallery
        }
        val handleBack: () -> Unit = {
            if (dashboardState.expandedWidget != null) {
                collapseWidget()
            } else {
                onBack?.invoke()
            }
        }
        BackHandler(
            enabled = dashboardState.expandedWidget != null || onBack != null,
            onBack = handleBack,
        )
        PredictiveBackHandler(enabled = dashboardState.expandedWidget != null) { progress ->
            try {
                progress.collect { }
                collapseWidget()
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Gesture cancelled — keep expanded state.
            }
        }

        SharedTransitionLayout(
            modifier = modifier
                .fillMaxSize()
                .floatingSystemChromePadding(),
        ) {
            Box(Modifier.fillMaxSize()) {
                AssistantGradientBlurHost(blurred = hostBlurred) {
                    AnimatedContent(
                        targetState = pageWidget,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None
                        },
                        label = "dashboard_container_transform",
                    ) { expandedWidget ->
                    when (expandedWidget) {
                        null -> DashboardHubContent(
                            state = dashboardState,
                            onEvent = dashboardViewModel::onEvent,
                            animatedVisibilityScope = this@AnimatedContent,
                            mediaState = mediaState,
                            onMediaEvent = mediaViewModel::onEvent,
                            climateState = climateState,
                            climateTemperature = climateViewModel.activeTemperature(),
                            onClimateEvent = climateViewModel::onEvent,
                            navigationState = navigationState,
                            onNavigationEvent = navigationViewModel::onEvent,
                            vehicleState = vehicleState,
                            onVehicleEvent = vehicleViewModel::onEvent,
                            onBack = onBack,
                        )
                        DashboardWidget.AdaptiveSpace -> AdaptiveSpaceScreen(
                            mediaState = mediaState,
                            onMediaEvent = mediaViewModel::onEvent,
                            onBack = collapseWidget,
                            animatedVisibilityScope = this@AnimatedContent,
                        )
                        DashboardWidget.DualZone -> DualZoneScreen(
                            mediaState = mediaState,
                            onMediaEvent = mediaViewModel::onEvent,
                            navigationState = navigationState,
                            onBack = collapseWidget,
                            animatedVisibilityScope = this@AnimatedContent,
                        )
                        DashboardWidget.Climate -> ClimateControlScreen(
                            uiState = climateState,
                            activeTemperature = climateViewModel.activeTemperature(),
                            onEvent = climateViewModel::onEvent,
                            onBack = collapseWidget,
                            animatedVisibilityScope = this@AnimatedContent,
                        )
                        DashboardWidget.Media -> MediaPlayerScreen(
                            uiState = mediaState,
                            onEvent = mediaViewModel::onEvent,
                            onBack = collapseWidget,
                            animatedVisibilityScope = this@AnimatedContent,
                        )
                        DashboardWidget.Navigation -> NavigationScreen(
                            uiState = navigationState,
                            onEvent = navigationViewModel::onEvent,
                            onBack = collapseWidget,
                            animatedVisibilityScope = this@AnimatedContent,
                        )
                        DashboardWidget.Vehicle -> VehicleScreen(
                            uiState = vehicleState,
                            onEvent = vehicleViewModel::onEvent,
                            onBack = collapseWidget,
                            animatedVisibilityScope = this@AnimatedContent,
                        )
                        DashboardWidget.MaterialComponents -> MaterialComponentsScreen(
                            onBack = collapseWidget,
                        )
                        DashboardWidget.CustomizedMaterial -> CustomizedMaterialComponentsScreen(
                            onBack = collapseWidget,
                        )
                        DashboardWidget.Settings -> SettingsScreen(
                            onBack = collapseWidget,
                            animatedVisibilityScope = this@AnimatedContent,
                        )
                        DashboardWidget.VirtualAssistant,
                        DashboardWidget.AssistantGallery -> Unit
                    }
                }
                }

                if (assistantOpen) {
                    VirtualAssistantScreen(
                        onBack = collapseWidget,
                        modifier = Modifier.fillMaxSize(),
                        onPresentationChanged = { assistantPresentation = it },
                    )
                }
                if (galleryOpen) {
                    AssistantUiGalleryScreen(
                        onClose = collapseWidget,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.DashboardHubContent(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    mediaState: com.test.design.presentation.ivi.media.MediaUiState,
    onMediaEvent: (com.test.design.presentation.ivi.media.MediaEvent) -> Unit,
    climateState: com.test.design.presentation.ivi.climate.ClimateUiState,
    climateTemperature: Float,
    onClimateEvent: (com.test.design.presentation.ivi.climate.ClimateEvent) -> Unit,
    navigationState: com.test.design.presentation.ivi.navigation.NavigationUiState,
    onNavigationEvent: (com.test.design.presentation.ivi.navigation.NavigationEvent) -> Unit,
    vehicleState: com.test.design.presentation.ivi.vehicle.VehicleUiState,
    onVehicleEvent: (com.test.design.presentation.ivi.vehicle.VehicleEvent) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val drivingUx = LocalDrivingUxState.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CarDesignTokens.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(CarDesignTokens.TouchTargetSpacing),
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.carTouchTarget(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to driving home",
                    )
                }
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (drivingUx == DrivingUxState.Restricted) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Restricted driving UX — prefer Dual Zone / Adaptive Space glanceables. Deep apps stay available for demo, but motion is Standard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
        DashboardWidgetGrid(
            widgets = state.widgets,
            modifier = Modifier.fillMaxSize(),
            widgetContent = { widget, widgetModifier ->
                DashboardWidgetCard(
                    widget = widget,
                    onClick = { onEvent(DashboardEvent.WidgetTapped(widget)) },
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = widgetModifier,
                    mediaState = mediaState,
                    onMediaEvent = onMediaEvent,
                    climateState = climateState,
                    climateTemperature = climateTemperature,
                    onClimateEvent = onClimateEvent,
                    navigationState = navigationState,
                    onNavigationEvent = onNavigationEvent,
                    vehicleState = vehicleState,
                    onVehicleEvent = onVehicleEvent,
                )
            },
        )
    }
}
