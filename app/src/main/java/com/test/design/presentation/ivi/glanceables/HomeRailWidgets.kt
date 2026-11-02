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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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

// ---------------------------------------------------------------------------
// Adaptive sizing
// ---------------------------------------------------------------------------

/** Design reference for the short, wide card (the original launcher widget slot). */
internal val CompactReferenceWidth = 464.dp
internal val CompactReferenceHeight = 284.dp

/** Design reference for the tall card (a full-height Scalable UI column). */
internal val TallReferenceHeight = 600.dp

private const val MinWidgetScale = 0.7f
private const val MaxWidgetScale = 1.8f

/**
 * Size class and scale for a home card, derived from the panel SystemUI gives it.
 *
 * [tall] switches to a stacked layout that uses the extra height for bigger
 * controls and more content. [scale] grows or shrinks type, controls, and
 * spacing so the chosen layout fits the panel without leaving dead space.
 */
@Immutable
internal data class WidgetMetrics(val scale: Float, val tall: Boolean) {
    fun dp(value: Float): Dp = (value * scale).dp
    fun dp(value: Int): Dp = dp(value.toFloat())
    fun sp(value: Float): TextUnit = (value * scale).sp
    fun sp(value: Int): TextUnit = sp(value.toFloat())
}

internal fun widgetMetricsFor(width: Dp, height: Dp): WidgetMetrics {
    if (width <= 0.dp || height <= 0.dp) return WidgetMetrics(1f, tall = false)
    // Taller than ~4:3 (w:h) gets the stacked layout.
    val tall = height >= width * 0.75f
    val referenceHeight = if (tall) TallReferenceHeight else CompactReferenceHeight
    val scale = minOf(width / CompactReferenceWidth, height / referenceHeight)
        .coerceIn(MinWidgetScale, MaxWidgetScale)
    return WidgetMetrics(scale, tall)
}

private val LocalWidgetMetrics = staticCompositionLocalOf { WidgetMetrics(1f, tall = false) }

/**
 * Card shell shared by every home widget: fills the Scalable UI panel, measures it,
 * and hands the resulting [WidgetMetrics] to [content].
 */
