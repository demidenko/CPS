package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.demich.cps.ui.SettingsColumn
import com.demich.cps.utils.context


@Composable
fun ContestsSettingsScreen(navController: NavController) {
    val settings = with(context) { remember { settingsContests } }

    SettingsColumn {
        ContestPlatformsSettingsItem(item = settings.enabledPlatforms)
        ClistApiKeySettingsItem(item = settings.clistApiAccess)
        ClistAdditionalPlatformsSettingsItem(item = settings.clistAdditionalResources)
    }
}
