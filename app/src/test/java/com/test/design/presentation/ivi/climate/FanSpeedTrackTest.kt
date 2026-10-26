package com.test.design.presentation.ivi.climate

import org.junit.Assert.assertEquals
import org.junit.Test

class FanSpeedTrackTest {

    @Test
    fun trackPositionSnapsToFanSteps() {
        assertEquals(0, fanSpeedFromTrackPosition(0f, 200f, 5))
        assertEquals(0, fanSpeedFromTrackPosition(10f, 200f, 5))
        assertEquals(3, fanSpeedFromTrackPosition(120f, 200f, 5))
        assertEquals(5, fanSpeedFromTrackPosition(200f, 200f, 5))
        assertEquals(0, fanSpeedFromTrackPosition(40f, 0f, 5))
        assertEquals(0, fanSpeedFromTrackPosition(40f, 200f, 0))
    }
}
