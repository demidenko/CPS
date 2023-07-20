package com.demich.cps.ui

import android.content.Context
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.ui.theme.DarkLightMode
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


val Context.settingsUI: UISettingsDataStore
    get() = UISettingsDataStore(this)

class UISettingsDataStore(context: Context): ItemizedDataStore(context.settingsUI_dataStore) {
    companion object {
        private val Context.settingsUI_dataStore by dataStoreWrapper("settings_ui")
    }

    val darkLightMode = itemEnum(name = "dark_light_mode", defaultValue = DarkLightMode.SYSTEM)

    val useOriginalColors = itemBoolean(name = "use_original_colors", defaultValue = false)

    val coloredStatusBar = itemBoolean(name = "use_status_bar", defaultValue = true)
    val statusBarDisabledManagers = itemEnumSet<AccountManagers>(name = "status_bar_disabled_managers")
    val statusBarResultByMaximum = itemBoolean(name = "status_bar_result_by_max", defaultValue = true)

    private val accountsOrder = jsonCPS.item<List<AccountManagers>>(name = "accounts_order", defaultValue = emptyList())
    suspend fun saveAccountsOrder(order: List<AccountManagers>) = accountsOrder(order)
    fun flowOfAccountsOrder(): Flow<List<AccountManagers>> = accountsOrder.flow.map { order ->
        order + AccountManagers.entries.filter { it !in order }
    }

    val startScreenRoute = itemString(name = "start_screen_route", defaultValue = ScreenTypes.accounts.route)
}