package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.contests.loading_engine.launchContestsLoading
import com.demich.cps.contests.loading_engine.loaders.AtCoderContestsLoader
import com.demich.cps.contests.loading_engine.loaders.ClistContestsLoader
import com.demich.cps.contests.loading_engine.loaders.CodeforcesContestsLoader
import com.demich.cps.contests.loading_engine.loaders.DmojContestsLoader
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
            //fake loading
            contestsReceiver.onStartLoading(platform = Contest.Platform.unknown)
            contestsReceiver.onResult(
                platform = Contest.Platform.unknown,
                result = Result.success(emptyList()),
                loaderType = ContestsLoaderType.clist_api
            )
            contestsReceiver.onFinish(platform = Contest.Platform.unknown)
            //continue without unknown
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
            ContestsLoaderType.clist_api -> ClistContestsLoader(
                apiAccess = settings.clistApiAccess(),
                includeResourceIds = { settings.clistAdditionalResources().map { it.id } }
            )
            ContestsLoaderType.codeforces_api -> CodeforcesContestsLoader()
            ContestsLoaderType.atcoder_parse -> AtCoderContestsLoader()
            ContestsLoaderType.dmoj_api -> DmojContestsLoader()
        }
    }
}