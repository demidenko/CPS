package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.contests.ContestsInfoDataStore
import com.demich.cps.ui.SettingsColumn
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
    val snapshotSavedState = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(snapshotSavedState) {
        if (!snapshotSavedState.value) {
            val snapshot = context.settingsContests.makeSnapshot()
            ContestsInfoDataStore(context).settingsSnapshot.update {
                snapshot
            }
            snapshotSavedState.value = true
        }
    }
}
