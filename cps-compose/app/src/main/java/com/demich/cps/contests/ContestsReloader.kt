package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsFetchResult
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading_engine.contestsFetchFlows
import com.demich.cps.contests.loading_engine.loaders.AtCoderContestsLoader
import com.demich.cps.contests.loading_engine.loaders.ClistContestsLoader
import com.demich.cps.contests.loading_engine.loaders.CodeforcesContestsLoader
import com.demich.cps.contests.loading_engine.loaders.DmojContestsLoader
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.platforms.clients.AtCoderClient
import com.demich.cps.platforms.clients.ClistClient
import com.demich.cps.platforms.clients.DmojClient
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.utils.getCurrentTime
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.toEnumSet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

interface ContestsReloader {
    suspend fun reloadEnabledPlatforms(
        settings: ContestsSettingsDataStore,
        contestsReceiver: ContestsReceiver
    ) {
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

        coroutineScope {
            settings.contestsFetchFlows(platforms = platforms.toEnumSet())
                .forEach { (platform, resultsFlow) ->
                    launch {
                        val last = transform(platform, resultsFlow).last()
                        last.result.onSuccess { contestsReceiver.save(platform, it) }
                    }
                }
        }
    }

    fun transform(
        platform: Contest.Platform,
        flow: Flow<ContestsFetchResult>
    ): Flow<ContestsFetchResult> {
        return flow
    }
}


private suspend fun ContestsSettingsDataStore.contestsFetchFlows(platforms: Set<Contest.Platform>) =
    fromSnapshot {
        contestsFetchFlows(
            setup = contestsLoadersPriorityLists.value.filterKeys { it in platforms },
            dateConstraints = contestsDateConstraints.value.at(currentTime = getCurrentTime()),
        ) { loaderType ->
            when (loaderType) {
                clist_api -> ClistContestsLoader(
                    api = ClistClient,
                    apiAccess = clistApiAccess.value,
                    resources = clistAdditionalResources.value
                )
                codeforces_api -> CodeforcesContestsLoader(api = CodeforcesClient)
                atcoder_parse -> AtCoderContestsLoader(api = AtCoderClient)
                dmoj_api -> DmojContestsLoader(api = DmojClient)
            }
        }
    }