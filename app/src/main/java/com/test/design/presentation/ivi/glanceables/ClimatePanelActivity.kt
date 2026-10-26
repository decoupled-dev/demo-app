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
import com.test.design.presentation.ivi.climate.ClimateControlScreen
import com.test.design.presentation.ivi.climate.ClimateViewModel

/** Standalone full climate screen — app launcher, [ACTION_OPEN_CLIMATE], or component name. */
class ClimatePanelActivity : StandalonePanelActivity() {

    private val climateViewModel: ClimateViewModel by viewModels()

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun GlanceContent() {
        val climateState by climateViewModel.state.collectAsStateWithLifecycle()
        GlanceRoot {
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    ClimateControlScreen(
                        uiState = climateState,
                        activeTemperature = climateState.temperatureCelsius,
                        onEvent = climateViewModel::onEvent,
                        onBack = ::navigateHomeAndFinish,
                        animatedVisibilityScope = this,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
