package com.test.design.presentation.ivi.vehicle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TireChassisLayoutTest {

    @Test
    fun chassisKeepsTiresAndLabelsApartFrom220To320Dpi() {
        val wide = metricsFor(dpi = 220)
        val dense = metricsFor(dpi = 320)

        assertSeparated(wide.second, wide.first.first, wide.first.second)
        assertSeparated(dense.second, dense.first.first, dense.first.second)
        assertTrue(dense.second.frontLeftTire.width < wide.second.frontLeftTire.width)
        assertTrue(dense.second.valueFontSp <= wide.second.valueFontSp)
    }

    @Test
    fun chassisKeepsTiresAndLabelsApartOnANarrowPanel() {
        val metrics = chassisMapMetrics(widthDp = 220f, heightDp = 320f)
        assertSeparated(metrics, width = 220f, height = 320f)
    }

    private fun metricsFor(dpi: Int): Pair<Pair<Float, Float>, ChassisMapMetrics> {
        val size = chassisSizeDp(dpi)
        return size to chassisMapMetrics(size.first, size.second)
    }
}

private fun assertSeparated(metrics: ChassisMapMetrics, width: Float, height: Float) {
    val slots = listOf(metrics.body) + metrics.tires + metrics.labels
    slots.forEach { rect ->
        assertTrue(rect.left >= -0.5f)
        assertTrue(rect.top >= -0.5f)
        assertTrue(rect.right <= width + 0.5f)
        assertTrue(rect.bottom <= height + 0.5f)
    }
    metrics.labels.forEach { label ->
        (metrics.tires + metrics.body).forEach { obstacle ->
            assertFalse(label.intersects(obstacle))
        }
    }
    metrics.tires.forEach { tire ->
        assertFalse(tire.intersects(metrics.body))
    }
    assertFalse(metrics.frontLeftTire.intersects(metrics.rearLeftTire))
    assertFalse(metrics.frontRightTire.intersects(metrics.rearRightTire))
    assertFalse(metrics.frontLeftLabel.intersects(metrics.rearLeftLabel))
    assertFalse(metrics.frontRightLabel.intersects(metrics.rearRightLabel))
    assertTrue(metrics.frontLeftLabel.right < metrics.frontLeftTire.left)
    assertTrue(metrics.frontLeftTire.right < metrics.body.left)
    assertTrue(metrics.body.right < metrics.frontRightTire.left)
    assertTrue(metrics.frontRightTire.right < metrics.frontRightLabel.left)
}

/** Sport-mode center column on a 2560×1600 panel, after chrome around the chassis map. */
private fun chassisSizeDp(dpi: Int): Pair<Float, Float> {
    val density = dpi / 160f
    val screenWidth = 2560f / density
    val screenHeight = 1600f / density
    val contentWidth = screenWidth - 32f * 2f
    val contentHeight = screenHeight - 32f * 2f
    val centerWidth = (contentWidth - 32f * 2f) * 0.32f - 24f * 2f
    val chassisHeight = contentHeight - 72f - 32f - 132f - 40f - 24f
    return centerWidth to chassisHeight
}
