package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.demich.cps.contests.ContestsInfoDataStore
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.ui.settings.SettingsColumn
import com.demich.cps.ui.settingsUI
import com.demich.cps.utils.LaunchedEffectOneTime
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context

@Composable
private fun ContestsSettingsScreen() {
    val context = context
    val settings = remember { context.settingsContests }

    val devModeEnabled by collectItemAsState { context.settingsUI.devModeEnabled }

    SettingsColumn {
        ContestPlatformsSettingsItem(
            enabledPlatformsItem = settings.enabledPlatforms,
            clistAdditionalResourcesItem = settings.clistAdditionalResources
        )
        DateConstraintsSettingsItem(settings = settings)
        ClistApiAccessSettingsItem(item = settings.clistApiAccess)
        AutoUpdateSettingsItem(item = settings.autoUpdateInterval)

        if (devModeEnabled) {
            DeletedContestsSettingsItem(item = ContestsInfoDataStore(context).ignoredContests)
        }
    }

    LaunchedEffectOneTime {
        val snapshot = context.settingsContests.makeSnapshot()
        ContestsInfoDataStore(context).settingsSnapshot.update {
            // do not override
            it ?: snapshot
        }
    }
}

@Composable
fun CPSNavigator.ScreenScope<Screen.ContestsSettings>.NavContentContestsSettingsScreen() {
    screenTitle = ScreenStaticTitleState("contests", "settings")

    ContestsSettingsScreen()
}