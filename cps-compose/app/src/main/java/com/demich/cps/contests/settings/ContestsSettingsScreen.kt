package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.demich.cps.contests.ContestsUnitsAndroidFactory
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.toGeneralPlatform
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.platforms.Platform
import com.demich.cps.ui.settings.SettingsColumn
import com.demich.cps.ui.theme.LocalDevModeEnabled
import com.demich.cps.utils.LaunchedEffectOneTime
import com.demich.cps.utils.context

@Composable
private fun ContestsSettingsScreen() {
    val context = context
    val unitsFactory = remember { ContestsUnitsAndroidFactory(context) }
    val settingsStorage = remember { unitsFactory.settingsStorage() }

    val devModeEnabled = LocalDevModeEnabled.current

    context(settingsStorage) {
        SettingsColumn {
            ContestPlatformsSettingsItem()
            DateConstraintsSettingsItem()
            ClistApiAccessSettingsItem()
            AutoUpdateSettingsItem(item = settingsStorage.autoUpdateInterval)

            if (devModeEnabled) {
                DeletedContestsSettingsItem(item = unitsFactory.infoStorage().ignoredContests)
            }
        }
    }

    LaunchedEffectOneTime {
        val snapshot = settingsStorage.fetchSettingsSnapshot()
        unitsFactory.infoStorage().settingsSnapshot.update {
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

val contestPlatforms: List<Platform> =
    Contest.platformsExceptUnknown.map { it.toGeneralPlatform() }