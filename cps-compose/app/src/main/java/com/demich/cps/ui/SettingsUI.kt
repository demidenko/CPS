package com.demich.cps.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.CPSDataStore

class SettingsUI(private val context: Context): CPSDataStore(context.settingsUI_dataStore) {
    companion object {
        private val Context.settingsUI_dataStore by preferencesDataStore("settings_ui")
    }

    val useOriginalColors = Item(booleanPreferencesKey("use_original_colors"), false)
    val coloredStatusBar = Item(booleanPreferencesKey("use_status_bar"), true)
    val darkLightMode = ItemEnum("ui_mode", DarkLightMode::class.java, defaultValueCallback = { DarkLightMode.SYSTEM })
}

enum class DarkLightMode {
    DARK, LIGHT, SYSTEM;

    @Composable
    fun isDarkMode(): Boolean = if(this == SYSTEM) isSystemInDarkTheme() else this == DARK
}


val Context.settingsUI: SettingsUI
    get() = SettingsUI(this)