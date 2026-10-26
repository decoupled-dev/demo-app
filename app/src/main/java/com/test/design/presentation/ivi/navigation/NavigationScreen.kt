package com.test.design.presentation.ivi.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.test.design.presentation.ivi.dashboard.model.DashboardWidget
import com.test.design.presentation.ivi.navigation.components.OpenMapBackground
import com.test.design.presentation.ivi.navigation.components.mapChromeLayer
import com.test.design.theme.CarDesignTokens
import com.test.design.theme.ExpressiveShapes
import com.test.design.theme.carTouchTarget
import com.test.design.theme.navigationGlassPanelColor

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.NavigationScreen(
    @Suppress("UNUSED_PARAMETER") uiState: NavigationUiState,
    @Suppress("UNUSED_PARAMETER") onEvent: (NavigationEvent) -> Unit,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    embedMap: Boolean = true,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Full-bleed map stays outside sharedBounds. A TextureView inside the
        // container-transform overlay renders blank.
        // Driving home passes embedMap = false and keeps one map across back.
        if (embedMap) {
            OpenMapBackground(
                modifier = Modifier.fillMaxSize(),
                showRoute = false,
                interactive = true,
            )
        }

        Surface(
            shape = ExpressiveShapes.large,
            color = navigationGlassPanelColor(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(CarDesignTokens.TouchTargetSpacing)
                .fillMaxWidth()
                .mapChromeLayer(),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = DashboardWidget.Navigation.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .carTouchTarget(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(CarDesignTokens.PrimaryIcon),
                    )
                }
            }
        }
    }
}
