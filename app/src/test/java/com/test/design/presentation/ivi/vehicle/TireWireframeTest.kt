package com.test.design.presentation.ivi.vehicle

import org.junit.Assert.assertEquals
import org.junit.Test

class TireWireframeTest {

    @Test
    fun missingTiresUseTheReferenceReadout() {
        val wireframe = VehicleUiState().toTireWireframe()
        assertEquals(36, wireframe.frontLeft.psi)
        assertEquals(39, wireframe.frontRight.psi)
        assertEquals(35, wireframe.rearLeft.psi)
        assertEquals(37, wireframe.rearRight.psi)
        assertEquals(TireStatus.Normal, wireframe.frontLeft.status)
        assertEquals(TireStatus.Critical, wireframe.frontRight.status)
        assertEquals(TireStatus.Warning, wireframe.rearRight.status)
        assertEquals("45 °C", wireframe.frontLeft.temperature)
        assertEquals("10 min ago", wireframe.rearLeft.updated)
    }

    @Test
    fun liveCornersConvertKilopascalsToPsi() {
        val wireframe = VehicleUiState(
            isLive = true,
            outsideTempLabel = "21°C",
            tires = listOf(
                TireReading("fl", "FL", "Front left", "172 kPa", 172.4f, criticallyLow = true),
                TireReading("fr", "FR", "Front right", "214 kPa", 213.7f, criticallyLow = false),
                TireReading("rl", "RL", "Rear left", "234 kPa", 234.4f, criticallyLow = false),
                TireReading("rr", "RR", "Rear right", "303 kPa", 303.4f, criticallyLow = false),
            ),
        ).toTireWireframe()
        assertEquals(25, wireframe.frontLeft.psi)
        assertEquals(TireStatus.Critical, wireframe.frontLeft.status)
        assertEquals(31, wireframe.frontRight.psi)
        assertEquals(TireStatus.Normal, wireframe.frontRight.status)
        assertEquals(34, wireframe.rearLeft.psi)
        assertEquals(44, wireframe.rearRight.psi)
        assertEquals(TireStatus.Warning, wireframe.rearRight.status)
        assertEquals("21 °C", wireframe.frontLeft.temperature)
        assertEquals("Front\nLeft", wireframe.frontLeft.title)
        assertEquals("Just now", wireframe.frontLeft.updated)
    }

    @Test
    fun statusBandsFollowPlacardPressure() {
        assertEquals(TireStatus.Critical, tireStatus(psi = 22, criticallyLow = false))
        assertEquals(TireStatus.Warning, tireStatus(psi = 26, criticallyLow = false))
        assertEquals(TireStatus.Normal, tireStatus(psi = 29, criticallyLow = false))
        assertEquals(TireStatus.Normal, tireStatus(psi = 36, criticallyLow = false))
        assertEquals(TireStatus.Warning, tireStatus(psi = 43, criticallyLow = false))
        assertEquals(TireStatus.Critical, tireStatus(psi = 36, criticallyLow = true))
    }
}
