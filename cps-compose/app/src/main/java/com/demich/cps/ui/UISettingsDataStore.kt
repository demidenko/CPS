package com.demich.cps.ui

import android.content.Context
import com.demich.cps.platforms.Platform
import com.demich.cps.profiles.managers.profilePlatforms
import com.demich.cps.ui.bottombar.NavigationLayoutType
import com.demich.cps.ui.theme.DarkLightMode
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.combine
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.value


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

    val darkLightMode = itemEnum<DarkLightMode>(name = "dark_light_mode", defaultValue = SYSTEM)

    val useOriginalColors = itemBoolean(name = "use_original_colors", defaultValue = false)

    val coloredStatusBar = itemBoolean(name = "use_status_bar", defaultValue = true)
    val statusBarDisabledPlatforms = itemEnumSet<Platform>(name = "status_bar_disabled_platforms")
    val statusBarRankSelector = itemEnum<StatusBarRankSelector>(name = "status_bar_rank_selector", defaultValue = Max)

    val profilesOrder = jsonCPS.itemList<Platform>(name = "profiles_order").mapGetter { order ->
        order.filter { it in profilePlatforms } + profilePlatforms.filter { it !in order }
    }

    val navigationLayoutType = itemEnum<NavigationLayoutType>(name = "navigation_bar_layout", defaultValue = start)

    val uiColorSpecs get() = combine {
        CPSUIColorSpecs(
            darkLightMode = darkLightMode.value,
            useOriginalColors = useOriginalColors.value
        )
    }

    val bottomBarSpecs get() = combine {
        CPSBottomBarSpecs(
            devModeEnabled = devModeEnabled.value,
            layoutType = navigationLayoutType.value
        )
    }
}

data class CPSUIColorSpecs(
    val darkLightMode: DarkLightMode,
    val useOriginalColors: Boolean
)

data class CPSBottomBarSpecs(
    val devModeEnabled: Boolean,
    val layoutType: NavigationLayoutType
)