package com.test.design.presentation.ivi.vehicle

import com.test.design.data.car.DistanceUnit
import com.test.design.data.car.VehicleSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleLiveStateTest {

    @Test
    fun liveSignalsReplaceDemoValuesAndKeepDriveMode() {
        val updated = VehicleUiState(driveMode = DriveMode.Sport).withLiveSignals(
            VehicleSignals(
                connected = true,
                batteryRemainingWh = 80f,
                rangeMeters = 160_934.4f,
                distanceUnit = DistanceUnit.Miles,
            ),
        )

        assertTrue(updated.isLive)
        assertEquals(DriveMode.Sport, updated.driveMode)
        assertEquals(80, updated.batteryPercent)
        assertEquals(100, updated.rangeMiles)
        assertEquals("100 mi", updated.rangeLabel)
        assertEquals("Battery", updated.energyLabel)
        assertEquals(null, updated.packCapacityKwh)
    }

    @Test
    fun livePackCapacityComesFromWattHours() {
        val updated = VehicleUiState().withLiveSignals(
            VehicleSignals(
                connected = true,
                batteryRemainingWh = 28_400f,
                batteryCapacityWh = 35_500f,
            ),
        )

        assertEquals(80, updated.batteryPercent)
        assertEquals(35.5f, updated.packCapacityKwh!!, 0.05f)
    }

    @Test
    fun disconnectRestoresDemoWithoutDroppingDriveMode() {
        val live = VehicleUiState(driveMode = DriveMode.Eco).withLiveSignals(
            VehicleSignals(connected = true, batteryRemainingWh = 40f),
        )
        val restored = live.withLiveSignals(VehicleSignals(connected = false))

        assertFalse(restored.isLive)
        assertEquals(DriveMode.Eco, restored.driveMode)
        assertEquals(82, restored.batteryPercent)
        assertTrue(restored.tires.isEmpty())
    }

    @Test
    fun demoStateIgnoresEmptySnapshot() {
        val demo = VehicleUiState()
        assertEquals(demo, demo.withLiveSignals(VehicleSignals()))
    }

    @Test
    fun tireFillTracksPressureUpToVisualFull() {
        assertEquals(0f, tireFillFraction(-10f), 0.001f)
        assertEquals(241f / TireVisualFullKpa, tireFillFraction(TireRecommendedKpa), 0.001f)
        assertEquals(1f, tireFillFraction(400f), 0.001f)
    }
}
