package com.demich.cps.contests

import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.loading.ContestsFetchResult
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading_engine.contestsFetchFlows
import com.demich.cps.contests.loading_engine.fetchers.AtCoderContestsFetcher
import com.demich.cps.contests.loading_engine.fetchers.ClistContestsFetcher
import com.demich.cps.contests.loading_engine.fetchers.CodeforcesContestsFetcher
import com.demich.cps.contests.loading_engine.fetchers.DmojContestsFetcher
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.platforms.clients.AtCoderClient
import com.demich.cps.platforms.clients.ClistClient
import com.demich.cps.platforms.clients.DmojClient
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.utils.getSystemTime
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
            platforms = settings.enabledContestPlatforms(),
            settings = settings,
            contestsReceiver = contestsReceiver
        )
    }

    suspend fun reload(
        platforms: Collection<ContestPlatform>,
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
        platform: ContestPlatform,
        flow: Flow<ContestsFetchResult>
    ): Flow<ContestsFetchResult> {
        return flow
    }
}


private suspend fun ContestsSettingsDataStore.contestsFetchFlows(platforms: Set<ContestPlatform>) =
    fromSnapshot {
        contestsFetchFlows(
            setup = fetchPriorityLists.value.filterKeys { it in platforms },
            dateConstraints = contestsDateConstraints.value.at(currentTime = getSystemTime()),
        ) { fetchSource ->
            when (fetchSource) {
                clist_api -> ClistContestsFetcher(
                    api = ClistClient,
                    apiAccess = clistApiAccess.value,
                    resources = clistAdditionalResources.value
                )
                codeforces_api -> CodeforcesContestsFetcher(api = CodeforcesClient)
                atcoder_parse -> AtCoderContestsFetcher(api = AtCoderClient)
                dmoj_api -> DmojContestsFetcher(api = DmojClient)
            }
        }
    }