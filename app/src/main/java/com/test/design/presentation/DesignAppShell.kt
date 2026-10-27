package com.test.design.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.design.core.driving.DrivingUxViewModel
import com.test.design.core.driving.LocalDrivingUxUpdater
import com.test.design.core.motion.LocalMotionSchemeUpdater
import com.test.design.core.motion.MotionSchemeViewModel
import com.test.design.core.theme.LocalThemeModeUpdater
import com.test.design.core.theme.ThemeModeViewModel
import com.test.design.presentation.common.ScreenWithBackground
import com.test.design.theme.AppTheme

@Composable
fun DesignAppShell(
    applySafeDrawingInsets: Boolean = true,
    content: @Composable () -> Unit,
) {
    val drivingUxViewModel: DrivingUxViewModel = viewModel()
    val motionSchemeViewModel: MotionSchemeViewModel = viewModel()
    val themeModeViewModel: ThemeModeViewModel = viewModel()
    val drivingState by drivingUxViewModel.drivingUxState.collectAsStateWithLifecycle()
    val motionScheme by motionSchemeViewModel.motionScheme.collectAsStateWithLifecycle()
    val themeMode by themeModeViewModel.themeMode.collectAsStateWithLifecycle()

    AppTheme(
        themeMode = themeMode,
        drivingUxState = drivingState,
        appMotionScheme = motionScheme,
    ) {
        CompositionLocalProvider(
            LocalDrivingUxUpdater provides drivingUxViewModel::update,
            LocalMotionSchemeUpdater provides motionSchemeViewModel::update,
            LocalThemeModeUpdater provides themeModeViewModel::update,
        ) {
            val shellModifier = if (applySafeDrawingInsets) {
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            } else {
                Modifier.fillMaxSize()
            }
            ScreenWithBackground(modifier = shellModifier) {
                Box(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }
}
