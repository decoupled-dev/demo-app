package com.test.design.presentation.ivi.media

/**
 * Pure now-playing transitions shared by every media surface
 * (driving-home card, media widget, glanceables).
 */
fun MediaUiState.reduce(event: MediaEvent): MediaUiState = when (event) {
    MediaEvent.TogglePlayback -> copy(isPlaying = !isPlaying)
    MediaEvent.ToggleQueue -> copy(isQueueVisible = !isQueueVisible)
    MediaEvent.ToggleShuffle -> copy(isShuffleOn = !isShuffleOn)
    MediaEvent.CycleRepeat -> {
        val next = when (repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        copy(repeatMode = next)
    }
    is MediaEvent.SelectSource -> copy(source = event.source)
    MediaEvent.NextTrack -> skipBy(1)
    MediaEvent.PreviousTrack -> skipBy(-1)
    is MediaEvent.SelectTrack -> copy(
        currentTrack = event.track,
        isPlaying = true,
        progress = 0f,
        elapsedLabel = "0:00",
        isQueueVisible = false,
    )
}

/**
 * Advances the playhead by [deltaSeconds] while playing.
 * At the end of a track: repeat-one restarts, shuffle or the queue picks the
 * next track, repeat-all wraps, otherwise playback pauses at the end.
 */
fun MediaUiState.advancePlayback(
    deltaSeconds: Int = 1,
    nextRandomIndex: (Int) -> Int = { kotlin.random.Random.nextInt(it) },
): MediaUiState {
    if (!isPlaying || deltaSeconds <= 0) return this
    val duration = parseClockSeconds(currentTrack.durationLabel)
    if (duration <= 0) return this
    val elapsed = parseClockSeconds(elapsedLabel) + deltaSeconds
    if (elapsed < duration) {
        return copy(
            progress = elapsed.toFloat() / duration.toFloat(),
            elapsedLabel = formatClockSeconds(elapsed),
        )
    }
    return onTrackEnded(nextRandomIndex)
}

internal fun parseClockSeconds(label: String): Int {
    val parts = label.split(':')
    if (parts.size != 2) return 0
    val minutes = parts[0].toIntOrNull() ?: return 0
    val seconds = parts[1].toIntOrNull() ?: return 0
    if (minutes < 0 || seconds < 0) return 0
    return minutes * 60 + seconds
}

internal fun formatClockSeconds(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun formatClockMs(positionMs: Long): String =
    formatClockSeconds((positionMs.coerceAtLeast(0L) / 1000L).toInt())

/**
 * Playhead used by the car launcher media widget: frozen while paused, and
 * advanced from the session's last update time while playing.
 */
fun extrapolatedPositionMs(
    positionMs: Long,
    lastUpdateElapsedRealtimeMs: Long,
    nowElapsedRealtimeMs: Long,
    speed: Float,
    isPlaying: Boolean,
): Long {
    val base = positionMs.coerceAtLeast(0L)
    if (!isPlaying || speed == 0f) return base
    val delta = (nowElapsedRealtimeMs - lastUpdateElapsedRealtimeMs).coerceAtLeast(0L)
    return base + (delta * speed).toLong()
}

private fun MediaUiState.skipBy(delta: Int): MediaUiState {
    val index = queue.indexOfFirst { it.id == currentTrack.id }
    val nextIndex = (index + delta).coerceIn(0, queue.lastIndex.coerceAtLeast(0))
    val track = queue.getOrNull(nextIndex) ?: currentTrack
    return copy(currentTrack = track, isPlaying = true, progress = 0f, elapsedLabel = "0:00")
}

private fun MediaUiState.onTrackEnded(
    nextRandomIndex: (Int) -> Int,
): MediaUiState {
    if (repeatMode == RepeatMode.One) {
        return copy(progress = 0f, elapsedLabel = "0:00", isPlaying = true)
    }
    val index = queue.indexOfFirst { it.id == currentTrack.id }.let { if (it < 0) 0 else it }
    val nextIndex = when {
        isShuffleOn && queue.size > 1 -> {
            var pick = nextRandomIndex(queue.size - 1).coerceIn(0, queue.size - 2)
            if (pick >= index) pick += 1
            pick
        }
        index < queue.lastIndex -> index + 1
        repeatMode == RepeatMode.All && queue.isNotEmpty() -> 0
        else -> -1
    }
    if (nextIndex < 0) {
        val duration = parseClockSeconds(currentTrack.durationLabel)
        return copy(
            progress = 1f,
            elapsedLabel = formatClockSeconds(duration),
            isPlaying = false,
        )
    }
    return copy(
        currentTrack = queue[nextIndex],
        isPlaying = true,
        progress = 0f,
        elapsedLabel = "0:00",
    )
}
