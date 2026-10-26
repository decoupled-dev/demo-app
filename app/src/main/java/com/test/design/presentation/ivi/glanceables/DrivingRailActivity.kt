package com.test.design.presentation.ivi.glanceables

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.core.LocalDrivingUxState
import com.test.design.core.cluster.ClusterUiState
import com.test.design.presentation.assistant.VirtualAssistantActivity
import com.test.design.presentation.ivi.climate.ClimateViewModel
import com.test.design.presentation.ivi.map.MapIntents
import com.test.design.presentation.ivi.media.MediaViewModel
import com.test.design.presentation.ivi.vehicle.VehicleViewModel

/**
 * Dewd `widget_panel` host — left rail glanceables in one TaskPanel activity.
 * SystemUI owns status/nav bars; this only draws Design content.
 */
class DrivingRailActivity : GlanceableActivity() {

    private val vehicleViewModel: VehicleViewModel by viewModels()
    private val mediaViewModel: MediaViewModel by viewModels()
    private val climateViewModel: ClimateViewModel by viewModels()

    @Composable
    override fun GlanceContent() {
        val vehicleState by vehicleViewModel.state.collectAsStateWithLifecycle()
        val mediaState by mediaViewModel.state.collectAsStateWithLifecycle()
        val climateState by climateViewModel.state.collectAsStateWithLifecycle()
        val cluster = ClusterUiState.fromDrivingUx(LocalDrivingUxState.current)

        GlanceRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DrivingStatusGlance(
                    speedMph = cluster.speedMph,
                    speedLimitMph = cluster.speedLimitMph,
                    gear = cluster.gear,
                    batteryPercent = vehicleState.batteryPercent,
                    rangeLabel = vehicleState.rangeLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
                MediaGlance(
                    mediaState = mediaState,
                    onMediaEvent = mediaViewModel::onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                )
                AppsGlance(
                    onOpenDashboard = {
                        startActivity(
                            MapIntents.openMain(this@DrivingRailActivity, openDashboard = true).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    },
                    onOpenAssistant = {
                        VirtualAssistantActivity.launch(this@DrivingRailActivity)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                ClimateGlanceBar(
                    climateState = climateState,
                    climateTemperature = climateState.temperatureCelsius,
                    onClimateEvent = climateViewModel::onEvent,
                    onExpandClimate = {
                        startActivity(
                            Intent(this@DrivingRailActivity, ClimatePanelActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
