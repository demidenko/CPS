package com.demich.cps.contests

import com.demich.cps.contests.database.ContestPlatform
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
import com.demich.datastore_itemized.DataStoreSnapshot
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value

suspend fun ContestsSettingsDataStore.contestsFetchFlows() =
    fromSnapshot {
        makeContestsFetchFlows(platforms = enabledContestPlatforms.value)
    }

suspend fun ContestsSettingsDataStore.contestsFetchFlows(platforms: Set<ContestPlatform>) =
    fromSnapshot {
        makeContestsFetchFlows(platforms)
    }

context(scope: DataStoreSnapshot)
private fun ContestsSettingsDataStore.makeContestsFetchFlows(platforms: Set<ContestPlatform>) =
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