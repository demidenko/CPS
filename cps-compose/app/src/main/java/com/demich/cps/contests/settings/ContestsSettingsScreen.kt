package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import com.demich.cps.contests.ContestsInfoDataStore
import com.demich.cps.ui.settings.SettingsColumn
import com.demich.cps.utils.LaunchedEffectOneTime
import com.demich.cps.utils.context


@Composable
fun ContestsSettingsScreen() {
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
