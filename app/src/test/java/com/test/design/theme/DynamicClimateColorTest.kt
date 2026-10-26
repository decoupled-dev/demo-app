package com.test.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicClimateColorTest {

    @Test
    fun lightClimateStaysOnLightSheetsWithDarkInk() {
        val light = lightColorScheme(
            background = Color(0xFFF0F2F5),
            surface = Color(0xFFF4F5F7),
        )
        val cool = climateColorScheme(0f, light)
        val warm = climateColorScheme(1f, light)

        assertTrue(cool.surface.luminance() > 0.85f)
        assertTrue(warm.surface.luminance() > 0.85f)
        assertTrue(cool.primaryContainer.luminance() > 0.75f)
        assertTrue(warm.primaryContainer.luminance() > 0.75f)
        assertTrue(cool.onPrimaryContainer.luminance() < 0.2f)
        assertTrue(warm.onPrimary.luminance() > 0.9f)
        assertTrue(cool.primary.luminance() < 0.25f)
    }

    @Test
    fun darkClimateStaysOnDarkSheets() {
        val dark = darkColorScheme(background = Color(0xFF1C1E22))
        val scheme = climateColorScheme(0.5f, dark)
        assertTrue(scheme.surface.luminance() < 0.2f)
        assertTrue(scheme.onPrimary.luminance() > 0.9f)
    }

    @Test
    fun contentAccentIsInkOnLightAndGlowOnDark() {
        val coolLight = climateContentAccent(0f, lightCanvas = true)
        val coolDark = climateContentAccent(0f, lightCanvas = false)
        assertTrue(coolLight.luminance() < coolDark.luminance())
        assertTrue(climateDialFill(0f, lightCanvas = true).luminance() > 0.8f)
        assertTrue(climateDialFill(0f, lightCanvas = false).luminance() < 0.2f)
    }
}
