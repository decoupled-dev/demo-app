package com.test.design.presentation.ivi.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMeterTest {

    @Test
    fun odometerReadsTheDisplayedDistanceAndKeepsMissingLiveValuesBlank() {
        assertEquals(DistanceReadout(18420, "mi", true), distanceReadout(null, 18420))
        assertEquals(DistanceReadout(16, "km", true), distanceReadout("16 km", 0))
        assertEquals(
            DistanceReadout(0, "mi", false),
            distanceReadout(null, 0, missingLabelIsUnavailable = true),
        )
        assertFalse(distanceReadout("—", 12).available)
    }

    @Test
    fun odometerDrumsShowSixDigitsAndDimOnlyLeadingZeros() {
        assertEquals(listOf(0, 1, 8, 4, 2, 0), odometerDigits(18420))
        assertEquals(7, odometerPlaces(1_000_000))
        val digits = odometerDigits(18420)
        assertTrue(odometerDigitDimmed(digits, 0))
        assertFalse(odometerDigitDimmed(digits, 1))
        assertFalse(odometerDigitDimmed(odometerDigits(0), 5))
        assertTrue(odometerDigitDimmed(odometerDigits(0), 0))
    }

    @Test
    fun rangeFillGrowsFromTheCarTowardTheHorizon() {
        assertEquals(0f, rangeFillFraction(0), 0.001f)
        assertEquals(0.8f, rangeFillFraction(240), 0.001f)
        assertEquals(RangeBand.Good, rangeBand(0.8f))
        assertEquals(RangeBand.Low, rangeBand(0.1f))
        assertEquals(RangeBand.Empty, rangeBand(0f))

        val empty = rangeRoad(200f, 120f, 0f)
        val half = rangeRoad(200f, 120f, 0.5f)
        val full = rangeRoad(200f, 120f, 1f)

        assertEquals(empty.bottom, empty.fillFarY, 0.5f)
        assertTrue(half.fillFarY < empty.fillFarY)
        assertTrue(half.fillFarY > half.horizonY)
        assertEquals(full.horizonY, full.fillFarY, 0.5f)
        assertTrue(half.fillFarLeft in half.nearLeft..half.farLeft)
        assertTrue(half.fillFarRight in half.farRight..half.nearRight)
    }
}
