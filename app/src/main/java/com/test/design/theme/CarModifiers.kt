package com.test.design.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

fun Modifier.carTouchTarget(): Modifier = defaultMinSize(
    minWidth = CarDesignTokens.MinTouchTarget,
    minHeight = CarDesignTokens.MinTouchTarget,
)

fun Modifier.carListItemHeight(): Modifier = defaultMinSize(
    minHeight = CarDesignTokens.ListItemHeight,
)

/** Wide, low-opacity blur so the shadow feathers out instead of ending in a hard edge. */
fun Modifier.softCardShadow(shape: Shape): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = 40.dp,
        spread = (-6).dp,
        offset = DpOffset(0.dp, 10.dp),
        color = Color.Black.copy(alpha = 0.26f),
    ),
)

val CarButtonContentPadding = PaddingValues(
    horizontal = CarDesignTokens.TouchTargetSpacing,
    vertical = 20.dp,
)
