package com.test.design.presentation.ivi.dashboard.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.test.design.presentation.ivi.vehicle.VehicleUiState

private val CardBg = Color(0xF01C1C1E)
private val AccentGreen = Color(0xFF34C759)
private val AccentRed = Color(0xFFE53935)
private val AccentYellow = Color(0xFFF5C542)

/**
 * Floating left rail over the map: stats / media / apps with clear gaps.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DrivingSidebar(
    vehicleState: VehicleUiState,
    mediaState: MediaUiState,
    onVehicleClick: () -> Unit,
    onMediaEvent: (MediaEvent) -> Unit,
    onExpandMedia: () -> Unit,
    onOpenApp: (DashboardWidget) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    speedMph: Int = 54,
    gear: String = "D",
    speedLimitMph: Int = 60,
    onOpenWidgetDashboard: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DrivingStatsCard(
            speedMph = speedMph,
            speedLimitMph = speedLimitMph,
            gear = gear,
            batteryPercent = vehicleState.batteryPercent,
            rangeLabel = vehicleState.rangeLabel,
            onClick = onVehicleClick,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.fillMaxWidth(),
        )

        SidebarMediaCard(
            mediaState = mediaState,
            onMediaEvent = onMediaEvent,
            onExpand = onExpandMedia,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        AppLauncherCard(
            onOpenApp = onOpenApp,
            onOpenWidgetDashboard = onOpenWidgetDashboard,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.DrivingStatsCard(
    speedMph: Int,
    speedLimitMph: Int,
    gear: String,
    batteryPercent: Int,
    rangeLabel: String,
    onClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = widgetContainerTransform(
            widget = DashboardWidget.Vehicle,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = modifier,
        ),
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$speedMph MPH",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 52.sp,
                )
                SpeedLimitBadge(limit = speedLimitMph)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LinearProgressIndicator(
                    progress = { batteryPercent / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = AccentGreen,
                    trackColor = Color.White.copy(alpha = 0.12f),
                )
                Text(
                    text = rangeLabel,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("P", "R", "N", "D").forEach { g ->
                    val selected = g == gear
                    Text(
                        text = g,
                        fontSize = 24.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            selected && g == "D" -> AccentRed
                            selected -> AccentGreen
                            else -> Color.White.copy(alpha = 0.3f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedLimitBadge(limit: Int) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .padding(4.dp)
            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$limit",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
            Text(
                text = "MAX",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.SidebarMediaCard(
    mediaState: MediaUiState,
    onMediaEvent: (MediaEvent) -> Unit,
    onExpand: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onExpand,
        modifier = widgetContainerTransform(
            widget = DashboardWidget.Media,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = modifier,
        ),
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        shadowElevation = 10.dp,
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
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = mediaState.nowPlayingSubtitle(),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = AccentRed,
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
                    color = AccentYellow,
                    trackColor = Color.White.copy(alpha = 0.15f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = mediaState.elapsedLabel,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                    )
                    Text(
                        text = mediaState.currentTrack.durationLabel,
                        color = Color.White.copy(alpha = 0.5f),
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
                modifier = widgetControlsSharedElement(
                    widget = DashboardWidget.Media,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.fillMaxWidth(),
                ),
            )
        }
    }
}

private data class LauncherApp(
    val label: String,
    val icon: ImageVector?,
    val monogram: String? = null,
    val color: Color,
    val widget: DashboardWidget? = null,
)

@Composable
private fun AppLauncherCard(
    onOpenApp: (DashboardWidget) -> Unit,
    onOpenWidgetDashboard: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val apps = listOf(
        LauncherApp("Search", null, "G", Color(0xFF4285F4), DashboardWidget.Navigation),
        LauncherApp("Video", Icons.Default.SmartDisplay, null, Color(0xFFFF0000), DashboardWidget.Media),
        LauncherApp("Store", Icons.Default.Store, null, Color(0xFF34A853), DashboardWidget.MaterialComponents),
        LauncherApp("Camera", Icons.Default.CameraAlt, null, Color(0xFFE1306C), DashboardWidget.Vehicle),
        LauncherApp("Chat", Icons.Default.Chat, null, Color(0xFF25D366), DashboardWidget.Settings),
        LauncherApp("Music", Icons.Default.MusicNote, null, Color(0xFF1DB954), DashboardWidget.Media),
        LauncherApp("Watch", null, "N", Color(0xFFE50914), DashboardWidget.CustomizedMaterial),
        LauncherApp(
            "Assistant",
            Icons.Default.AutoAwesome,
            null,
            Color(0xFF8AB4F8),
            DashboardWidget.VirtualAssistant,
        ),
    )
    val columns = 4
    val gap = 14.dp
    val pad = 18.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        shadowElevation = 10.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(pad),
        ) {
            val tile = ((maxWidth - gap * (columns - 1)) / columns).coerceAtLeast(64.dp)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                apps.chunked(columns).forEach { rowApps ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        rowApps.forEach { app ->
                            AppTile(
                                app = app,
                                onClick = {
                                    when {
                                        app.widget != null -> onOpenApp(app.widget)
                                        onOpenWidgetDashboard != null -> onOpenWidgetDashboard()
                                    }
                                },
                                modifier = Modifier.size(tile),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: LauncherApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(app.color.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .clip(RoundedCornerShape(14.dp))
                .background(app.color),
            contentAlignment = Alignment.Center,
        ) {
            if (app.monogram != null) {
                Text(
                    text = app.monogram,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else if (app.icon != null) {
                Icon(
                    imageVector = app.icon,
                    contentDescription = app.label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
