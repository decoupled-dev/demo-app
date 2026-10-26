package com.test.design.presentation.ivi.adaptivespace

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.ivi.common.SimulatedBadge
import com.test.design.presentation.ivi.common.WidgetScreenHeader
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetContainerTransform
import com.test.design.presentation.ivi.media.MediaEvent
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.media.components.MediaTransportControlsBar
import com.test.design.presentation.ivi.navigation.components.DefaultMapCenter
import com.test.design.presentation.ivi.navigation.components.OpenMapBackground
import com.test.design.presentation.ivi.navigation.components.mapChromeLayer
import com.test.design.theme.CarDesignTokens

/**
 * Adaptive Space dashboard — in-app showcase of Android 17 Scalable UI:
 * Map-Under-Apps, dynamic overlay panels, and zero-stutter split resize.
 *
 * On-device RRO panels live in `:scalable-ui-rro`; this screen mirrors the
 * same windowing story inside the Design app for demos without SystemUI.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AdaptiveSpaceScreen(
    mediaState: MediaUiState,
    onMediaEvent: (MediaEvent) -> Unit,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    var mediaOpen by remember { mutableStateOf(false) }
    var parkingOpen by remember { mutableStateOf(false) }
    var splitFraction by remember { mutableFloatStateOf(0f) }
    var choreographyToken by remember { mutableIntStateOf(0) }
    var isPlayingDemo by remember { mutableStateOf(false) }
    var activeSceneLabel by remember { mutableStateOf<String?>(null) }
    val splitActive = splitFraction > 0.02f

    LaunchedEffect(choreographyToken) {
        if (choreographyToken == 0) return@LaunchedEffect
        isPlayingDemo = true
        try {
            playAdaptiveSpaceChoreography(
                onScene = { scene ->
                    val panel = scene.toPanelState()
                    mediaOpen = panel.mediaOpen
                    parkingOpen = panel.parkingOpen
                    splitFraction = panel.splitFraction
                    activeSceneLabel = scene.name
                },
            )
        } finally {
            isPlayingDemo = false
            activeSceneLabel = null
        }
    }

    fun resetPanels() {
        mediaOpen = false
        parkingOpen = false
        splitFraction = 0f
        activeSceneLabel = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                widgetContainerTransform(
                    widget = DashboardWidget.AdaptiveSpace,
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            ),
    ) {
        OpenMapBackground(
            center = DefaultMapCenter,
            zoom = 15.0,
            showRoute = true,
            modifier = Modifier.fillMaxSize(),
        )

        Box(modifier = Modifier.fillMaxSize().mapChromeLayer()) {
        val scrimAlpha by animateFloatAsState(
            targetValue = when {
                mediaOpen || parkingOpen -> 0.28f
                splitActive -> 0.18f
                else -> 0f
            },
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            label = "adaptive_scrim",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CarDesignTokens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WidgetScreenHeader(
                widget = DashboardWidget.AdaptiveSpace,
                onBack = onBack,
                animatedVisibilityScope = animatedVisibilityScope,
                trailingContent = { SimulatedBadge() },
            )

            AdaptiveSpaceControlRail(
                mediaOpen = mediaOpen,
                parkingOpen = parkingOpen,
                splitFraction = splitFraction,
                isPlayingDemo = isPlayingDemo,
                activeSceneLabel = activeSceneLabel,
                onToggleMedia = {
                    if (isPlayingDemo) return@AdaptiveSpaceControlRail
                    mediaOpen = !mediaOpen
                    if (mediaOpen) parkingOpen = false
                },
                onToggleParking = {
                    if (isPlayingDemo) return@AdaptiveSpaceControlRail
                    parkingOpen = !parkingOpen
                    if (parkingOpen) mediaOpen = false
                },
                onSplitChange = {
                    if (!isPlayingDemo) splitFraction = it
                },
                onPlayDemo = { choreographyToken += 1 },
                onReset = {
                    choreographyToken = 0
                    isPlayingDemo = false
                    resetPanels()
                },
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val splitWidth = maxWidth * (0.40f + splitFraction * 0.30f)
                val animatedSplit by animateDpAsState(
                    targetValue = if (splitActive) splitWidth else 0.dp,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "split_width",
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    if (splitActive) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(animatedSplit)
                                .fillMaxHeight()
                                .padding(end = 12.dp)
                                .pointerInput(isPlayingDemo) {
                                    if (isPlayingDemo) return@pointerInput
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val delta = dragAmount.x / size.width.coerceAtLeast(1)
                                        splitFraction = (splitFraction + delta).coerceIn(0f, 1f)
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xE6141C26),
                            shadowElevation = 10.dp,
                        ) {
                            SplitAppDemoPanel(
                                fraction = splitFraction,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = mediaOpen,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        enter = slideInHorizontally(
                            animationSpec = tween(360, easing = FastOutSlowInEasing),
                            initialOffsetX = { it },
                        ) + fadeIn(tween(280)),
                        exit = slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { it },
                        ) + fadeOut(tween(220)),
                    ) {
                        MediaOverlayDemo(
                            mediaState = mediaState,
                            onMediaEvent = onMediaEvent,
                            modifier = Modifier
                                .width(420.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = parkingOpen,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        enter = slideInHorizontally(
                            animationSpec = tween(360, easing = FastOutSlowInEasing),
                            initialOffsetX = { it },
                        ) + fadeIn(tween(280)),
                        exit = slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { it },
                        ) + fadeOut(tween(220)),
                    ) {
                        ParkingAssistantPanel(
                            modifier = Modifier
                                .width(420.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AdaptiveSpaceControlRail(
    mediaOpen: Boolean,
    parkingOpen: Boolean,
    splitFraction: Float,
    isPlayingDemo: Boolean,
    activeSceneLabel: String?,
    onToggleMedia: () -> Unit,
    onToggleParking: () -> Unit,
    onSplitChange: (Float) -> Unit,
    onPlayDemo: () -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xCC101820),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF7EB6FF))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Adaptive Space",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = if (isPlayingDemo && activeSceneLabel != null) {
                            "Playing demo · $activeSceneLabel"
                        } else {
                            "Map-Under-Apps · overlays · zero-stutter resize"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                TextButton(
                    onClick = onPlayDemo,
                    enabled = !isPlayingDemo,
                ) {
                    Icon(
                        imageVector = if (isPlayingDemo) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF9EC5FF),
                        modifier = Modifier.height(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlayingDemo) "Playing…" else "Play demo",
                        color = Color(0xFF9EC5FF),
                    )
                }
                TextButton(onClick = onReset) {
                    Text("Reset", color = Color(0xFF9EC5FF))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = mediaOpen,
                    onClick = onToggleMedia,
                    enabled = !isPlayingDemo,
                    label = { Text("Media overlay") },
                    leadingIcon = {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.height(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        labelColor = Color.White,
                        selectedContainerColor = Color(0xFF2A5A9E),
                        selectedLabelColor = Color.White,
                    ),
                )
                FilterChip(
                    selected = parkingOpen,
                    onClick = onToggleParking,
                    enabled = !isPlayingDemo,
                    label = { Text("Parking") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalParking, contentDescription = null, modifier = Modifier.height(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        labelColor = Color.White,
                        selectedContainerColor = Color(0xFF2A5A9E),
                        selectedLabelColor = Color.White,
                    ),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Split resize  ${(40 + (splitFraction * 30).toInt())}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Slider(
                        value = splitFraction,
                        onValueChange = onSplitChange,
                        enabled = !isPlayingDemo,
                        valueRange = 0f..1f,
                    )
                    Text(
                        text = "WM scales via onConfigurationChanged — activity stays alive",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaOverlayDemo(
    mediaState: MediaUiState,
    onMediaEvent: (MediaEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE6121820),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
                Text(
                    text = mediaState.currentTrack.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = mediaState.currentTrack.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3D5A80), Color(0xFF1B2838)),
                        ),
                    ),
            )
            MediaTransportControlsBar(
                isPlaying = mediaState.isPlaying,
                onToggleQueue = { },
                onPrevious = { onMediaEvent(MediaEvent.PreviousTrack) },
                onTogglePlayback = { onMediaEvent(MediaEvent.TogglePlayback) },
                onNext = { onMediaEvent(MediaEvent.NextTrack) },
                showQueue = false,
            )
        }
    }
}

@Composable
private fun SplitAppDemoPanel(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Split app panel",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Text(
            text = "Lane width tracks the slider. In Scalable UI the same bounds animate on TaskPanel variants (narrow → mid → wide) while the map companion lane stays live.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        listOf("Narrow 40%", "Mid 55%", "Wide 70%").forEachIndexed { index, label ->
            val active = when (index) {
                0 -> fraction < 0.33f
                1 -> fraction in 0.33f..0.66f
                else -> fraction > 0.66f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        if (active) Color(0xFF2A5A9E) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(label, color = Color.White.copy(alpha = if (active) 1f else 0.55f))
            }
        }
    }
}
