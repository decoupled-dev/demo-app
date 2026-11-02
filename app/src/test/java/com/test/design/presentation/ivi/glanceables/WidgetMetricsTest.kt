package com.test.design.presentation.ivi.glanceables

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMetricsTest {

    @Test
    fun originalLauncherSlotKeepsDesignScale() {
        val m = widgetMetricsFor(464.dp, 284.dp)
        assertFalse(m.tall)
        assertEquals(1f, m.scale, 0.001f)
    }

    @Test
    fun fullHeightColumnUsesTallLayout() {
        // Live Scalable UI column on the 1920x720 home: 462x596.
        val m = widgetMetricsFor(462.dp, 596.dp)
        assertTrue(m.tall)
        assertEquals(0.99f, m.scale, 0.01f)
    }

    @Test
    fun largerPanelScalesUpUntilWidthLimits() {
        val m = widgetMetricsFor(696.dp, 900.dp)
        assertTrue(m.tall)
        assertEquals(1.5f, m.scale, 0.01f)
    }

    @Test
    fun shortWidePanelIsLimitedByHeight() {
        val m = widgetMetricsFor(900.dp, 213.dp)
        assertFalse(m.tall)
        assertEquals(0.75f, m.scale, 0.01f)
    }

    @Test
    fun scaleIsClamped() {
        assertEquals(0.7f, widgetMetricsFor(100.dp, 100.dp).scale, 0.001f)
        assertEquals(1.8f, widgetMetricsFor(4000.dp, 4000.dp).scale, 0.001f)
    }

    @Test
    fun unmeasuredPanelFallsBackToDefaults() {
        val m = widgetMetricsFor(0.dp, 0.dp)
        assertFalse(m.tall)
        assertEquals(1f, m.scale, 0.001f)
    }
}
