package com.test.design.presentation.ivi.dashboard.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.vector.ImageVector

enum class DashboardWidget(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val sharedElementKey: String,
) {
    Climate(
        title = "Climate",
        subtitle = "22°C · Auto airflow",
        icon = Icons.Default.AcUnit,
        sharedElementKey = "widget_climate",
    ),
    Vehicle(
        title = "Vehicle",
        subtitle = "82% charge · 240 mi",
        icon = Icons.Default.DirectionsCar,
        sharedElementKey = "widget_vehicle",
    ),
}
