package com.demich.cps.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.Screen
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.utils.CPSDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsUI(context: Context): CPSDataStore(context.settingsUI_dataStore) {
    companion object {
        private val Context.settingsUI_dataStore by preferencesDataStore("settings_ui")
    }

    val darkLightMode = itemEnum(name = "dark_light_mode", defaultValue = DarkLightMode.SYSTEM)

    val useOriginalColors = itemBoolean(name = "use_original_colors", defaultValue = false)

    val coloredStatusBar = itemBoolean(name = "use_status_bar", defaultValue = true)
    val statusBarDisabledManagers = itemEnumSet<AccountManagers>(name = "status_bar_disabled_managers")
    val statusBarResultByMaximum = itemBoolean(name = "status_bar_result_by_max", defaultValue = true)

    private val accountsOrder = itemJsonable<List<AccountManagers>>(name = "accounts_order", defaultValue = emptyList())
    suspend fun saveAccountsOrder(order: List<AccountManagers>) = accountsOrder(order)
    fun flowOfAccountsOrder(): Flow<List<AccountManagers>> = accountsOrder.flow.map { order ->
        order + AccountManagers.values().filter { it !in order }
    }

    val startScreenRoute = itemString(name = "start_screen_route", defaultValue = Screen.Accounts.routePattern)
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