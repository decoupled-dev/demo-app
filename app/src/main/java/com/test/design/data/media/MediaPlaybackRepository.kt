package com.test.design.data.media

import android.content.Context
import android.util.Log
import com.test.design.presentation.ivi.media.MediaEvent
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.media.advancePlayback
import com.test.design.presentation.ivi.media.reduce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Now-playing for every media surface.
 *
 * When [CarActiveMediaClient] is bound, state is the car's active playback source
 * (the same source the launcher media widget shows). Otherwise the simulated player runs.
 */
class MediaPlaybackRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val carMedia: CarActiveMediaClient = CarActiveMediaClient(context),
    private val tickMillis: Long = SIMULATED_TICK_MS,
    private val liveRefreshMillis: Long = LIVE_POSITION_REFRESH_MS,
) {
    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    init {
        carMedia.start()
        carMedia.nowPlaying
            .onEach { snapshot -> applyCarSnapshot(snapshot) }
            .launchIn(scope)
        scope.launch {
            while (isActive) {
                if (carMedia.isLive()) {
                    delay(liveRefreshMillis)
                    carMedia.refreshPosition()
                } else {
                    delay(tickMillis)
                    _state.update { current ->
                        if (current.isLive || !current.isPlaying) current else current.advancePlayback()
                    }
                }
            }
        }
    }

    /**
     * Home-widget tap: open the playing source's own screen.
     * Media Center shows the same source when that app has no media activity.
     */
    fun openActiveSource(context: Context) {
        val source = _state.value.sourceComponent
        if (launchSourceTemplate(context, source)) {
            Log.i(TAG, "Opened source screen for $source")
            return
        }
        carMedia.showPlaybackSourceInBrowse()
        if (launchMediaCenter(context, source)) {
            Log.i(TAG, "Opened Media Center for ${source.ifBlank { "the default source" }}")
        } else {
            Log.w(TAG, "No source screen for $source")
        }
    }

    fun onEvent(event: MediaEvent) {
        if (carMedia.isLive()) {
            when (event) {
                MediaEvent.TogglePlayback -> carMedia.togglePlayback()
                MediaEvent.NextTrack -> carMedia.skipNext()
                MediaEvent.PreviousTrack -> carMedia.skipPrevious()
                is MediaEvent.SelectTrack -> carMedia.skipTo(event.track.id)
                MediaEvent.ToggleQueue -> _state.update { it.copy(isQueueVisible = !it.isQueueVisible) }
                MediaEvent.ToggleShuffle,
                MediaEvent.CycleRepeat,
                is MediaEvent.SelectSource,
                -> Unit
            }
        } else {
            _state.update { it.reduce(event) }
        }
    }

    private fun applyCarSnapshot(snapshot: MediaUiState?) {
        if (snapshot == null) {
            if (_state.value.isLive) {
                _state.value = MediaUiState()
            }
            return
        }
        _state.update { current ->
            snapshot.copy(isQueueVisible = current.isQueueVisible)
        }
    }

    companion object {
        private const val TAG = "MediaPlayback"
        const val SIMULATED_TICK_MS = 1_000L
        const val LIVE_POSITION_REFRESH_MS = 500L

        @Volatile
        private var instance: MediaPlaybackRepository? = null

        fun get(context: Context): MediaPlaybackRepository {
            return instance ?: synchronized(this) {
                instance ?: MediaPlaybackRepository(
                    context.applicationContext,
                    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                ).also { instance = it }
            }
        }
    }
}
