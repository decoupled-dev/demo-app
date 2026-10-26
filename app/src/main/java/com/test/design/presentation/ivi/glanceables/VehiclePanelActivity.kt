package com.test.design.presentation.ivi.glanceables

import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.presentation.ivi.vehicle.VehicleScreen
import com.test.design.presentation.ivi.vehicle.VehicleViewModel

/** Standalone full vehicle info screen — app launcher, [ACTION_OPEN_VEHICLE], or component name. */
class VehiclePanelActivity : StandalonePanelActivity() {

    private val vehicleViewModel: VehicleViewModel by viewModels()

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun GlanceContent() {
        val vehicleState by vehicleViewModel.state.collectAsStateWithLifecycle()
        GlanceRoot {
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    VehicleScreen(
                        uiState = vehicleState,
                        onEvent = vehicleViewModel::onEvent,
                        onBack = ::navigateHomeAndFinish,
                        animatedVisibilityScope = this,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
