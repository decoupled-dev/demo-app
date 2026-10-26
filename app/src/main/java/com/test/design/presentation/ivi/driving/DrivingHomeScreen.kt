package com.test.design.presentation.ivi.driving

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.design.presentation.activityViewModel
import com.test.design.presentation.assistant.AssistantGradientBlurHost
import com.test.design.presentation.assistant.AssistantPresentation
import com.test.design.presentation.assistant.VirtualAssistantScreen
import com.test.design.presentation.assistant.gallery.AssistantUiGalleryScreen
import com.test.design.presentation.ivi.IviExpressiveTheme
import com.test.design.presentation.ivi.adaptivespace.AdaptiveSpaceScreen
import com.test.design.presentation.ivi.climate.ClimateControlScreen
import com.test.design.presentation.ivi.climate.ClimateViewModel
import com.test.design.presentation.ivi.dashboard.DashboardEvent
import com.test.design.presentation.ivi.dashboard.DashboardViewModel
import com.test.design.presentation.ivi.dashboard.components.DrivingDashboardLayout
import com.test.design.presentation.ivi.dashboard.components.floatingSystemChromePadding
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dualzone.DualZoneScreen
import com.test.design.presentation.ivi.map.MapLaunchConfig
import com.test.design.presentation.ivi.media.MediaPlayerScreen
import com.test.design.presentation.ivi.media.MediaViewModel
import com.test.design.presentation.ivi.navigation.NavigationScreen
import com.test.design.presentation.ivi.navigation.NavigationViewModel
import com.test.design.presentation.ivi.navigation.components.DefaultMapCenter
import com.test.design.presentation.ivi.navigation.components.OpenMapBackground
import com.test.design.presentation.ivi.navigation.components.mapChromeLayer
import com.test.design.presentation.ivi.vehicle.VehicleScreen
import com.test.design.presentation.ivi.vehicle.VehicleViewModel
import com.test.design.presentation.material.CustomizedMaterialComponentsScreen
import com.test.design.presentation.material.MaterialComponentsScreen
import com.test.design.presentation.settings.SettingsScreen

/**
 * Tesla-like map-first driving home — start destination.
 * Opens the original widget-list dashboard via [onOpenWidgetDashboard].
 *
 * Virtual Assistant overlays the driving home (does not replace it) so the
 * map / chrome stay visible under a light blackish scrim.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DrivingHomeScreen(
    onOpenWidgetDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    mapLaunchConfig: MapLaunchConfig = MapLaunchConfig.default(),
    onOpenMain: (() -> Unit)? = null,
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

    LaunchedEffect(mapLaunchConfig.expandNavigation) {
        if (mapLaunchConfig.expandNavigation) {
            dashboardViewModel.onEvent(DashboardEvent.WidgetTapped(DashboardWidget.Navigation))
        }
    }

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
        // Keep home / other sheets under the assistant overlay.
        val pageWidget = dashboardState.expandedWidget.takeUnless {
            it == DashboardWidget.VirtualAssistant || it == DashboardWidget.AssistantGallery
        }

        BackHandler(enabled = dashboardState.expandedWidget != null, onBack = collapseWidget)
        BackHandler(
            enabled = onOpenMain != null && dashboardState.expandedWidget == null,
            onBack = { onOpenMain?.invoke() },
        )
        PredictiveBackHandler(enabled = dashboardState.expandedWidget != null) { progress ->
            try {
                progress.collect { }
                collapseWidget()
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Gesture cancelled — keep expanded state.
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            // Keep the map outside the shared-element overlay so that morph
            // does not flatten the texture view to black.
            OpenMapBackground(
                modifier = Modifier.fillMaxSize(),
                center = mapLaunchConfig.center ?: DefaultMapCenter,
                zoom = mapLaunchConfig.zoom,
                showRoute = mapLaunchConfig.showRoute,
                interactive = true,
            )
            SharedTransitionLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .mapChromeLayer(),
            ) {
            Box(Modifier.fillMaxSize()) {
                AssistantGradientBlurHost(blurred = hostBlurred) {
                    AnimatedContent(
                        targetState = pageWidget,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val slide = tween<IntOffset>(
                                durationMillis = 420,
                                easing = FastOutSlowInEasing,
                            )
                            val openingMap = targetState == DashboardWidget.Navigation
                            val closingMap = initialState == DashboardWidget.Navigation &&
                                targetState == null
                            when {
                                // Search bar, sidebar, and climate bar leave together to the left.
                                // Climate keeps its shared-element morph.
                                openingMap -> slideInHorizontally(slide) { it } togetherWith
                                    slideOutHorizontally(slide) { -it }
                                closingMap -> slideInHorizontally(slide) { -it } togetherWith
                                    slideOutHorizontally(slide) { it }
                                else -> EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        label = "driving_home_container_transform",
                    ) { expandedWidget ->
                    when (expandedWidget) {
                        null -> DrivingDashboardLayout(
                            vehicleState = vehicleState,
                            navigationState = navigationState,
                            mediaState = mediaState,
                            climateState = climateState,
                            climateTemperature = climateViewModel.activeTemperature(),
                            onEvent = dashboardViewModel::onEvent,
                            onMediaEvent = mediaViewModel::onEvent,
                            onClimateEvent = climateViewModel::onEvent,
                            onOpenWidgetDashboard = onOpenWidgetDashboard,
                            animatedVisibilityScope = this@AnimatedContent,
                            mapCenter = mapLaunchConfig.center,
                            initialMapZoom = mapLaunchConfig.zoom,
                            showMapRoute = mapLaunchConfig.showRoute,
                            embedMap = false,
                            onOpenMain = onOpenMain,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (expandedWidget == DashboardWidget.Navigation) {
                                        Modifier
                                    } else {
                                        Modifier.floatingSystemChromePadding()
                                    },
                                ),
                        ) {
                            when (expandedWidget) {
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
                                    embedMap = false,
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
                                DashboardWidget.CustomizedMaterial ->
                                    CustomizedMaterialComponentsScreen(
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
}
