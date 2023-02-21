package com.demich.cps.contests

import com.demich.cps.contests.loaders.ContestsReceiver
import com.demich.cps.contests.loaders.getContests
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.datastore_itemized.edit

interface ContestsReloader {
    suspend fun reloadEnabledPlatforms(
        settings: ContestsSettingsDataStore,
        contestsReceiver: ContestsReceiver
    ) {
        val enabledPlatforms = settings.enabledPlatforms()
        settings.lastReloadedPlatforms.updateValue { emptySet() }
        reload(
            platforms = enabledPlatforms,
            settings = settings,
            contestsReceiver = contestsReceiver
        )
    }

    suspend fun reload(
        platforms: Collection<Contest.Platform>,
        settings: ContestsSettingsDataStore,
        contestsReceiver: ContestsReceiver
    ) {
        if (platforms.isEmpty()) {
            return
        }

        settings.lastReloadedPlatforms.edit { addAll(platforms) }
        if (Contest.Platform.unknown in platforms) {
            val ids = settings.clistAdditionalResources().map { it.id }
            settings.clistLastReloadedAdditionalResources.edit { addAll(ids) }
        }

        loadContests(
            platforms = platforms,
            settings = settings,
            contestsReceiver = contestsReceiver
        )
    }
}


private suspend fun loadContests(
    platforms: Collection<Contest.Platform>,
    settings: ContestsSettingsDataStore,
    contestsReceiver: ContestsReceiver
) {
    if (Contest.Platform.unknown in platforms) {
        if (settings.clistAdditionalResources().isEmpty()) {
            contestsReceiver.finishSuccess(
                platform = Contest.Platform.unknown,
                contests = emptyList()
            )
            loadContests(
                platforms = platforms - Contest.Platform.unknown,
                settings = settings,
                contestsReceiver = contestsReceiver
            )
            return
        }
    }
    getContests(
        setup = settings.contestsLoadersPriorityLists().filterKeys { it in platforms },
        settings = settings,
        contestsReceiver = contestsReceiver
    )
}