package com.demich.cps.contests.loading_engine

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsFetchResult
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.contests.loading_engine.loaders.ContestsFetcher
import com.demich.cps.contests.loading_engine.loaders.ContestsMultiplatformFetcher
import com.demich.cps.contests.loading_engine.loaders.correctAtCoderTitle
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun contestsFetchFlows(
    setup: Map<Contest.Platform, List<ContestsFetchSource>>,
    dateConstraints: ContestDateConstraints,
    createFetcher: (ContestsFetchSource) -> ContestsFetcher
): Map<Contest.Platform, Flow<ContestsFetchResult>> {
    val memoizer = ContestsFetchMemoizer(setup, dateConstraints, createFetcher)

    return setup.mapValues { (platform, priorities) ->
        contestsFetchFlow(
            platform = platform,
            priorities = priorities,
            memoizer = memoizer
        )
    }
}

private fun contestsFetchFlow(
    platform: Contest.Platform,
    priorities: List<ContestsFetchSource>,
    memoizer: ContestsFetchMemoizer
) = flow {
    for (fetchSource in priorities) {
        val result: Result<List<Contest>> = memoizer.getContests(platform, fetchSource)
        emit(ContestsFetchResult(
            platform = platform,
            fetchSource = fetchSource,
            result = result.map { it.map { it.correctTitle() } }
        ))

        if (result.isSuccess) break
    }
}

private fun Contest.correctTitle(): Contest {
    val fixedTitle = when (platform) {
        Contest.Platform.atcoder -> correctAtCoderTitle(title)
        else -> title
    }.trim()
    return if (title == fixedTitle) this
    else copy(title = fixedTitle)
}

private class ContestsFetchMemoizer(
    private val setup: Map<Contest.Platform, List<ContestsFetchSource>>,
    private val dateConstraints: ContestDateConstraints,
    private val getFetcher: (ContestsFetchSource) -> ContestsFetcher
) {
    private typealias ContestsResult = Result<Map<Contest.Platform, List<Contest>>>

    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsFetchSource, Deferred<ContestsResult>>()
    private val fetchers = mutableMapOf<ContestsFetchSource, ContestsFetcher>()

    suspend fun getContests(
        platform: Contest.Platform,
        source: ContestsFetchSource
    ): Result<List<Contest>> {
        val fetcher = mutex.withLock {
            fetchers.getOrPut(source) {
                getFetcher(source)
            }
        }

        if (fetcher is ContestsMultiplatformFetcher) {
            return mutex.withLock {
                results.getOrPut(source) {
                    coroutineScope {
                        async { fetcher.fetchAllPlatforms() }
                    }
                }
            }.await().map {
                it.getOrElse(platform) { emptyList() }
            }
        } else {
            return fetcher.runCatching {
                fetchContests(
                    platform = platform,
                    dateConstraints = dateConstraints
                )
            }
        }
    }

    private suspend fun ContestsMultiplatformFetcher.fetchAllPlatforms(): ContestsResult {
        val platforms = setup.mapNotNull { (platform, sources) ->
            if (fetchSource in sources) platform else null
        }
        return runCatching {
            fetchContests(
                platforms = platforms,
                dateConstraints = dateConstraints
            ).groupBy { it.platform }
        }
    }
}