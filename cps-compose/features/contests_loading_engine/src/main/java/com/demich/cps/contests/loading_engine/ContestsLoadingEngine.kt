package com.demich.cps.contests.loading_engine

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsFetchResult
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.contests.loading_engine.fetchers.ContestsFetcher
import com.demich.cps.contests.loading_engine.fetchers.ContestsMultiplatformFetcher
import com.demich.cps.contests.loading_engine.fetchers.ContestsSinglePlatformFetcher
import com.demich.cps.contests.loading_engine.fetchers.correctAtCoderTitle
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun contestsFetchFlows(
    setup: Map<ContestPlatform, List<ContestsFetchSource>>,
    dateConstraints: ContestDateConstraints,
    createFetcher: (ContestsFetchSource) -> ContestsFetcher
): Map<ContestPlatform, Flow<ContestsFetchResult>> {
    val memoizer = ContestsFetchMemoizer(setup, dateConstraints, createFetcher)

    return setup.mapValues { (platform, priorities) ->
        priorities.toFetchFlow(
            platform = platform,
            memoizer = memoizer
        ).transformWhile {
            emit(it)
            it.result.isFailure
        }
    }
}

private fun List<ContestsFetchSource>.toFetchFlow(
    platform: ContestPlatform,
    memoizer: ContestsFetchMemoizer
): Flow<ContestsFetchResult> = flow {
    forEach { fetchSource ->
        val result = ContestsFetchResult(
            platform = platform,
            fetchSource = fetchSource,
            result = memoizer.getContests(platform, fetchSource).map { it.map { it.correctTitle() } }
        )

        check(result.platform == platform)
        check(result.fetchSource == fetchSource)

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
    private val getFetcher: (ContestsFetchSource) -> ContestsFetcher
) {
    private typealias ContestsResult = Result<Map<ContestPlatform, List<Contest>>>

    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsFetchSource, Deferred<ContestsResult>>()
    private val fetchers = mutableMapOf<ContestsFetchSource, ContestsFetcher>()

    suspend fun getContests(
        platform: ContestPlatform,
        source: ContestsFetchSource
    ): Result<List<Contest>> {
        val fetcher = mutex.withLock {
            fetchers.getOrPut(source) {
                getFetcher(source)
            }
        }

        return when (fetcher) {
            is ContestsSinglePlatformFetcher -> {
                fetcher.runCatching {
                    fetchContests(dateConstraints = dateConstraints)
                }
            }
            is ContestsMultiplatformFetcher -> {
                mutex.withLock {
                    results.getOrPut(source) {
                        coroutineScope {
                            async { fetcher.fetchAllPlatforms() }
                        }
                    }
                }.await().map {
                    it.getOrElse(platform) { emptyList() }
                }
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