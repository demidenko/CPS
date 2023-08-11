package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import com.demich.cps.ui.SettingsColumn


@Composable
fun ContestsSettingsScreen() {
    SettingsColumn {
        ContestPlatformsSettingsItem()
        DateConstraintsSettingsItem()
        ClistApiAccessSettingsItem()
        AutoUpdateSettingsItem()
    }
}
