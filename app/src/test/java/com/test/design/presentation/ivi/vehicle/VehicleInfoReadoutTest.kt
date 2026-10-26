package com.test.design.presentation.ivi.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleInfoReadoutTest {

    @Test
    fun demoReadoutUsesRangeEfficiencyAndDriveGear() {
        val readout = VehicleUiState().toInfoReadout()

        assertEquals("240", readout.remainingValue)
        assertEquals("mi", readout.remainingUnit)
        assertEquals("263", readout.averageValue)
        assertEquals("Wh/mi", readout.averageUnit)
        assertEquals("77.0", readout.capacityValue)
        assertEquals("kWh", readout.capacityUnit)
        assertEquals(0.82f, readout.chargeFraction, 0.001f)
        assertEquals("0", readout.speedValue)
        assertEquals("mph", readout.speedUnit)
        assertEquals("D", readout.selectedGear)
        assertFalse(readout.alertOn)
        assertFalse(readout.batteryOn)
    }

    @Test
    fun liveReadoutUsesReportedSpeedGearAndCapacity() {
        val readout = VehicleUiState(
            isLive = true,
            rangeLabel = "204 km",
            rangeMiles = 127,
            batteryPercent = 80,
            speedLabel = "84 km/h",
            gearLabel = "N",
            packCapacityKwh = 35.5f,
            efficiencyMpkWh = 3.8f,
        ).toInfoReadout()

        assertEquals("204", readout.remainingValue)
        assertEquals("km", readout.remainingUnit)
        assertEquals("—", readout.averageValue)
        assertEquals("Wh/km", readout.averageUnit)
        assertEquals("35.5", readout.capacityValue)
        assertEquals("84", readout.speedValue)
        assertEquals("km/h", readout.speedUnit)
        assertEquals("N", readout.selectedGear)
    }

    @Test
    fun alertAndBatteryFollowFaultsAndCharging() {
        val readout = VehicleUiState(
            isCharging = true,
            batteryPercent = 40,
            tires = listOf(
                TireReading("fl", "FL", "Front left", "20 psi", 140f, criticallyLow = true),
            ),
        ).toInfoReadout()

        assertTrue(readout.alertOn)
        assertTrue(readout.batteryOn)
    }
}
