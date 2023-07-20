package com.demich.cps.contests.settings

import androidx.compose.runtime.Composable
import com.demich.cps.ui.SettingsSwitchItemWithWork
import com.demich.cps.utils.context
import com.demich.cps.workers.ContestsWorker

@Composable
internal fun AutoUpdateSettingsItem() {
    val context = context
    //TODO: allow choose time interval
    //TODO: set same height
    SettingsSwitchItemWithWork(
        item = context.settingsContests.enabledAutoUpdate,
        title = "Background auto update",
        workGetter = ContestsWorker::getWork
    )
}