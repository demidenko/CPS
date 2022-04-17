package com.demich.cps.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.itemBoolean
import com.demich.cps.utils.itemJsonable

class SettingsUI(context: Context): CPSDataStore(context.settingsUI_dataStore) {
    companion object {
        private val Context.settingsUI_dataStore by preferencesDataStore("settings_ui")
    }

    val darkLightMode = itemEnum("ui_mode", DarkLightMode.SYSTEM)

    val useOriginalColors = itemBoolean(name = "use_original_colors", defaultValue = false)

    val coloredStatusBar = itemBoolean(name = "use_status_bar", defaultValue = true)
    val statusBarDisabledManagers = itemEnumSet<AccountManagers>("status_bar_disabled_managers")
    val statusBarResultByMaximum = itemBoolean(name = "status_bar_result_by_max", defaultValue = true)

    val accountsOrder = itemJsonable<List<AccountManagers>>(name = "accounts_order", defaultValue = emptyList())
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