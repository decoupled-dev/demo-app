package com.test.design.presentation.ivi.media

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.test.design.data.media.MediaPlaybackRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Now-playing shared with the car launcher media widget.
 * [MediaPlaybackRepository] reads CarMediaManager's active playback source.
 */
class MediaViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val playback = MediaPlaybackRepository.get(application)
    val state: StateFlow<MediaUiState> = playback.state

    fun onEvent(event: MediaEvent) = playback.onEvent(event)

    /** Opens Spotify, Media App, Media Center, or whichever source is playing. */
    fun openActiveSource(context: Context) = playback.openActiveSource(context)
}
