package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import com.demich.cps.contests.ContestsInfoDataStore
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.ui.settings.SettingsColumn
import com.demich.cps.utils.LaunchedEffectOneTime
import com.demich.cps.utils.context

@Composable
private fun ContestsSettingsScreen() {
    SettingsColumn {
        ContestPlatformsSettingsItem()
        DateConstraintsSettingsItem()
        ClistApiAccessSettingsItem()
        AutoUpdateSettingsItem()

        DeletedContestsSettingsItem()
    }

    val context = context
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