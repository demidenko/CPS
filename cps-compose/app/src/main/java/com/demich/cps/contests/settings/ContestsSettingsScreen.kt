package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.demich.cps.ui.SettingsColumn
import com.demich.cps.utils.context


@Composable
fun ContestsSettingsScreen() {
    val settings = with(context) { remember { settingsContests } }

    SettingsColumn {
        ContestPlatformsSettingsItem(
            enabledPlatformsItem = settings.enabledPlatforms
        )
        ClistApiKeySettingsItem(item = settings.clistApiAccess)
    }
}
