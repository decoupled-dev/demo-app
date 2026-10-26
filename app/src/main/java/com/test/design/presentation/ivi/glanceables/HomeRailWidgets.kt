package com.test.design.presentation.ivi.glanceables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.R
import com.test.design.core.theme.LocalAppThemeMode
import com.test.design.core.theme.resolveDarkTheme
import com.test.design.data.media.friendlySourceName
import com.test.design.presentation.ivi.climate.AirflowMode
import com.test.design.presentation.ivi.climate.ClimateEvent
import com.test.design.presentation.ivi.climate.ClimateUiState
import com.test.design.presentation.ivi.climate.ClimateZone
import com.test.design.presentation.ivi.climate.components.ClimateHvacIcons
import com.test.design.presentation.ivi.climate.formatTemperature
import com.test.design.presentation.ivi.media.MediaEvent
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.media.components.MediaArtInitials
import com.test.design.presentation.ivi.media.nowPlayingSubtitle
import com.test.design.presentation.ivi.vehicle.DriveMode
import com.test.design.presentation.ivi.vehicle.VehicleEvent
import com.test.design.presentation.ivi.vehicle.VehicleUiState
import com.test.design.theme.climateAmbientColor
import com.test.design.theme.climateContentAccent
import com.test.design.theme.climateDialFill
import com.test.design.theme.climateDialInk
import com.test.design.theme.temperatureToFraction

/**
 * Home-card size measured from the live launcher widgets on this display:
 * each card window is 696×426 px at 240 dpi.
 */
internal val HomeWidgetCardWidth = 464.dp
internal val HomeWidgetCardHeight = 284.dp

private val CardCorner = RoundedCornerShape(16.dp)

/** Press feedback that does not redraw or reshape the widget corners. */
private object FixedCornerIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}

@Composable
private fun FixedWidgetCorners(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIndication provides FixedCornerIndication, content = content)
}

private fun Modifier.widgetControlClick(onClick: () -> Unit): Modifier = clickable(
    interactionSource = null,
    indication = null,
    onClick = onClick,
)
private val DialCorner = RoundedCornerShape(20.dp)
private val EcoGreen = Color(0xFF3DDC84)
private val ComfortBlue = Color(0xFF7EB6FF)
private val SportOrange = Color(0xFFFF6A3D)
private val EcoGreenInk = Color(0xFF1B8F4E)
private val ComfortBlueInk = Color(0xFF1565C0)
private val SportOrangeInk = Color(0xFFD84315)

private data class WidgetPalette(
    val light: Boolean,
    val title: Color,
    val label: Color,
    val card: Color,
    val control: Color,
    val onControl: Color,
    val track: Color,
    val modeTrack: Color,
    val ink: Color,
)

private val DarkWidgets = WidgetPalette(
    light = false,
    title = Color.White,
    label = Color(0xFFB7BCC4),
    card = Color(0xFF10141C),
    control = Color.White.copy(alpha = 0.08f),
    onControl = Color.White,
    track = Color.White.copy(alpha = 0.14f),
    modeTrack = Color.Black.copy(alpha = 0.28f),
    ink = Color(0xFF10141C),
)

private val LightWidgets = WidgetPalette(
    light = true,
    title = Color(0xFF1A1C1E),
    label = Color(0xFF3A4450),
    card = Color(0xFFFBFCFD),
    control = Color(0xFFE3E7EE),
    onControl = Color(0xFF1A1C1E),
    track = Color(0xFFD2D7DF),
    modeTrack = Color(0xFFE6E9EF),
    ink = Color.White,
)

private val LightCardStroke = Color(0xFFD0D5DC)

@Composable
private fun widgetPalette(): WidgetPalette {
    val dark = LocalAppThemeMode.current.resolveDarkTheme(isSystemInDarkTheme())
    return if (dark) DarkWidgets else LightWidgets
}

/**
 * Climate card in the climate screen's language: ambient glow, one temperature dial,
 * and a quiet fan and airflow line. Not a scaled copy of the full dual-zone screen.
 */
