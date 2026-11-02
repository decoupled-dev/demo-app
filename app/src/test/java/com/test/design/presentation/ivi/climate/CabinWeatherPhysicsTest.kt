package com.test.design.presentation.ivi.climate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinWeatherPhysicsTest {

    @Test
    fun syncCollapsesTheMixingSeam() {
        val open = CabinWeatherPhysics.frontStrength(0.1f, 0.9f, synced = false, auto = false)
        val synced = CabinWeatherPhysics.frontStrength(0.1f, 0.9f, synced = true, auto = false)
        assertTrue(open > 0.8f)
        assertTrue(synced < 0.2f)
    }

    @Test
    fun mixIsWiderWhenFrontIsWeak() {
        val wide = CabinWeatherPhysics.mixHalfWidth(0f)
        val tight = CabinWeatherPhysics.mixHalfWidth(1f)
        assertTrue(wide > tight)
    }

    @Test
    fun defrostClearsMostGlassHaze() {
        val fogged = CabinWeatherPhysics.frostAmount(0.8f, 0.2f, defrostOn = false, feetDefrost = false, powerOn = true)
        val clearing = CabinWeatherPhysics.frostAmount(0.8f, 0.2f, defrostOn = true, feetDefrost = false, powerOn = true)
        assertTrue(fogged > clearing * 3f)
    }

    @Test
    fun recircSealsOutsidePressure() {
        val fresh = CabinWeatherPhysics.outsidePressure(acOn = true, recirc = false, powerOn = true)
        val recirc = CabinWeatherPhysics.outsidePressure(acOn = true, recirc = true, powerOn = true)
        assertTrue(recirc < fresh * 0.4f)
    }

    @Test
    fun faceAirSitsHigherThanFeet() {
        val face = CabinWeatherPhysics.atmosphereY(AirflowMode.Face, auto = false)
        val feet = CabinWeatherPhysics.atmosphereY(AirflowMode.Feet, auto = false)
        assertTrue(face < feet)
        assertEquals(0.54f, CabinWeatherPhysics.atmosphereY(AirflowMode.Face, auto = true), 0.001f)
    }

    @Test
    fun breathStaysSubtle() {
        val amp = CabinWeatherPhysics.breathAmplitude(5, 5, powerOn = true)
        assertTrue(amp <= 0.3f)
        assertTrue(CabinWeatherPhysics.breathHertz(5, 5, powerOn = true) < 0.4f)
    }
}
