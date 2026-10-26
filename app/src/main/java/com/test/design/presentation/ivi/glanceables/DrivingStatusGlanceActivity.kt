package com.test.design.presentation.ivi.glanceables

import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.design.core.LocalDrivingUxState
import com.test.design.core.cluster.ClusterUiState
import com.test.design.core.theme.LocalAppThemeMode
import com.test.design.core.theme.resolveDarkTheme
import com.test.design.presentation.ivi.vehicle.VehicleViewModel

/** Scalable UI `driving_status` TaskPanel — speed / range / gear glanceable. */
class DrivingStatusGlanceActivity : GlanceableActivity() {

    private val vehicleViewModel: VehicleViewModel by viewModels()

    @Composable
    override fun GlanceContent() {
        val vehicleState by vehicleViewModel.state.collectAsStateWithLifecycle()
        val cluster = ClusterUiState.fromDrivingUx(LocalDrivingUxState.current)
        GlanceRoot {
            DrivingStatusGlance(
                speedMph = cluster.speedMph,
                speedLimitMph = cluster.speedLimitMph,
                gear = cluster.gear,
                batteryPercent = vehicleState.batteryPercent,
                rangeLabel = vehicleState.rangeLabel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun DrivingStatusGlance(
    speedMph: Int,
    speedLimitMph: Int,
    gear: String,
    batteryPercent: Int,
    rangeLabel: String,
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
                    color = colors.title,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 52.sp,
                )
                SpeedLimitBadge(limit = speedLimitMph, colors = colors)
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
                    trackColor = colors.track,
                )
                Text(
                    text = rangeLabel,
                    color = colors.body,
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
                            else -> colors.inactive
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedLimitBadge(limit: Int, colors: GlancePalette) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .border(2.5.dp, colors.hairline, CircleShape)
            .padding(4.dp)
            .border(2.dp, colors.hairline.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$limit",
                color = colors.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
            Text(
                text = "MAX",
                color = colors.muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp,
            )
        }
    }
}

internal data class GlancePalette(
    val light: Boolean,
    val card: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val track: Color,
    val inactive: Color,
    val hairline: Color,
)

private val DarkGlance = GlancePalette(
    light = false,
    card = Color(0xF01C1C1E),
    title = Color.White,
    body = Color.White.copy(alpha = 0.85f),
    muted = Color.White.copy(alpha = 0.55f),
    track = Color.White.copy(alpha = 0.14f),
    inactive = Color.White.copy(alpha = 0.4f),
    hairline = Color.White.copy(alpha = 0.85f),
)

private val LightGlance = GlancePalette(
    light = true,
    card = Color(0xFFFBFCFD),
    title = Color(0xFF1A1C1E),
    body = Color(0xFF2C3138),
    muted = Color(0xFF5C6570),
    track = Color(0xFFD2D7DF),
    inactive = Color(0xFF6B7280),
    hairline = Color(0xFF1A1C1E),
)

internal val GlanceCardBg = DarkGlance.card

@Composable
internal fun glancePalette(): GlancePalette {
    val dark = LocalAppThemeMode.current.resolveDarkTheme(isSystemInDarkTheme())
    return if (dark) DarkGlance else LightGlance
}

internal fun GlancePalette.cardBorder(): BorderStroke? =
    if (light) BorderStroke(1.dp, LightCardStroke) else null

private val LightCardStroke = Color(0xFFD0D5DC)
private val AccentGreen = Color(0xFF34C759)
private val AccentRed = Color(0xFFE53935)
