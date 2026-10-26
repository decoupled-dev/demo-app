package com.test.design.presentation.ivi.glanceables

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.presentation.ivi.climate.ClimateViewModel

/**
 * Climate quarter hosted by the ClimateVehicle Scalable UI panel.
 * A tap on the card opens [ClimatePanelActivity] as a normal full activity.
 */
class ClimateHomePanelActivity : GlanceableActivity() {

    private val climateViewModel: ClimateViewModel by viewModels()

    @Composable
    override fun GlanceContent() {
        val climateState by climateViewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        GlanceRoot {
            ClimateQuarter(
                state = climateState,
                onEvent = climateViewModel::onEvent,
                onOpenMain = { openFullActivity(context, ClimatePanelActivity::class.java, ACTION_OPEN_CLIMATE) },
            )
        }
    }
}
