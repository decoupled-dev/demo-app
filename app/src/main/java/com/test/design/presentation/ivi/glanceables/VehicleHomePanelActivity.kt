package com.test.design.presentation.ivi.glanceables

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.presentation.ivi.vehicle.VehicleViewModel

/**
 * Vehicle quarter hosted by the ClimateVehicle Scalable UI panel.
 * A tap on the card opens [VehiclePanelActivity] as a normal full activity.
 */
class VehicleHomePanelActivity : GlanceableActivity() {

    private val vehicleViewModel: VehicleViewModel by viewModels()

    @Composable
    override fun GlanceContent() {
        val vehicleState by vehicleViewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        GlanceRoot {
            VehicleQuarter(
                state = vehicleState,
                onOpenMain = { openFullActivity(context, VehiclePanelActivity::class.java, ACTION_OPEN_VEHICLE) },
                onEvent = vehicleViewModel::onEvent,
            )
        }
    }
}
