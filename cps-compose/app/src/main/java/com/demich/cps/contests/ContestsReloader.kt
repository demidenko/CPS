package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateBaseConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.contests.loading.ContestsLoadingResult
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading_engine.contestsLoadingFlows
import com.demich.cps.contests.loading_engine.loaders.AtCoderContestsLoader
import com.demich.cps.contests.loading_engine.loaders.ClistContestsLoader
import com.demich.cps.contests.loading_engine.loaders.CodeforcesContestsLoader
import com.demich.cps.contests.loading_engine.loaders.DmojContestsLoader
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.platforms.api.ClistResource
import com.demich.cps.utils.getCurrentTime
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

interface ContestsReloader {
    suspend fun reloadEnabledPlatforms(
        settings: ContestsSettingsDataStore,
        contestsInfo: ContestsInfoDataStore,
        contestsReceiver: ContestsReceiver
    ) {
        contestsInfo.edit { prefs ->
            prefs[lastReloadedPlatforms] = emptySet()
            prefs[clistLastReloadedAdditionalResources] = emptySet()
        }
        reload(
            platforms = settings.flowOfEnabledPlatforms().first(),
            settings = settings,
            contestsInfo = contestsInfo,
            contestsReceiver = contestsReceiver
        )
    }

    suspend fun reload(
        platforms: Collection<Contest.Platform>,
        settings: ContestsSettingsDataStore,
        contestsInfo: ContestsInfoDataStore,
        contestsReceiver: ContestsReceiver
    ) {
        if (platforms.isEmpty()) {
            return
        }

        contestsInfo.lastReloadedPlatforms.edit { addAll(platforms) }
        if (Contest.Platform.unknown in platforms) {
            val ids = settings.clistAdditionalResources().map { it.id }
            contestsInfo.clistLastReloadedAdditionalResources.edit { addAll(ids) }
        }

        coroutineScope {
            contestsLoadingFlows(
                platforms = platforms,
                settings = settings
            ).forEach { (platform, resultsFlow) ->
                resultsFlow.onStart {
                    contestsReceiver.onStartLoading(platform)
                }.onEach {
                    contestsReceiver.onResult(it)
                }.onCompletion {
                    contestsReceiver.onFinish(platform)
                }.launchIn(this)
            }
        }
    }
}


private suspend fun contestsLoadingFlows(
    platforms: Collection<Contest.Platform>,
    settings: ContestsSettingsDataStore
): Map<Contest.Platform, Flow<ContestsLoadingResult>> {

    val clistApiAccess: ClistApi.ApiAccess
    val clistAdditionalResources: List<ClistResource>
    val contestsDateConstraints: ContestDateBaseConstraints
    val contestsLoadersPriorityLists: Map<Contest.Platform, List<ContestsLoaderType>>
    settings.snapshot().let {
        clistApiAccess = it[settings.clistApiAccess]
        clistAdditionalResources = it[settings.clistAdditionalResources]
        contestsDateConstraints = it[settings.contestsDateConstraints]
        contestsLoadersPriorityLists = it[settings.contestsLoadersPriorityLists]
    }

    return contestsLoadingFlows(
        setup = contestsLoadersPriorityLists.filterKeys { it in platforms },
        dateConstraints = contestsDateConstraints.at(currentTime = getCurrentTime()),
    ) { loaderType ->
        when (loaderType) {
            ContestsLoaderType.clist_api -> ClistContestsLoader(
                apiAccess = clistApiAccess,
                additionalResources = clistAdditionalResources
            )
            ContestsLoaderType.codeforces_api -> CodeforcesContestsLoader()
            ContestsLoaderType.atcoder_parse -> AtCoderContestsLoader()
            ContestsLoaderType.dmoj_api -> DmojContestsLoader()
        }
    }
}