package com.test.design.presentation.ivi.vehicle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.design.data.car.CarVehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class VehicleViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = CarVehicleRepository(application)
    private val _state = MutableStateFlow(VehicleUiState())
    val state: StateFlow<VehicleUiState> = _state.asStateFlow()

    init {
        repository.signals
            .onEach { signals -> setState { withLiveSignals(signals) } }
            .launchIn(viewModelScope)
        repository.start()
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }

    fun onEvent(event: VehicleEvent) {
        if (_state.value.isLive && event.isDemoOnly) return
        when (event) {
            is VehicleEvent.SelectDriveMode -> setState {
                if (isLive) {
                    copy(driveMode = event.mode)
                } else {
                    copy(
                        driveMode = event.mode,
                        regenLevel = event.mode.defaultRegenLevel(),
                    )
                }
            }
            is VehicleEvent.SelectSystem -> setState {
                copy(
                    selectedSystemId = event.id,
                    systems = systems.map { system ->
                        if (system.id != event.id) system
                        else {
                            val next = when {
                                system.valuePercent >= 95 -> 38
                                system.valuePercent >= 70 -> system.valuePercent - 8
                                else -> system.valuePercent + 12
                            }.coerceIn(20, 100)
                            system.copy(
                                valuePercent = next,
                                health = when {
                                    next >= 75 -> SystemHealth.Good
                                    next >= 50 -> SystemHealth.Caution
                                    else -> SystemHealth.Warning
                                },
                            )
                        }
                    },
                )
            }
            VehicleEvent.CycleRegenLevel -> setState {
                val levels = RegenLevel.entries
                val next = levels[(levels.indexOf(regenLevel) + 1) % levels.size]
                copy(regenLevel = next)
            }
            VehicleEvent.ToggleCharging -> setState { copy(isCharging = !isCharging) }
            VehicleEvent.CycleBatteryDemo -> setState {
                val next = when {
                    batteryPercent >= 90 -> 62
                    batteryPercent >= 70 -> 45
                    else -> 88
                }
                val miles = (next * 2.9f).toInt()
                copy(
                    batteryPercent = next,
                    rangeMiles = miles,
                    rangeLabel = "$miles mi",
                    isCharging = next < 70,
                )
            }
        }
    }

    private fun setState(reducer: VehicleUiState.() -> VehicleUiState) {
        _state.update(reducer)
    }
}

private val VehicleEvent.isDemoOnly: Boolean
    get() = this is VehicleEvent.CycleBatteryDemo ||
        this is VehicleEvent.ToggleCharging ||
        this is VehicleEvent.CycleRegenLevel ||
        this is VehicleEvent.SelectSystem
