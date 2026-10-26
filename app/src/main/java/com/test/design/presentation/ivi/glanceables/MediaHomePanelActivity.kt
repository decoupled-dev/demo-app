package com.test.design.presentation.ivi.glanceables

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.presentation.ivi.media.MediaViewModel

/**
 * Media card hosted by Scalable UI on the system home screen.
 * A tap on the card opens the playing source (Spotify, Media App, Media Center).
 * Transport buttons keep controlling that session.
 */
class MediaHomePanelActivity : GlanceableActivity() {

    private val mediaViewModel: MediaViewModel by viewModels()

    @Composable
    override fun GlanceContent() {
        val mediaState by mediaViewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        GlanceRoot {
            MediaQuarter(
                state = mediaState,
                onEvent = mediaViewModel::onEvent,
                onOpenMain = { mediaViewModel.openActiveSource(context) },
            )
        }
    }
}
