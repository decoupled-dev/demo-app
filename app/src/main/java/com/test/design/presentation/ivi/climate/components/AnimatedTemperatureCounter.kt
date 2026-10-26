package com.test.design.presentation.ivi.climate.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset

@Composable
fun AnimatedTemperatureCounter(
    temperatureLabel: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    /** Used only to pick slide direction when the numeric value changes. */
    sortKey: Float = 0f,
    textStyle: TextStyle? = null,
) {
    val motionSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

    AnimatedContent(
        targetState = Triple(temperatureLabel, sortKey, compact),
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.second > initialState.second) 1 else -1
            slideInVertically(animationSpec = motionSpec) { height -> direction * height } togetherWith
                slideOutVertically(animationSpec = motionSpec) { height -> -direction * height }
        },
        label = "temperature_counter",
    ) { (label, _, isCompact) ->
        Text(
            text = label,
            style = textStyle ?: if (isCompact) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.displayLarge
            },
            color = color,
            textAlign = TextAlign.Center,
            maxLines = if (isCompact) 1 else Int.MAX_VALUE,
            softWrap = !isCompact,
        )
    }
}
