package com.demich.cps.contests.loading_engine

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsFetchResult
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.contests.loading_engine.loaders.ContestsLoader
import com.demich.cps.contests.loading_engine.loaders.ContestsLoaderMultiple
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
    createLoader: (ContestsFetchSource) -> ContestsLoader
): Map<Contest.Platform, Flow<ContestsFetchResult>> {
    val memoizer = MultipleLoadersMemoizer(setup, dateConstraints)

    val loaders = mutableMapOf<ContestsFetchSource, ContestsLoader>()

    return setup.mapValues { (platform, priorities) ->
        contestsFetchFlow(
            platform = platform,
            priorities = priorities,
            dateConstraints = dateConstraints,
            memoizer = memoizer,
            getLoader = { loaders.getOrPut(it) { createLoader(it) } }
        )
    }
}

private fun contestsFetchFlow(
    platform: Contest.Platform,
    priorities: List<ContestsFetchSource>,
    dateConstraints: ContestDateConstraints,
    memoizer: MultipleLoadersMemoizer,
    getLoader: (ContestsFetchSource) -> ContestsLoader
) = flow {
    for (fetchSource in priorities) {
        val loader = getLoader(fetchSource)
        val result: Result<List<Contest>> =
            if (loader is ContestsLoaderMultiple) {
                memoizer.getContestsResult(loader).map {
                    it.getOrElse(platform) { emptyList() }
                }
            } else {
                loader.runCatching {
                    fetchContests(platform = platform, dateConstraints = dateConstraints)
                }
            }

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

private typealias ContestsResult = Result<Map<Contest.Platform, List<Contest>>>

private class MultipleLoadersMemoizer(
    private val setup: Map<Contest.Platform, List<ContestsFetchSource>>,
    private val dateConstraints: ContestDateConstraints
) {
    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsFetchSource, Deferred<ContestsResult>>()
    suspend fun getContestsResult(loader: ContestsLoaderMultiple): ContestsResult {
        return mutex.withLock {
            results.getOrPut(loader.fetchSource) {
                coroutineScope {
                    async { loader.getContests() }
                }
            }
        }.await()
    }

    private suspend fun ContestsLoaderMultiple.getContests(): ContestsResult {
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