@Composable
private fun AdaptiveWidgetCard(
    onOpen: () -> Unit,
    colors: WidgetPalette,
    background: Brush,
    modifier: Modifier = Modifier,
    content: @Composable (WidgetMetrics) -> Unit,
) {
    FixedWidgetCorners {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clip(CardCorner)
                .background(colors.card)
                .background(background)
                .lightCardStroke(colors)
                .clickable(interactionSource = null, indication = null, onClick = onOpen),
        ) {
            val metrics = widgetMetricsFor(maxWidth, maxHeight)
            CompositionLocalProvider(LocalWidgetMetrics provides metrics) {
                content(metrics)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared styling
// ---------------------------------------------------------------------------

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

private fun Modifier.lightCardStroke(colors: WidgetPalette): Modifier =
    if (colors.light) border(1.dp, LightCardStroke, CardCorner) else this

@Composable
private fun CardTitle(text: String, colors: WidgetPalette, m: WidgetMetrics) {
    Text(
        text = text,
        color = colors.title,
        fontSize = m.sp(if (m.tall) 24 else 22),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

// ---------------------------------------------------------------------------
// Climate
// ---------------------------------------------------------------------------

/**
 * Climate card in the climate screen's language: ambient glow, a temperature dial,
 * and a quiet fan and airflow line. Tall panels add the passenger zone as its own control.
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

    AdaptiveWidgetCard(
        onOpen = onOpenMain,
        colors = colors,
        background = Brush.horizontalGradient(
            0f to glow.copy(alpha = glowAlpha),
            0.55f to colors.card,
            1f to passengerGlow.copy(alpha = passengerAlpha),
        ),
        modifier = modifier,
    ) { m ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = m.dp(20), vertical = m.dp(if (m.tall) 20 else 16)),
        ) {
            ClimateHeader(state, onEvent, accent, colors, m)
            if (m.tall) {
                TallClimateBody(state, onEvent, fraction, passengerFraction, accent, colors, m)
            } else {
                CompactClimateBody(state, onEvent, fraction, accent, colors, m)
            }
        }
    }
}

@Composable
private fun ClimateHeader(
    state: ClimateUiState,
    onEvent: (ClimateEvent) -> Unit,
    accent: Color,
    colors: WidgetPalette,
    m: WidgetMetrics,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CardTitle("Climate", colors, m)
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
                modifier = Modifier.padding(start = m.dp(8)),
            )
        }
    }
}

@Composable
private fun ColumnScope.CompactClimateBody(
    state: ClimateUiState,
    onEvent: (ClimateEvent) -> Unit,
    fraction: Float,
    accent: Color,
    colors: WidgetPalette,
    m: WidgetMetrics,
) {
    ZoneStepper(
        zone = ClimateZone.Driver,
        state = state,
        fraction = fraction,
        onEvent = onEvent,
        colors = colors,
        dialWidth = m.dp(180),
        dialHeight = m.dp(116),
        temperatureSize = m.sp(42),
        buttonSize = m.dp(52),
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.capabilities.hasPassengerTemp) {
            Text(
                text = "Passenger ${state.formatTemperature(state.passengerTemperatureCelsius)}",
                color = colors.label,
                fontSize = m.sp(17),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        ClimateAirControls(state, onEvent, accent, colors, m, fanWidth = m.dp(108))
    }
}

@Composable
private fun ColumnScope.TallClimateBody(
    state: ClimateUiState,
    onEvent: (ClimateEvent) -> Unit,
    fraction: Float,
    passengerFraction: Float,
    accent: Color,
    colors: WidgetPalette,
    m: WidgetMetrics,
) {
    Spacer(modifier = Modifier.weight(0.5f))
    ZoneStepper(
        zone = ClimateZone.Driver,
        state = state,
        fraction = fraction,
        onEvent = onEvent,
        colors = colors,
        dialWidth = m.dp(212),
        dialHeight = m.dp(160),
        temperatureSize = m.sp(60),
        buttonSize = m.dp(60),
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.capabilities.hasPassengerTemp) {
        Spacer(modifier = Modifier.weight(0.35f))
        ZoneStepper(
            zone = ClimateZone.Passenger,
            state = state,
            fraction = passengerFraction,
            onEvent = onEvent,
            colors = colors,
            dialWidth = m.dp(212),
            dialHeight = m.dp(96),
            temperatureSize = m.sp(36),
            buttonSize = m.dp(48),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.weight(0.5f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(m.dp(16)))
            .background(colors.control)
            .padding(horizontal = m.dp(16), vertical = m.dp(12)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Fan",
            color = colors.label,
            fontSize = m.sp(17),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = m.dp(14)),
        )
        ClimateAirControls(
            state = state,
            onEvent = onEvent,
            accent = accent,
            colors = colors,
            m = m,
            fanWidth = null,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One zone: − / dial / +. Used at hero size for the driver and smaller for the passenger. */
@Composable
private fun ZoneStepper(
    zone: ClimateZone,
    state: ClimateUiState,
    fraction: Float,
    onEvent: (ClimateEvent) -> Unit,
    colors: WidgetPalette,
    dialWidth: Dp,
    dialHeight: Dp,
    temperatureSize: TextUnit,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
) {
    val m = LocalWidgetMetrics.current
    val accent = climateContentAccent(fraction, colors.light)
    val ink = climateDialInk(fraction, colors.light)
    val celsius = if (zone == ClimateZone.Driver) state.temperatureCelsius else state.passengerTemperatureCelsius
    val dialShape = RoundedCornerShape(m.dp(20))
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(m.dp(16), Alignment.CenterHorizontally),
    ) {
        RoundStepButton(
            contentDescription = "Decrease ${zone.label.lowercase()} temperature",
            icon = Icons.Default.Remove,
            size = buttonSize,
            onClick = { onEvent(ClimateEvent.AdjustZoneTemperature(zone, -1)) },
        )
        Column(
            modifier = Modifier
                .width(dialWidth)
                .height(dialHeight)
                .clip(dialShape)
                .background(climateDialFill(fraction, colors.light))
                .border(1.dp, accent.copy(alpha = if (colors.light) 0.38f else 0.45f), dialShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.formatTemperature(celsius),
                color = ink,
                fontSize = temperatureSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = zone.label,
                color = ink.copy(alpha = 0.72f),
                fontSize = m.sp(16),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        RoundStepButton(
            contentDescription = "Increase ${zone.label.lowercase()} temperature",
            icon = Icons.Default.Add,
            size = buttonSize,
            onClick = { onEvent(ClimateEvent.AdjustZoneTemperature(zone, 1)) },
        )
    }
}

/** Fan bars plus airflow toggle. [fanWidth] null lets the bars take the remaining width. */
@Composable
private fun ClimateAirControls(
    state: ClimateUiState,
    onEvent: (ClimateEvent) -> Unit,
    accent: Color,
    colors: WidgetPalette,
    m: WidgetMetrics,
    fanWidth: Dp?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (state.capabilities.hasFanSpeed) {
            FanBars(
                fanSpeed = state.fanSpeed,
                maxFanSpeed = state.maxFanSpeed,
                tint = accent,
                track = colors.track,
                onSelected = { onEvent(ClimateEvent.SetFanSpeed(it)) },
                modifier = if (fanWidth != null) {
                    Modifier.width(fanWidth).height(m.dp(28))
                } else {
                    Modifier.weight(1f).height(m.dp(36))
                },
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
                modifier = Modifier.padding(start = m.dp(10)),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Vehicle
// ---------------------------------------------------------------------------

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
    AdaptiveWidgetCard(
        onOpen = onOpenMain,
        colors = colors,
        background = Brush.verticalGradient(
            listOf(accent.copy(alpha = if (colors.light) 0.10f else 0.16f), colors.card),
        ),
        modifier = modifier,
    ) { m ->
        if (m.tall) {
            TallVehicleBody(state, onEvent, accent, colors, m)
        } else {
            CompactVehicleBody(state, onEvent, accent, colors, m)
        }
    }
}

@Composable
private fun CompactVehicleBody(
    state: VehicleUiState,
    onEvent: (VehicleEvent) -> Unit,
    accent: Color,
    colors: WidgetPalette,
    m: WidgetMetrics,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = m.dp(16), end = m.dp(16), top = m.dp(16), bottom = m.dp(14)),
        horizontalArrangement = Arrangement.spacedBy(m.dp(16)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactEnergyGauge(
            percent = state.batteryPercent,
            rangeLabel = state.rangeLabel,
            accent = accent,
            colors = colors,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(148f / 254f, matchHeightConstraintsFirst = true),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(m.dp(6)),
        ) {
            DriveModeHeading(state, accent, colors, m)
            VehicleImage(Modifier.weight(1f).fillMaxWidth())
            DriveModeRow(state.driveMode, accent, colors) { onEvent(VehicleEvent.SelectDriveMode(it)) }
        }
    }
}

@Composable
private fun TallVehicleBody(
    state: VehicleUiState,
    onEvent: (VehicleEvent) -> Unit,
    accent: Color,
    colors: WidgetPalette,
    m: WidgetMetrics,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = m.dp(20), vertical = m.dp(20)),
        verticalArrangement = Arrangement.spacedBy(m.dp(10)),
    ) {
        DriveModeHeading(state, accent, colors, m)
        CompactEnergyGauge(
            percent = state.batteryPercent,
            rangeLabel = state.rangeLabel,
            accent = accent,
            colors = colors,
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth(),
        )
        VehicleImage(Modifier.weight(0.8f).fillMaxWidth())
        DriveModeRow(state.driveMode, accent, colors) { onEvent(VehicleEvent.SelectDriveMode(it)) }
    }
}

@Composable
private fun DriveModeHeading(state: VehicleUiState, accent: Color, colors: WidgetPalette, m: WidgetMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(m.dp(4))) {
        Text(
            text = "DRIVE",
            color = colors.label,
            fontSize = m.sp(11),
            fontWeight = FontWeight.Medium,
            letterSpacing = m.sp(1.6f),
        )
        Text(
            text = state.driveMode.label,
            color = colors.title,
            fontSize = m.sp(if (m.tall) 30 else 26),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = state.driveMode.subtitle,
            color = accent,
            fontSize = m.sp(13),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VehicleImage(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.vehicle_suv_side),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------
// Media
// ---------------------------------------------------------------------------

/**
 * Media card on the system home: album art, track line, progress, and transport.
 * A tap on the card opens the playing source. Transport buttons stay on the card.
 * Tall panels stack hero artwork above the track details.
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
    AdaptiveWidgetCard(
        onOpen = onOpenMain,
        colors = colors,
        background = Brush.linearGradient(
            if (colors.light) {
                listOf(Color(0xFFF4F7FC), Color.White, Color(0xFFFFF7F8))
            } else {
                listOf(Color(0xFF1A2744), colors.card, Color(0xFF241820))
            },
        ),
        modifier = modifier,
    ) { m ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = m.dp(18), vertical = m.dp(if (m.tall) 18 else 14)),
            verticalArrangement = Arrangement.spacedBy(m.dp(12)),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CardTitle("Media", colors, m)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = source,
                    color = colors.label,
                    fontSize = m.sp(16),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (m.tall) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AlbumArt(
                        state = state,
                        labelSize = m.sp(56),
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true),
                    )
                }
                TrackDetails(state, onEvent, accent, colors, m, centered = true)
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(m.dp(16)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        state = state,
                        labelSize = m.sp(34),
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        TrackDetails(state, onEvent, accent, colors, m, centered = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumArt(state: MediaUiState, labelSize: TextUnit, modifier: Modifier) {
    val m = LocalWidgetMetrics.current
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = m.dp(18),
                    topEnd = m.dp(10),
                    bottomEnd = m.dp(18),
                    bottomStart = m.dp(10),
                ),
            )
            .background(
                Brush.linearGradient(listOf(Color(0xFF5B8DEF), Color(0xFF1A1A2E), Color(0xFF8E3A4A))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        MediaArtInitials(
            album = state.currentTrack.album,
            artwork = state.artwork,
            labelSize = labelSize,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TrackDetails(
    state: MediaUiState,
    onEvent: (MediaEvent) -> Unit,
    accent: Color,
    colors: WidgetPalette,
    m: WidgetMetrics,
    centered: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(m.dp(8)),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = state.currentTrack.title,
            color = colors.title,
            fontSize = m.sp(if (m.tall) 28 else 26),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.nowPlayingSubtitle(),
            color = colors.label,
            fontSize = m.sp(17),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .padding(top = m.dp(4))
                .fillMaxWidth()
                .height(m.dp(4))
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
            Text(state.elapsedLabel, color = colors.label, fontSize = m.sp(15))
            Spacer(modifier = Modifier.weight(1f))
            Text(state.currentTrack.durationLabel, color = colors.label, fontSize = m.sp(15))
        }
        val playSize = m.dp(if (m.tall) 68 else 56)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(m.dp(if (m.tall) 20 else 10), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundStepButton(
                contentDescription = "Previous track",
                icon = Icons.Default.SkipPrevious,
                size = m.dp(if (m.tall) 56 else 52),
                onClick = { onEvent(MediaEvent.PreviousTrack) },
            )
            Box(
                modifier = Modifier
                    .size(playSize)
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
                    modifier = Modifier.size(playSize * 0.57f),
                )
            }
            RoundStepButton(
                contentDescription = "Next track",
                icon = Icons.Default.SkipNext,
                size = m.dp(if (m.tall) 56 else 52),
                onClick = { onEvent(MediaEvent.NextTrack) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

@Composable
private fun CompactEnergyGauge(
    percent: Int,
    rangeLabel: String,
    accent: Color,
    colors: WidgetPalette,
    modifier: Modifier = Modifier,
) {
    val m = LocalWidgetMetrics.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(m.dp(8))) {
            val diameter = size.minDimension
            // Stroke keeps a consistent weight relative to the ring.
            val stroke = (diameter * 0.068f).coerceAtLeast(6.dp.toPx())
            val inset = stroke / 2f
            val arc = Size(diameter - stroke, diameter - stroke)
            val topLeft = Offset(
                (size.width - diameter) / 2f + inset,
                (size.height - diameter) / 2f + inset,
            )
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
                sweepAngle = 270f * (percent.coerceIn(0, 100) / 100f),
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
                fontSize = m.sp(if (m.tall) 44 else 32),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = rangeLabel,
                color = colors.label,
                fontSize = m.sp(if (m.tall) 16 else 13),
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
    val m = LocalWidgetMetrics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(m.dp(if (m.tall) 48 else 36))
            .clip(RoundedCornerShape(m.dp(12)))
            .background(colors.modeTrack),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DriveMode.entries.forEach { mode ->
            val on = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(m.dp(3))
                    .clip(RoundedCornerShape(m.dp(10)))
                    .background(if (on) accent else Color.Transparent)
                    .semantics { contentDescription = mode.label }
                    .widgetControlClick { onSelected(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = mode.widgetIcon(),
                    contentDescription = null,
                    tint = if (on) colors.ink else colors.label,
                    modifier = Modifier.size(m.dp(if (m.tall) 22 else 16)),
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
    modifier: Modifier = Modifier,
) {
    val m = LocalWidgetMetrics.current
    val steps = maxFanSpeed.coerceAtLeast(1)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(m.dp(4)),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (step in 1..steps) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.4f + 0.6f * step / steps)
                    .clip(RoundedCornerShape(m.dp(2)))
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
    val m = LocalWidgetMetrics.current
    Box(
        modifier = modifier
            .size(m.dp(if (m.tall) 48 else 40))
            .clip(CircleShape)
            .semantics { contentDescription = mode.label }
            .widgetControlClick(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(mode.widgetIcon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(m.dp(if (m.tall) 30 else 26)),
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
    val m = LocalWidgetMetrics.current
    val label = if (selected && colors.light) lerp(tint, colors.title, 0.45f) else tint
    Text(
        text = text,
        color = if (selected) label else colors.title,
        fontSize = m.sp(16),
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(m.dp(10)))
            .background(if (selected) tint.copy(alpha = if (colors.light) 0.22f else 0.18f) else colors.control)
            .widgetControlClick(onClick)
            .padding(horizontal = m.dp(12), vertical = m.dp(6)),
    )
}

@Composable
private fun RoundStepButton(
    contentDescription: String,
    icon: ImageVector,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = widgetPalette()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.control)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .widgetControlClick(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.onControl, modifier = Modifier.size(size * 0.5f))
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
