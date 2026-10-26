package com.test.design.presentation.ivi.media.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.test.design.presentation.ivi.media.MediaSource
import com.test.design.presentation.ivi.media.RepeatMode
import com.test.design.presentation.ivi.media.Track
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.MediaAlbumShape
import com.test.design.theme.carTouchTarget

@Composable
fun AnimatedTrackInfo(
    track: Track,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    compact: Boolean = false,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    subtitleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    AnimatedContent(
        targetState = track,
        modifier = modifier,
        transitionSpec = {
            slideInVertically(animationSpec = spatialSpec) { it / 2 } + fadeIn(animationSpec = effectsSpec) togetherWith
                slideOutVertically(animationSpec = spatialSpec) { -it / 2 } + fadeOut(animationSpec = effectsSpec)
        },
        contentKey = { it.id },
        label = "track_info",
    ) { animatedTrack ->
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 8.dp)) {
            Text(
                text = animatedTrack.title,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.displaySmall,
                color = titleColor,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle ?: animatedTrack.artist,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.headlineSmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compact) {
                Text(
                    text = animatedTrack.album,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun MediaSourceChips(
    selected: MediaSource,
    onSelected: (MediaSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CarDesignTokens.TouchTargetSpacing),
    ) {
        items(MediaSource.entries, key = { it.name }) { source ->
            FilterChip(
                selected = selected == source,
                onClick = { onSelected(source) },
                modifier = Modifier
                    .carTouchTarget()
                    .height(CarDesignTokens.MinTouchTarget),
                label = {
                    Text(source.label, style = MaterialTheme.typography.labelLarge)
                },
            )
        }
    }
}

@Composable
fun PlaybackProgressSection(
    progress: Float,
    elapsedLabel: String,
    durationLabel: String,
    modifier: Modifier = Modifier,
    progressShape: Shape = MediaAlbumShape,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(progressShape),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(elapsedLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(durationLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RepeatModeLabel(mode: RepeatMode): String = when (mode) {
    RepeatMode.Off -> "Repeat off"
    RepeatMode.All -> "Repeat all"
    RepeatMode.One -> "Repeat one"
}
