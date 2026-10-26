package com.test.design.data.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleSignalsTest {

    @Test
    fun batteryPercentUsesCapacityWhenPackIsKnown() {
        assertEquals(80, batteryPercent(remainingWh = 60_000f, capacityWh = 75_000f))
    }

    @Test
    fun batteryPercentAcceptsDirectPercentageWithoutCapacity() {
        assertEquals(82, batteryPercent(remainingWh = 82f, capacityWh = null))
    }

    @Test
    fun batteryPercentIgnoresWattHoursWithoutCapacity() {
        assertNull(batteryPercent(remainingWh = 40_000f, capacityWh = null))
    }

    @Test
    fun fuelPercentUsesTankCapacity() {
        assertEquals(50, fuelPercent(levelMilliliters = 25_000f, capacityMilliliters = 50_000f))
    }

    @Test
    fun chargeRateTreatsLargeValuesAsMilliwatts() {
        assertEquals(11f, chargeRateKilowatts(11_000_000f)!!, 0.01f)
    }

    @Test
    fun chargeRateTreatsSmallerValuesAsWatts() {
        assertEquals(7.2f, chargeRateKilowatts(7_200f)!!, 0.01f)
    }

    @Test
    fun displayFormatsTiresSpeedAndRangeFromCanonicalUnits() {
        val display = VehicleSignals(
            connected = true,
            rangeMeters = 160_934.4f,
            odometerKilometers = 16.09344f,
            speedMetersPerSecond = 10f,
            gear = VehicleGearKind.Drive,
            outsideCelsius = 21f,
            distanceUnit = DistanceUnit.Miles,
            speedInMilesPerHour = true,
            pressureUnit = PressureUnit.Psi,
            tires = listOf(
                TireSignal(WheelPosition.RearLeft, kiloPascals = 140f, criticallyLow = false),
                TireSignal(WheelPosition.FrontLeft, kiloPascals = 234f, criticallyLow = false),
            ),
            tiresAvailable = true,
        ).toDisplay()

        assertEquals(100, display.rangeMiles)
        assertEquals("100 mi", display.rangeLabel)
        assertEquals("10 mi", display.odometerLabel)
        assertEquals("22 mph", display.speedLabel)
        assertEquals("D", display.gearLabel)
        assertEquals("21°C", display.outsideTempLabel)
        assertEquals(WheelPosition.FrontLeft, display.tires.first().position)
        assertEquals("34 psi", display.tires.first().pressureLabel)
        assertEquals(234f, display.tires.first().kiloPascals, 0.01f)
        assertEquals(true, display.tires.last().criticallyLow)
    }

    @Test
    fun chargingFollowsPositiveRateAndFuelBecomesEnergyWhenNoBattery() {
        val display = VehicleSignals(
            connected = true,
            fuelMilliliters = 10f,
            chargePortConnected = true,
            chargeRateRaw = 11_000f,
            distanceUnit = DistanceUnit.Kilometers,
        ).toDisplay()

        assertEquals(10, display.fuelPercent)
        assertNull(display.batteryPercent)
        assertEquals("Fuel", display.energyLabel)
        assertEquals(true, display.isCharging)
        assertNull(display.chargePortLabel)
        assertEquals(11f, display.chargeRateKw!!, 0.01f)
    }
}
