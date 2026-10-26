package com.test.design.core.theme

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ThemeModeViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val stateKey = "app_theme_mode"
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        if (key == stateKey) {
            val next = shared.getString(stateKey, null) ?: return@OnSharedPreferenceChangeListener
            if (savedStateHandle.get<String>(stateKey) != next) {
                savedStateHandle[stateKey] = next
            }
        }
    }

    init {
        val stored = prefs.getString(stateKey, null)
        if (stored != null) {
            savedStateHandle[stateKey] = stored
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onCleared()
    }

    val themeMode: StateFlow<AppThemeMode> = savedStateHandle
        .getStateFlow(stateKey, prefs.getString(stateKey, null) ?: AppThemeMode.System.name)
        .map(::parseMode)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = parseMode(savedStateHandle[stateKey] ?: prefs.getString(stateKey, null)),
        )

    fun update(mode: AppThemeMode) {
        savedStateHandle[stateKey] = mode.name
        prefs.edit().putString(stateKey, mode.name).apply()
    }

    private fun parseMode(raw: String?): AppThemeMode =
        AppThemeMode.entries.find { it.name == raw } ?: AppThemeMode.System

    private companion object {
        const val PREFS = "design_theme"
    }
}

val LocalThemeModeUpdater = staticCompositionLocalOf<(AppThemeMode) -> Unit> {
    error("ThemeModeUpdater not provided")
}
