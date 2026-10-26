package com.test.design.presentation.ivi.glanceables

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.presentation.ivi.media.MediaEvent
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.media.MediaViewModel
import com.test.design.presentation.ivi.media.components.MediaArtInitials
import com.test.design.presentation.ivi.media.components.MediaTransportControlsBar
import com.test.design.presentation.ivi.media.nowPlayingSubtitle

/** Scalable UI `media_glance` TaskPanel. */
class MediaGlanceActivity : GlanceableActivity() {

    private val mediaViewModel: MediaViewModel by viewModels()

    @Composable
    override fun GlanceContent() {
        val mediaState by mediaViewModel.state.collectAsStateWithLifecycle()
        GlanceRoot {
            MediaGlance(
                mediaState = mediaState,
                onMediaEvent = mediaViewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun MediaGlance(
    mediaState: MediaUiState,
    onMediaEvent: (MediaEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = glancePalette()
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.card,
        border = colors.cardBorder(),
        shadowElevation = if (colors.light) 2.dp else 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF5B8DEF), Color(0xFF1A1A2E), Color(0xFFE53935)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    MediaArtInitials(
                        album = mediaState.currentTrack.album,
                        artwork = mediaState.artwork,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaState.currentTrack.title,
                        color = colors.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = mediaState.nowPlayingSubtitle(),
                        color = colors.muted,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { mediaState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (colors.light) Color(0xFF1565C0) else Color(0xFFF5C542),
                    trackColor = colors.track,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = mediaState.elapsedLabel,
                        color = colors.muted,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = mediaState.currentTrack.durationLabel,
                        color = colors.muted,
                        fontSize = 14.sp,
                    )
                }
            }

            MediaTransportControlsBar(
                isPlaying = mediaState.isPlaying,
                onToggleQueue = { onMediaEvent(MediaEvent.ToggleQueue) },
                onPrevious = { onMediaEvent(MediaEvent.PreviousTrack) },
                onTogglePlayback = { onMediaEvent(MediaEvent.TogglePlayback) },
                onNext = { onMediaEvent(MediaEvent.NextTrack) },
                showQueue = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
