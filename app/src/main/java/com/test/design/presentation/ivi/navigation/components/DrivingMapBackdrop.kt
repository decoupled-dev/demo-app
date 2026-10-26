package com.test.design.presentation.ivi.navigation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val MapNight = Color(0xFF121418)
private val MapBlock = Color(0xFF1B1F26)
private val MapRoad = Color(0xFF2A303A)
private val MapRoadMajor = Color(0xFF3A4452)
private val MapRoute = Color(0xFF4EA1FF)
private val MapWater = Color(0xFF15202B)

/**
 * Compose-only night map backdrop for the driving home.
 * Avoids a MapView that can paint above Compose siblings on AAOS and leave the launch buffer black.
 */
@Composable
fun DrivingMapBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MapNight),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Water / park blocks
            drawRect(MapWater, topLeft = Offset(w * 0.62f, h * 0.08f), size = Size(w * 0.42f, h * 0.28f))
            drawRect(MapBlock, topLeft = Offset(w * 0.08f, h * 0.12f), size = Size(w * 0.22f, h * 0.18f))
            drawRect(MapBlock, topLeft = Offset(w * 0.36f, h * 0.55f), size = Size(w * 0.28f, h * 0.22f))
            drawRect(MapBlock, topLeft = Offset(w * 0.72f, h * 0.48f), size = Size(w * 0.2f, h * 0.35f))

            val roadStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val majorStroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Grid roads
            for (i in 1..8) {
                val x = w * (i / 9f)
                drawLine(MapRoad, Offset(x, 0f), Offset(x, h), strokeWidth = roadStroke.width)
            }
            for (i in 1..5) {
                val y = h * (i / 6f)
                drawLine(MapRoad, Offset(0f, y), Offset(w, y), strokeWidth = roadStroke.width)
            }

            // Diagonal arterial
            drawLine(
                MapRoadMajor,
                Offset(w * 0.05f, h * 0.85f),
                Offset(w * 0.95f, h * 0.18f),
                strokeWidth = majorStroke.width,
            )
            drawLine(
                MapRoadMajor,
                Offset(w * 0.1f, h * 0.15f),
                Offset(w * 0.9f, h * 0.9f),
                strokeWidth = majorStroke.width,
            )

            // Demo route
            val route = Path().apply {
                moveTo(w * 0.28f, h * 0.78f)
                cubicTo(w * 0.38f, h * 0.62f, w * 0.48f, h * 0.55f, w * 0.58f, h * 0.42f)
                cubicTo(w * 0.66f, h * 0.32f, w * 0.72f, h * 0.28f, w * 0.8f, h * 0.22f)
            }
            drawPath(
                route,
                MapRoute,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Destination / vehicle dots
            drawCircle(MapRoute, radius = 10.dp.toPx(), center = Offset(w * 0.8f, h * 0.22f))
            drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(w * 0.28f, h * 0.78f))
        }
    }
}
