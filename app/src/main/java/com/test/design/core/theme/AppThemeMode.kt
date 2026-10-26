package com.test.design.core.theme

import androidx.compose.runtime.compositionLocalOf

enum class AppThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

fun AppThemeMode.resolveDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
    AppThemeMode.System -> systemInDarkTheme
    AppThemeMode.Light -> false
    AppThemeMode.Dark -> true
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.System }
