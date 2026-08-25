package com.demich.cps.contests.loading_engine

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.ContestsRepository
import com.demich.cps.contests.fetching.ContestDateConstraints
import com.demich.cps.contests.fetching.ContestsFetchResult
import com.demich.cps.contests.fetching.ContestsFetchSource
import com.demich.cps.contests.loading_engine.fetchers.ContestsFetcher
import com.demich.cps.contests.loading_engine.fetchers.ContestsMultiplatformFetcher
import com.demich.cps.contests.loading_engine.fetchers.ContestsSinglePlatformFetcher
import com.demich.cps.contests.loading_engine.fetchers.correctAtCoderTitle
import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.fetchstate.asResult
import com.demich.cps.fetchstate.fetchResultOf
import com.demich.cps.fetchstate.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend fun Map<ContestPlatform, Flow<ContestsFetchResult>>.collectTo(
    repository: ContestsRepository
) {
    if (isEmpty()) return

    coroutineScope {
        forEach { (platform, flow) ->
            launch {
                flow.firstOrNull { it.result.isSuccess }
                    ?.result
                    ?.onSuccess {
                        repository.setContests(platform = platform, contests = it)
                    }
            }
        }
    }
}

fun contestsFetchFlows(
    cacheScope: CoroutineScope,
    setup: Map<ContestPlatform, List<ContestsFetchSource>>,
    dateConstraints: ContestDateConstraints,
    createFetcher: (ContestsFetchSource) -> ContestsFetcher
): Map<ContestPlatform, Flow<ContestsFetchResult>> {
    val memoizer = ContestsFetchMemoizer(
        setup = setup,
        dateConstraints = dateConstraints,
        getFetcher = createFetcher,
        cacheScope = cacheScope
    )

    return setup.mapValues { (platform, priorities) ->
        priorities.toFetchFlow(
            platform = platform,
            memoizer = memoizer
        )
    }
}

private fun List<ContestsFetchSource>.toFetchFlow(
    platform: ContestPlatform,
    memoizer: ContestsFetchMemoizer
): Flow<ContestsFetchResult> = flow {
    forEach { fetchSource ->
        val fetchResult = memoizer.getContests(platform, fetchSource)
        val result = ContestsFetchResult(
            platform = platform,
            fetchSource = fetchSource,
            result = fetchResult.map { it.map { it.correctTitle() } }.asResult()
        )

        emit(result)
    }
}

private fun Contest.correctTitle(): Contest {
    val fixedTitle = when (platform) {
        atcoder -> correctAtCoderTitle(title)
        else -> title
    }.trim()
    return if (title == fixedTitle) this
    else copy(title = fixedTitle)
}

private class ContestsFetchMemoizer(
    private val setup: Map<ContestPlatform, List<ContestsFetchSource>>,
    private val dateConstraints: ContestDateConstraints,
    private val getFetcher: (ContestsFetchSource) -> ContestsFetcher,
    private val cacheScope: CoroutineScope
) {
    private typealias ContestsResult = FetchResult<Map<ContestPlatform, List<Contest>>>

    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsFetchSource, Deferred<ContestsResult>>()
    private val fetchers = mutableMapOf<ContestsFetchSource, ContestsFetcher>()

    suspend fun getContests(
        platform: ContestPlatform,
        source: ContestsFetchSource
    ): FetchResult<List<Contest>> {
        val fetcher = mutex.withLock {
            fetchers.getOrPut(source) {
                getFetcher(source)
            }
        }

        return when (fetcher) {
            is ContestsSinglePlatformFetcher -> {
                fetchResultOf {
                    fetcher.fetchContests(dateConstraints = dateConstraints)
                }
            }
            is ContestsMultiplatformFetcher -> {
                mutex.withLock {
                    results.getOrPut(source) {
                        cacheScope.async {
                            fetchResultOf {
                                fetcher.fetchAllPlatforms()
                            }
                        }
                    }
                }.await().map {
                    it.getOrElse(platform) { emptyList() }
                }
            }
        }
    }

    private suspend fun ContestsMultiplatformFetcher.fetchAllPlatforms(): Map<ContestPlatform, List<Contest>> {
        val platforms = setup.mapNotNull { (platform, sources) ->
            if (fetchSource in sources) platform else null
        }
        return fetchContests(
            platforms = platforms,
            dateConstraints = dateConstraints
        ).groupBy { it.platform }
    }
}