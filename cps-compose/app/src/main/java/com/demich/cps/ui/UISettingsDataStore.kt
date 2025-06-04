package com.demich.cps.ui

import android.content.Context
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.ui.bottombar.NavigationLayoutType
import com.demich.cps.ui.theme.DarkLightMode
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper


val Context.settingsUI: UISettingsDataStore
    get() = UISettingsDataStore(this)

class UISettingsDataStore(context: Context): ItemizedDataStore(context.settingsUI_dataStore) {
    companion object {
        private val Context.settingsUI_dataStore by dataStoreWrapper("settings_ui")
    }

    enum class StatusBarRankSelector {
        Min, Max
    }

    val devModeEnabled = itemBoolean(name = "develop_enabled", defaultValue = false)

    val darkLightMode = itemEnum(name = "dark_light_mode", defaultValue = DarkLightMode.SYSTEM)

    val useOriginalColors = itemBoolean(name = "use_original_colors", defaultValue = false)

    val coloredStatusBar = itemBoolean(name = "use_status_bar", defaultValue = true)
    val statusBarDisabledManagers = itemEnumSet<AccountManagerType>(name = "status_bar_disabled_managers")
    val statusBarRankSelector = itemEnum(name = "status_bar_rank_selector", defaultValue = StatusBarRankSelector.Max)

    val profilesOrder = jsonCPS.itemList<AccountManagerType>(name = "profiles_order").mapGetter { order ->
        order + AccountManagerType.entries.filter { it !in order }
    }

    val startScreenRoute = itemString(name = "start_screen_route", defaultValue = ScreenTypes.profiles.route)
    val navigationLayoutType = itemEnum(name = "navigation_bar_layout", defaultValue = NavigationLayoutType.start)

}