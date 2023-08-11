package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loaders.ContestsReceiver
import com.demich.cps.contests.loaders.launchContestsLoading
import com.demich.cps.contests.loading.ContestsLoaders
import com.demich.cps.contests.loading.loaders.AtCoderContestsLoader
import com.demich.cps.contests.loading.loaders.ClistContestsLoader
import com.demich.cps.contests.loading.loaders.CodeforcesContestsLoader
import com.demich.cps.contests.loading.loaders.DmojContestsLoader
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.utils.getCurrentTime
import com.demich.datastore_itemized.edit

interface ContestsReloader {
    suspend fun reloadEnabledPlatforms(
        settings: ContestsSettingsDataStore,
        contestsReceiver: ContestsReceiver
    ) {
        settings.lastReloadedPlatforms(emptySet())
        settings.clistLastReloadedAdditionalResources(emptySet())
        reload(
            platforms = settings.enabledPlatforms(),
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
    launchContestsLoading(
        setup = settings.contestsLoadersPriorityLists().filterKeys { it in platforms },
        dateConstraints = settings.contestsDateConstraints().at(currentTime = getCurrentTime()),
        contestsReceiver = contestsReceiver
    ) { loaderType ->
        when (loaderType) {
            ContestsLoaders.clist_api -> ClistContestsLoader(
                apiAccess = settings.clistApiAccess(),
                includeResourceIds = { settings.clistAdditionalResources().map { it.id } }
            )
            ContestsLoaders.codeforces_api -> CodeforcesContestsLoader()
            ContestsLoaders.atcoder_parse -> AtCoderContestsLoader()
            ContestsLoaders.dmoj_api -> DmojContestsLoader()
        }
    }
}