package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.demich.cps.ui.SettingsColumn
import com.demich.cps.ui.settingsUI
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect


@Composable
fun ContestsSettingsScreen() {
    val context = context
    val devModeEnabled by rememberCollect { context.settingsUI.devModeEnabled.flow }

    SettingsColumn {
        ContestPlatformsSettingsItem()
        DateConstraintsSettingsItem()
        ClistApiAccessSettingsItem()
        AutoUpdateSettingsItem()

        if (devModeEnabled) {
            DeletedContestsSettingsItem()
        }
    }
}
