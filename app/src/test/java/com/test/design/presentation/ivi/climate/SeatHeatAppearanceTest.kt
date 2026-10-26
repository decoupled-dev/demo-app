package com.test.design.presentation.ivi.climate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeatHeatAppearanceTest {

    @Test
    fun threeStepHeaterUsesOffLowMedHigh() {
        assertEquals("Off", seatHeatStateLabel(0, 3))
        assertEquals("Low", seatHeatStateLabel(1, 3))
        assertEquals("Med", seatHeatStateLabel(2, 3))
        assertEquals("High", seatHeatStateLabel(3, 3))
        assertEquals("High", seatHeatStateLabel(9, 3))
    }

    @Test
    fun otherStepCountsUseTheLevelNumber() {
        assertEquals("Off", seatHeatStateLabel(0, 1))
        assertEquals("1", seatHeatStateLabel(1, 1))
        assertEquals("2", seatHeatStateLabel(2, 5))
        assertEquals("5", seatHeatStateLabel(8, 5))
    }

    @Test
    fun intensityIsZeroOffAndFullAtMax() {
        assertEquals(0f, seatHeatIntensity(0, 3))
        assertEquals(0f, seatHeatIntensity(-1, 3))
        assertEquals(0f, seatHeatIntensity(2, 0))
        assertEquals(1f, seatHeatIntensity(3, 3))
        assertEquals(1f, seatHeatIntensity(1, 1))
        assertTrue(seatHeatIntensity(1, 3) < seatHeatIntensity(2, 3))
    }

    @Test
    fun higherLevelsShiftTowardTheHotterFill() {
        val low = seatHeatColors(1, 3, lightCanvas = false)
        val high = seatHeatColors(3, 3, lightCanvas = false)
        assertTrue(high.fill.green < low.fill.green)
        assertTrue(seatHeatColors(0, 3, lightCanvas = true).fill != high.fill)
    }
}
