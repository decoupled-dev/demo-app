package com.test.design.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.test.design.core.DrivingUxState
import com.test.design.core.LocalDrivingUxState
import com.test.design.core.motion.AppMotionScheme
import com.test.design.core.motion.LocalAppMotionScheme
import com.test.design.core.motion.LocalEffectiveMotionScheme
import com.test.design.core.motion.resolveMotionScheme
import com.test.design.core.theme.AppThemeMode
import com.test.design.core.theme.LocalAppThemeMode
import com.test.design.core.theme.resolveDarkTheme

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D2A40),
    primaryContainer = Color(0xFF2A4A62),
    onPrimaryContainer = Color(0xFFD4EAFF),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF1A2228),
    secondaryContainer = Color(0xFF3A454D),
    onSecondaryContainer = Color(0xFFDDE4E8),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF0A2E2A),
    tertiaryContainer = Color(0xFF2A4A46),
    onTertiaryContainer = Color(0xFFB8E8E2),
    background = CarBackgroundTokens.CanvasDark,
    onBackground = Color.White,
    surface = Color(0xFF22252A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2E3238),
    onSurfaceVariant = Color(0xFFE0E3E8),
    surfaceContainer = Color(0xFF282B31),
    surfaceContainerHigh = Color(0xFF32363D),
    surfaceContainerHighest = Color(0xFF3C4149),
    surfaceContainerLow = Color(0xFF1E2126),
    surfaceContainerLowest = Color(0xFF16181C),
    outline = Color(0xFF4A5058),
    outlineVariant = Color(0xFF383D44),
)

private val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E3F5),
    onPrimaryContainer = Color(0xFF0D2A40),
    secondary = Color(0xFF546E7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE4E8),
    onSecondaryContainer = Color(0xFF1A2228),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF0A2E2A),
    background = CarBackgroundTokens.CanvasLight,
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE2E5EA),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainer = Color(0xFFEEF0F3),
    surfaceContainerHigh = Color(0xFFE8EAEE),
    surfaceContainerHighest = Color(0xFFE2E5EA),
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFF72787F),
    outlineVariant = Color(0xFFC2C7CE),
)

@Composable
fun AppTheme(
    themeMode: AppThemeMode = AppThemeMode.System,
    darkTheme: Boolean = themeMode.resolveDarkTheme(isSystemInDarkTheme()),
    drivingUxState: DrivingUxState = DrivingUxState.Parked,
    appMotionScheme: AppMotionScheme = AppMotionScheme.Expressive,
    content: @Composable () -> Unit,
) {
    val effectiveScheme = resolveMotionScheme(drivingUxState, appMotionScheme)
    val motionScheme = effectiveScheme.toMotionScheme()
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    val view = LocalView.current
    val context = LocalContext.current
    SideEffect {
        val activity = context as? ComponentActivity ?: return@SideEffect
        if (view.isInEditMode) return@SideEffect
        val barStyle = if (darkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        activity.enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle,
        )
    }

    CompositionLocalProvider(
        LocalDrivingUxState provides drivingUxState,
        LocalAppMotionScheme provides appMotionScheme,
        LocalEffectiveMotionScheme provides effectiveScheme,
        LocalAppThemeMode provides themeMode,
    ) {
        // M3 1.5+ no longer sets LocalContentColor; without this, Text defaults to black.
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = CarTypography,
                motionScheme = motionScheme,
                content = content,
            )
        }
    }
}
