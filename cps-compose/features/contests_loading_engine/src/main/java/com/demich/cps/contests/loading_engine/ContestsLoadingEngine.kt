package com.demich.cps.contests.loading_engine

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.contests.loading.ContestsLoadingResult
import com.demich.cps.contests.loading_engine.loaders.ContestsLoader
import com.demich.cps.contests.loading_engine.loaders.ContestsLoaderMultiple
import com.demich.cps.contests.loading_engine.loaders.correctAtCoderTitle
import com.demich.kotlin_stdlib_boost.emptyEnumSet
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun contestsLoadingFlows(
    setup: Map<Contest.Platform, List<ContestsLoaderType>>,
    dateConstraints: ContestDateConstraints,
    createLoader: (ContestsLoaderType) -> ContestsLoader
): Map<Contest.Platform, Flow<ContestsLoadingResult>> {
    val memoizer = MultipleLoadersMemoizer(setup, dateConstraints)

    val possibleLoaders = setup.flatMapTo(emptyEnumSet()) { it.value }
        .associateWith { lazy { createLoader(it) } }

    return setup.mapValues { (platform, priorities) ->
        contestsLoadingFlow(
            platform = platform,
            priorities = priorities,
            dateConstraints = dateConstraints,
            memoizer = memoizer,
            getLoader = { possibleLoaders.getValue(it).value }
        )
    }
}

private fun contestsLoadingFlow(
    platform: Contest.Platform,
    priorities: List<ContestsLoaderType>,
    dateConstraints: ContestDateConstraints,
    memoizer: MultipleLoadersMemoizer,
    getLoader: (ContestsLoaderType) -> ContestsLoader
) = flow {
    for (loaderType in priorities) {
        val loader = getLoader(loaderType)
        val result: Result<List<Contest>> =
            if (loader is ContestsLoaderMultiple) {
                memoizer.getContestsResult(loader).map {
                    it.getOrElse(platform) { emptyList() }
                }
            } else {
                loader.runCatching {
                    getContests(platform = platform, dateConstraints = dateConstraints)
                }
            }

        emit(ContestsLoadingResult(
            platform = platform,
            loaderType = loaderType,
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
    private val setup: Map<Contest.Platform, List<ContestsLoaderType>>,
    private val dateConstraints: ContestDateConstraints
) {
    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsLoaderType, Deferred<ContestsResult>>()
    suspend fun getContestsResult(loader: ContestsLoaderMultiple): ContestsResult {
        return mutex.withLock {
            results.getOrPut(loader.type) {
                coroutineScope {
                    async { loader.getContests() }
                }
            }
        }.await()
    }

    private suspend fun ContestsLoaderMultiple.getContests(): ContestsResult {
        val platforms = setup.mapNotNull { (platform, loaderTypes) ->
            if (type in loaderTypes) platform else null
        }
        return runCatching {
            getContests(
                platforms = platforms,
                dateConstraints = dateConstraints
            ).groupBy { it.platform }
        }
    }
}