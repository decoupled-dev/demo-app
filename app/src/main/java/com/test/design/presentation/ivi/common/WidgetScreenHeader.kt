package com.test.design.presentation.ivi.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.dashboard.widgetIconSharedElement
import com.test.design.presentation.ivi.dashboard.widgetTitleSharedElement
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.carTouchTarget

@Composable
fun WidgetScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    trailingContent: @Composable () -> Unit = {},
) {
    WidgetScreenHeaderContent(
        title = title,
        onBack = onBack,
        modifier = modifier,
        showBack = showBack,
        trailingContent = trailingContent,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.WidgetScreenHeader(
    widget: DashboardWidget,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    trailingContent: @Composable () -> Unit = {},
) {
    WidgetScreenHeaderContent(
        title = widget.title,
        onBack = onBack,
        modifier = modifier,
        showBack = showBack,
        trailingContent = trailingContent,
        iconContent = {
            Icon(
                imageVector = widget.icon,
                contentDescription = null,
                modifier = widgetIconSharedElement(
                    widget = widget,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.size(CarDesignTokens.PrimaryIcon),
                ),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        },
        titleModifier = widgetTitleSharedElement(
            widget = widget,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier,
        ),
    )
}

@Composable
private fun WidgetScreenHeaderContent(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    trailingContent: @Composable () -> Unit = {},
    iconContent: @Composable (() -> Unit)? = null,
    titleModifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 420.dp) {
            NarrowWidgetHeader(
                title = title,
                onBack = onBack,
                showBack = showBack,
                trailingContent = trailingContent,
                iconContent = iconContent,
                titleModifier = titleModifier,
            )
        } else {
            WideWidgetHeader(
                title = title,
                onBack = onBack,
                showBack = showBack,
                trailingContent = trailingContent,
                iconContent = iconContent,
                titleModifier = titleModifier,
            )
        }
    }
}

@Composable
private fun NarrowWidgetHeader(
    title: String,
    onBack: () -> Unit,
    showBack: Boolean,
    trailingContent: @Composable () -> Unit,
    iconContent: @Composable (() -> Unit)?,
    titleModifier: Modifier,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.carTouchTarget(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to dashboard",
                        modifier = Modifier.size(CarDesignTokens.PrimaryIcon),
                    )
                }
            }
            iconContent?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = titleModifier,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailingContent()
        }
    }
}

@Composable
private fun WideWidgetHeader(
    title: String,
    onBack: () -> Unit,
    showBack: Boolean,
    trailingContent: @Composable () -> Unit,
    iconContent: @Composable (() -> Unit)?,
    titleModifier: Modifier,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (showBack) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .carTouchTarget(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to dashboard",
                    modifier = Modifier.size(CarDesignTokens.PrimaryIcon),
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(CarDesignTokens.TouchTargetSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            iconContent?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = titleModifier,
            )
            if (iconContent != null) {
                Spacer(
                    modifier = Modifier.width(
                        CarDesignTokens.PrimaryIcon,
                    ),
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            trailingContent()
        }
    }
}
