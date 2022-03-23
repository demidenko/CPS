package com.demich.cps.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.CPSDataStore

class SettingsUI(context: Context): CPSDataStore(context.settingsUI_dataStore) {
    companion object {
        private val Context.settingsUI_dataStore by preferencesDataStore("settings_ui")
    }

    val darkLightMode = itemEnum("ui_mode", DarkLightMode.SYSTEM)

    val useOriginalColors = Item(booleanPreferencesKey("use_original_colors"), false)

    val coloredStatusBar = Item(booleanPreferencesKey("use_status_bar"), true)
    val statusBarDisabledManagers = Item(stringSetPreferencesKey("status_bar_disabled_managers"), emptySet())
}

enum class DarkLightMode {
    DARK, LIGHT, SYSTEM;

    @Composable
    fun isDarkMode(): Boolean = if(this == SYSTEM) isSystemInDarkTheme() else this == DARK
}


val Context.settingsUI: SettingsUI
    get() = SettingsUI(this)


val LocalUseOriginalColors = compositionLocalOf { false }
val useOriginalColors: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalUseOriginalColors.current