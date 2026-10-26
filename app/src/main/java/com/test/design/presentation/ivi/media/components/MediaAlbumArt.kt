package com.test.design.presentation.ivi.media.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.theme.CarDesignTokens

@Composable
fun MediaAlbumArt(
    album: String,
    albumShape: Shape,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    compactSize: Dp = 56.dp,
    artwork: Bitmap? = null,
) {
    Box(
        modifier = modifier
            .then(
                if (compact) {
                    Modifier.size(compactSize)
                } else {
                    Modifier
                        .fillMaxHeight(0.9f)
                },
            )
            .clip(albumShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = album,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            return@Box
        }
        Text(
            text = if (compact) album.take(2).uppercase() else album,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = if (compact) Modifier else Modifier.padding(CarDesignTokens.TouchTargetSpacing),
            maxLines = if (compact) 1 else 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Album thumb used by the driving-home and glanceable media cards.
 * Shows the active session's artwork when the car media source provides it.
 */
@Composable
fun MediaArtInitials(
    album: String,
    artwork: Bitmap?,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 18.sp,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = album,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (album.isNotBlank()) {
            Text(
                text = album.take(2).uppercase(),
                color = Color.White,
                fontSize = labelSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
