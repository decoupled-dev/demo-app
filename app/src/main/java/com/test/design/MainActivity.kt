package com.test.design

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.presentation.DesignAppShell
import com.test.design.presentation.ivi.climate.ClimateControlScreen
import com.test.design.presentation.ivi.climate.ClimateViewModel
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.vehicle.VehicleScreen
import com.test.design.presentation.ivi.vehicle.VehicleViewModel

/** Intent action to open the climate screen. */
const val ACTION_OPEN_CLIMATE = "com.test.design.action.OPEN_CLIMATE"

/** Intent action to open the vehicle info screen. */
const val ACTION_OPEN_VEHICLE = "com.test.design.action.OPEN_VEHICLE"

/** Single host for the climate and vehicle info screens, switched via a navigation rail. */
class MainActivity : ComponentActivity() {

    private val climateViewModel: ClimateViewModel by viewModels()
    private val vehicleViewModel: VehicleViewModel by viewModels()
    private val selectedScreen = mutableStateOf(DashboardWidget.Climate)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the activity buffer opaque — transparent BLAST buffers read as a black screen
        // on the AAOS emulator (screencap pixels were RGBA 0,0,0,0).
        window.setFormat(PixelFormat.OPAQUE)
        window.setBackgroundDrawableResource(R.color.canvas_background)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        applyIntent(intent)

        setContent {
            val selected by selectedScreen
            val climateState by climateViewModel.state.collectAsStateWithLifecycle()
            val vehicleState by vehicleViewModel.state.collectAsStateWithLifecycle()

            DesignAppShell {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        DashboardWidget.entries.forEach { screen ->
                            NavigationRailItem(
                                selected = screen == selected,
                                onClick = { selectedScreen.value = screen },
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(screen.title) },
                            )
                        }
                    }
                    SharedTransitionLayout(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedContent(
                            targetState = selected,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen",
                        ) { screen ->
                            when (screen) {
                                DashboardWidget.Climate -> ClimateControlScreen(
                                    uiState = climateState,
                                    activeTemperature = climateState.temperatureCelsius,
                                    onEvent = climateViewModel::onEvent,
                                    onBack = {},
                                    animatedVisibilityScope = this@AnimatedContent,
                                    showBack = false,
                                    applyWindowInsets = false,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                DashboardWidget.Vehicle -> VehicleScreen(
                                    uiState = vehicleState,
                                    onEvent = vehicleViewModel::onEvent,
                                    onBack = {},
                                    animatedVisibilityScope = this@AnimatedContent,
                                    showBack = false,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_CLIMATE -> selectedScreen.value = DashboardWidget.Climate
            ACTION_OPEN_VEHICLE -> selectedScreen.value = DashboardWidget.Vehicle
        }
    }
}
