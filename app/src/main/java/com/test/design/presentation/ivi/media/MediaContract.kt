package com.test.design.presentation.ivi.media

import android.graphics.Bitmap

enum class MediaSource(val label: String) {
    Bluetooth("Bluetooth"),
    Radio("Radio"),
    Usb("USB"),
}

enum class RepeatMode {
    Off,
    All,
    One,
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationLabel: String,
)

data class MediaUiState(
    val currentTrack: Track = Track(
        id = "1",
        title = "Neon Horizon",
        artist = "Aurora Drive",
        album = "Synthwave Nights",
        durationLabel = "4:12",
    ),
    val queue: List<Track> = listOf(
        Track("1", "Neon Horizon", "Aurora Drive", "Synthwave Nights", "4:12"),
        Track("2", "Midnight Cruise", "Aurora Drive", "Synthwave Nights", "3:48"),
        Track("3", "Chrome Sunset", "Velvet Circuit", "Retro Future", "5:01"),
        Track("4", "Electric Drift", "Velvet Circuit", "Retro Future", "3:22"),
        Track("5", "Starlight Lane", "Nova Pulse", "Highway Dreams", "4:35"),
    ),
    val isPlaying: Boolean = true,
    val progress: Float = 0.42f,
    val elapsedLabel: String = "1:45",
    val isQueueVisible: Boolean = false,
    val isShuffleOn: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val source: MediaSource = MediaSource.Bluetooth,
    /** True when now-playing comes from CarMediaManager's active playback source. */
    val isLive: Boolean = false,
    /** Application label of the active media source, matching the car launcher widget. */
    val sourceLabel: String = "",
    /**
     * Active playback source from CarMediaManager.
     * A flattened component (`package/class`) when the platform names one, otherwise the
     * playing session's package name.
     */
    val sourceComponent: String = "",
    val artwork: Bitmap? = null,
)

/** Artist line, with the active source name when the session is live. */
fun MediaUiState.nowPlayingSubtitle(): String = when {
    isLive && sourceLabel.isNotBlank() && currentTrack.artist.isNotBlank() ->
        "${currentTrack.artist} · $sourceLabel"
    isLive && sourceLabel.isNotBlank() -> sourceLabel
    else -> currentTrack.artist
}

sealed interface MediaEvent {
    data object TogglePlayback : MediaEvent
    data object ToggleQueue : MediaEvent
    data object NextTrack : MediaEvent
    data object PreviousTrack : MediaEvent
    data class SelectTrack(val track: Track) : MediaEvent
    data object ToggleShuffle : MediaEvent
    data object CycleRepeat : MediaEvent
    data class SelectSource(val source: MediaSource) : MediaEvent
}