@Composable
fun ClimateQuarter(
    state: ClimateUiState,
    onEvent: (ClimateEvent) -> Unit,
    onOpenMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = temperatureToFraction(
        celsius = state.temperatureCelsius,
        min = state.minTemperature,
        max = state.maxTemperature,
    )
    val passengerFraction = temperatureToFraction(
        celsius = state.passengerTemperatureCelsius,
        min = state.minTemperature,
        max = state.maxTemperature,
    )
    val glow = climateAmbientColor(fraction)
    val passengerGlow = climateAmbientColor(passengerFraction)
    val colors = widgetPalette()
    val accent = climateContentAccent(fraction, colors.light)
    val glowAlpha = if (colors.light) 0.22f else 0.28f
    val passengerAlpha = if (colors.light) 0.12f else 0.18f

    FixedWidgetCorners {
    Column(
        modifier = modifier
            .homeWidgetCard(onOpenMain, colors)
            .background(
                Brush.horizontalGradient(
                    0f to glow.copy(alpha = glowAlpha),
                    0.55f to colors.card,
                    1f to passengerGlow.copy(alpha = passengerAlpha),
                ),
            )
            .lightCardStroke(colors)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Climate",
                color = colors.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            SoftChip(
                text = state.temperatureUnit.shortLabel,
                colors = colors,
                onClick = { onEvent(ClimateEvent.ToggleTemperatureUnit) },
            )
            if (state.capabilities.hasAc) {
            SoftChip(
                text = "A/C",
                selected = state.isAcEnabled,
                tint = accent,
                colors = colors,
                    onClick = { onEvent(ClimateEvent.ToggleAc) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            RoundStepButton(
                contentDescription = "Decrease temperature",
                icon = Icons.Default.Remove,
                onClick = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, -1)) },
            )
            Column(
                modifier = Modifier
                    .width(180.dp)
                    .height(116.dp)
                    .clip(DialCorner)
                    .background(climateDialFill(fraction, colors.light))
                    .border(
                        1.dp,
                        accent.copy(alpha = if (colors.light) 0.38f else 0.45f),
                        DialCorner,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.formatTemperature(state.temperatureCelsius),
                    color = climateDialInk(fraction, colors.light),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = ClimateZone.Driver.label,
                    color = climateDialInk(fraction, colors.light).copy(alpha = 0.72f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            RoundStepButton(
                contentDescription = "Increase temperature",
                icon = Icons.Default.Add,
                onClick = { onEvent(ClimateEvent.AdjustZoneTemperature(ClimateZone.Driver, 1)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.capabilities.hasPassengerTemp) {
                Text(
                    text = "Passenger ${state.formatTemperature(state.passengerTemperatureCelsius)}",
                    color = colors.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (state.capabilities.hasFanSpeed) {
                FanBars(
                    fanSpeed = state.fanSpeed,
                    maxFanSpeed = state.maxFanSpeed,
                    tint = accent,
                    track = colors.track,
                    onSelected = { onEvent(ClimateEvent.SetFanSpeed(it)) },
                )
            }
            if (state.capabilities.hasAirflowControls) {
                AirflowMark(
                    mode = state.airflowMode,
                    tint = accent,
                    onClick = {
                        val modes = airflowChoices(state)
                        if (modes.isEmpty()) return@AirflowMark
                        val next = modes[(modes.indexOf(state.airflowMode) + 1).mod(modes.size)]
                        onEvent(ClimateEvent.SelectAirflow(next))
                    },
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
    }
}

/**
 * Vehicle card in the vehicle screen's language: mode-colored gauge and the drive stage.
 * The tire panel and gear bar stay on the full screen.
 */
@Composable
fun VehicleQuarter(
    state: VehicleUiState,
    onOpenMain: () -> Unit,
    onEvent: (VehicleEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = widgetPalette()
    val accent = driveAccent(state.driveMode, colors.light)
    FixedWidgetCorners {
    Box(
        modifier = modifier
            .homeWidgetCard(onOpenMain, colors)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = if (colors.light) 0.10f else 0.16f), colors.card),
                ),
            )
            .lightCardStroke(colors),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactEnergyGauge(
                percent = state.batteryPercent,
                rangeLabel = state.rangeLabel,
                accent = accent,
                colors = colors,
                modifier = Modifier
                    .width(148.dp)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "DRIVE",
                    color = colors.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.6.sp,
                )
                Text(
                    text = state.driveMode.label,
                    color = colors.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.driveMode.subtitle,
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Image(
                    painter = painterResource(R.drawable.vehicle_suv_side),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                DriveModeRow(
                    selected = state.driveMode,
                    accent = accent,
                    colors = colors,
                    onSelected = { onEvent(VehicleEvent.SelectDriveMode(it)) },
                )
            }
        }
    }
    }
}

/**
 * Media card on the system home: album art, track line, progress, and transport.
 * A tap on the card opens the playing source. Transport buttons stay on the card.
 */
@Composable
fun MediaQuarter(
    state: MediaUiState,
    onEvent: (MediaEvent) -> Unit,
    onOpenMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = widgetPalette()
    val accent = mediaAccent(colors.light)
    val source = friendlySourceName(state.sourceLabel.ifBlank { state.source.label })
    FixedWidgetCorners {
    Column(
        modifier = modifier
            .homeWidgetCard(onOpenMain, colors)
            .background(
                Brush.linearGradient(
                    if (colors.light) {
                        listOf(Color(0xFFF4F7FC), Color.White, Color(0xFFFFF7F8))
                    } else {
                        listOf(Color(0xFF1A2744), colors.card, Color(0xFF241820))
                    },
                ),
            )
            .lightCardStroke(colors)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Media",
                color = colors.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = source,
                color = colors.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clip(AlbumCorner)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5B8DEF), Color(0xFF1A1A2E), Color(0xFF8E3A4A)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                MediaArtInitials(
                    album = state.currentTrack.album,
                    artwork = state.artwork,
                    labelSize = 34.sp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = state.currentTrack.title,
                    color = colors.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.nowPlayingSubtitle(),
                    color = colors.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.track),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                            .background(accent),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(state.elapsedLabel, color = colors.label, fontSize = 15.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(state.currentTrack.durationLabel, color = colors.label, fontSize = 15.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundStepButton(
                        contentDescription = "Previous track",
                        icon = Icons.Default.SkipPrevious,
                        onClick = { onEvent(MediaEvent.PreviousTrack) },
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (state.isPlaying) "Pause" else "Play"
                            }
                            .widgetControlClick { onEvent(MediaEvent.TogglePlayback) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    RoundStepButton(
                        contentDescription = "Next track",
                        icon = Icons.Default.SkipNext,
                        onClick = { onEvent(MediaEvent.NextTrack) },
                    )
                }
            }
        }
    }
    }
}

private val AlbumCorner = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 10.dp,
    bottomEnd = 18.dp,
    bottomStart = 10.dp,
)

private fun Modifier.homeWidgetCard(onOpen: () -> Unit, colors: WidgetPalette): Modifier = this
    .width(HomeWidgetCardWidth)
    .height(HomeWidgetCardHeight)
    .clip(CardCorner)
    .background(colors.card)
    .clickable(
        interactionSource = null,
        indication = null,
        onClick = onOpen,
    )

private fun Modifier.lightCardStroke(colors: WidgetPalette): Modifier =
    if (colors.light) border(1.dp, LightCardStroke, CardCorner) else this

@Composable
private fun CompactEnergyGauge(
    percent: Int,
    rangeLabel: String,
    accent: Color,
    colors: WidgetPalette,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = 9.dp.toPx()
            val diameter = size.minDimension
            val arc = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = colors.track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = 135f,
                sweepAngle = 270f * (percent / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                color = colors.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = rangeLabel,
                color = colors.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DriveModeRow(
    selected: DriveMode,
    accent: Color,
    colors: WidgetPalette,
    onSelected: (DriveMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.modeTrack),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DriveMode.entries.forEach { mode ->
            val on = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) accent else Color.Transparent)
                    .semantics { contentDescription = mode.label }
                    .widgetControlClick { onSelected(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = mode.widgetIcon(),
                    contentDescription = null,
                    tint = if (on) colors.ink else colors.label,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FanBars(
    fanSpeed: Int,
    maxFanSpeed: Int,
    tint: Color,
    track: Color,
    onSelected: (Int) -> Unit,
) {
    val steps = maxFanSpeed.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .width(108.dp)
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (step in 1..steps) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.4f + 0.6f * step / steps)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (step <= fanSpeed) tint else track)
                    .semantics { contentDescription = "Fan $step" }
                    .widgetControlClick { onSelected(if (step == fanSpeed) 0 else step) },
            )
        }
    }
}

@Composable
private fun AirflowMark(
    mode: AirflowMode,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .semantics { contentDescription = mode.label }
            .widgetControlClick(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(mode.widgetIcon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun SoftChip(
    text: String,
    colors: WidgetPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color = ComfortBlue,
) {
    val label = if (selected && colors.light) lerp(tint, colors.title, 0.45f) else tint
    Text(
        text = text,
        color = if (selected) label else colors.title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) tint.copy(alpha = if (colors.light) 0.22f else 0.18f) else colors.control)
            .widgetControlClick(onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun RoundStepButton(
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = widgetPalette()
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.control)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .widgetControlClick(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.onControl, modifier = Modifier.size(26.dp))
    }
}

private fun airflowChoices(state: ClimateUiState): List<AirflowMode> {
    val available = state.capabilities.availableAirflowModes
    return buildList {
        if (AirflowMode.Face in available) add(AirflowMode.Face)
        if (AirflowMode.Feet in available) add(AirflowMode.Feet)
        if (state.capabilities.hasAuto) add(AirflowMode.Auto)
        if (isEmpty()) add(state.airflowMode)
    }
}

private val AirflowMode.widgetIcon: Int
    get() = when (this) {
        AirflowMode.Face -> ClimateHvacIcons.AirflowFace
        AirflowMode.BiLevel -> ClimateHvacIcons.AirflowBiLevel
        AirflowMode.Feet -> ClimateHvacIcons.AirflowFeet
        AirflowMode.FeetDefrost -> ClimateHvacIcons.AirflowFeetDefrost
        AirflowMode.Auto -> ClimateHvacIcons.Auto
    }

private fun driveAccent(mode: DriveMode, light: Boolean): Color = when (mode) {
    DriveMode.Eco -> if (light) EcoGreenInk else EcoGreen
    DriveMode.Comfort -> if (light) ComfortBlueInk else ComfortBlue
    DriveMode.Sport -> if (light) SportOrangeInk else SportOrange
}

private fun mediaAccent(light: Boolean): Color = if (light) ComfortBlueInk else ComfortBlue

private fun DriveMode.widgetIcon(): ImageVector = when (this) {
    DriveMode.Eco -> Icons.Default.Eco
    DriveMode.Comfort -> Icons.Outlined.DirectionsCar
    DriveMode.Sport -> Icons.Default.Speed
}
