package com.test.design.presentation.ivi.dashboard

import com.test.design.presentation.ivi.climate.ClimateUiState
import com.test.design.presentation.ivi.climate.formatTemperature
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.navigation.NavigationUiState
import com.test.design.presentation.ivi.vehicle.VehicleUiState

fun DashboardWidget.liveStatus(
    mediaState: MediaUiState? = null,
    climateState: ClimateUiState? = null,
    climateTemperature: Float? = null,
    navigationState: NavigationUiState? = null,
    vehicleState: VehicleUiState? = null,
): String = when (this) {
    DashboardWidget.AdaptiveSpace -> "Play demo · map → media → split → parking"
    DashboardWidget.DualZone -> "Driver restricted · Passenger full UI"
    DashboardWidget.Media -> mediaState?.let { state ->
        val play = if (state.isPlaying) "Playing" else "Paused"
        val source = if (state.isLive) {
            state.sourceLabel.ifBlank { "Live" }
        } else {
            "Simulated"
        }
        "$source · $play · ${state.currentTrack.title}"
    } ?: subtitle
    DashboardWidget.Climate -> climateState?.let { state ->
        val temp = climateTemperature ?: state.temperatureCelsius
        val source = if (state.isLive) "Live" else "Simulated"
        buildString {
            append(source)
            append(" · ")
            append(state.formatTemperature(temp))
            if (state.capabilities.hasAc) {
                append(" · ")
                append(if (state.isAcEnabled) "A/C on" else "A/C off")
            }
            if (state.capabilities.hasFanSpeed) {
                append(" · ")
                append(if (state.fanSpeed <= 0) "Fan off" else "Fan ${state.fanSpeed}")
            }
        }
    } ?: subtitle
    DashboardWidget.Navigation -> navigationState?.let { state ->
        "Simulated · ${state.destination} · ${state.etaMinutes} min · ${state.distanceRemaining}"
    } ?: subtitle
    DashboardWidget.Vehicle -> vehicleState?.let { state ->
        val source = if (state.isLive) "Live" else "Simulated"
        val charge = if (state.isCharging) "Charging" else state.driveMode.label
        "$source · ${state.batteryPercent}% · ${state.rangeLabel} · $charge"
    } ?: subtitle
    DashboardWidget.VirtualAssistant -> "Eyes · STT · TTS lip-sync · cluster hand-off"
    DashboardWidget.AssistantGallery -> "Chrome styles · moods · opaque stage · auto-cycle"
    DashboardWidget.MaterialComponents -> "Browse Material 3 components"
    DashboardWidget.CustomizedMaterial -> "OEM brand system preview"
    DashboardWidget.Settings -> "Driving UX · Motion Studio · Display"
}
