package com.test.design.presentation.ivi.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackTest {

    private val track = Track("1", "Neon Horizon", "Aurora Drive", "Synthwave Nights", "0:05")
    private val next = Track("2", "Midnight Cruise", "Aurora Drive", "Synthwave Nights", "0:04")

    private fun playing() = MediaUiState(
        currentTrack = track,
        queue = listOf(track, next),
        isPlaying = true,
        progress = 0f,
        elapsedLabel = "0:00",
    )

    @Test
    fun tickAdvancesElapsedAndProgress() {
        val advanced = playing().advancePlayback()
        assertEquals("0:01", advanced.elapsedLabel)
        assertEquals(1f / 5f, advanced.progress, 0.001f)
        assertTrue(advanced.isPlaying)
        assertEquals(track.id, advanced.currentTrack.id)
    }

    @Test
    fun pausedPlaybackDoesNotAdvance() {
        val paused = playing().copy(isPlaying = false, elapsedLabel = "0:02", progress = 0.4f)
        assertEquals(paused, paused.advancePlayback())
    }

    @Test
    fun endOfTrackAdvancesToNext() {
        val atEnd = playing().copy(elapsedLabel = "0:04")
        val advanced = atEnd.advancePlayback()
        assertEquals(next.id, advanced.currentTrack.id)
        assertEquals("0:00", advanced.elapsedLabel)
        assertEquals(0f, advanced.progress)
        assertTrue(advanced.isPlaying)
    }

    @Test
    fun repeatOneRestartsCurrentTrack() {
        val atEnd = playing().copy(elapsedLabel = "0:04", repeatMode = RepeatMode.One)
        val advanced = atEnd.advancePlayback()
        assertEquals(track.id, advanced.currentTrack.id)
        assertEquals("0:00", advanced.elapsedLabel)
        assertTrue(advanced.isPlaying)
    }

    @Test
    fun lastTrackPausesAtEndWhenRepeatIsOff() {
        val last = playing().copy(currentTrack = next, queue = listOf(track, next), elapsedLabel = "0:03")
        val advanced = last.advancePlayback()
        assertEquals(next.id, advanced.currentTrack.id)
        assertEquals("0:04", advanced.elapsedLabel)
        assertEquals(1f, advanced.progress)
        assertFalse(advanced.isPlaying)
    }

    @Test
    fun repeatAllWrapsToFirstTrack() {
        val last = playing().copy(
            currentTrack = next,
            elapsedLabel = "0:03",
            repeatMode = RepeatMode.All,
        )
        val advanced = last.advancePlayback()
        assertEquals(track.id, advanced.currentTrack.id)
        assertTrue(advanced.isPlaying)
    }

    @Test
    fun shuffleSkipsCurrentTrack() {
        val atEnd = playing().copy(elapsedLabel = "0:04", isShuffleOn = true)
        val advanced = atEnd.advancePlayback(nextRandomIndex = { 0 })
        assertEquals(next.id, advanced.currentTrack.id)
    }

    @Test
    fun togglePlaybackFlipsPlayingFlag() {
        val paused = playing().reduce(MediaEvent.TogglePlayback)
        assertFalse(paused.isPlaying)
        assertEquals(track.id, paused.currentTrack.id)
        assertTrue(paused.reduce(MediaEvent.TogglePlayback).isPlaying)
    }

    @Test
    fun playingPositionAdvancesFromLastUpdate() {
        val position = extrapolatedPositionMs(
            positionMs = 1_000L,
            lastUpdateElapsedRealtimeMs = 5_000L,
            nowElapsedRealtimeMs = 6_500L,
            speed = 1f,
            isPlaying = true,
        )
        assertEquals(2_500L, position)
    }

    @Test
    fun pausedPositionStaysAtReportedPlayhead() {
        val position = extrapolatedPositionMs(
            positionMs = 1_000L,
            lastUpdateElapsedRealtimeMs = 5_000L,
            nowElapsedRealtimeMs = 9_000L,
            speed = 1f,
            isPlaying = false,
        )
        assertEquals(1_000L, position)
    }

    @Test
    fun manualNextMatchesQueueOrder() {
        val skipped = playing().reduce(MediaEvent.NextTrack)
        assertEquals(next.id, skipped.currentTrack.id)
        assertEquals("0:00", skipped.elapsedLabel)
        assertTrue(skipped.isPlaying)
    }
}
