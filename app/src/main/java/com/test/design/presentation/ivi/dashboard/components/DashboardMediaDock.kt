package com.test.design.presentation.ivi.dashboard.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.presentation.ivi.dashboard.widgetControlsSharedElement
import com.test.design.presentation.ivi.media.MediaEvent
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.media.components.MediaArtInitials
import com.test.design.presentation.ivi.media.components.MediaTransportControlsBar
import com.test.design.presentation.ivi.media.nowPlayingSubtitle

/**
 * Full-width Tesla-style media bar across the map bottom.
 * Explicit compact type — CarTypography is oversized for overlays.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DashboardMediaDock(
    mediaState: MediaUiState,
    onMediaEvent: (MediaEvent) -> Unit,
    onExpand: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = widgetContainerTransform(
            widget = DashboardWidget.Media,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = modifier.fillMaxWidth(),
        ),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xE6111114),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        onClick = onExpand,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF3D5A80),
                                Color(0xFF1B2838),
                                Color(0xFF98C1D9),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                MediaArtInitials(
                    album = mediaState.currentTrack.album,
                    artwork = mediaState.artwork,
                    labelSize = 16.sp,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = mediaState.currentTrack.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (mediaState.isLive) {
                        mediaState.nowPlayingSubtitle()
                    } else {
                        "${mediaState.currentTrack.artist}  ·  ${mediaState.currentTrack.album}"
                    },
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearProgressIndicator(
                    progress = { mediaState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .height(2.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    trackColor = Color.White.copy(alpha = 0.18f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MediaTransportControlsBar(
                    isPlaying = mediaState.isPlaying,
                    onToggleQueue = { onMediaEvent(MediaEvent.ToggleQueue) },
                    onPrevious = { onMediaEvent(MediaEvent.PreviousTrack) },
                    onTogglePlayback = { onMediaEvent(MediaEvent.TogglePlayback) },
                    onNext = { onMediaEvent(MediaEvent.NextTrack) },
                    compact = true,
                    showQueue = false,
                    modifier = widgetControlsSharedElement(
                        widget = DashboardWidget.Media,
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                )
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Icon(
                    Icons.Default.ExpandLess,
                    contentDescription = "Expand media",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onExpand),
                )
            }
        }
    }
}
