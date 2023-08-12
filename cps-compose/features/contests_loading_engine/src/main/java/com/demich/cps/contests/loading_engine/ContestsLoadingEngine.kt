package com.demich.cps.contests.loading_engine

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaders
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading_engine.loaders.ContestsLoader
import com.demich.cps.contests.loading_engine.loaders.ContestsLoaderMultiple
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

suspend fun launchContestsLoading(
    setup: Map<Contest.Platform, List<ContestsLoaders>>,
    dateConstraints: ContestDateConstraints,
    contestsReceiver: ContestsReceiver,
    createLoader: suspend (ContestsLoaders) -> ContestsLoader
) {
    val memoizer = MultipleLoadersMemoizer(setup, dateConstraints)

    val possibleLoaders = setup.flatMapTo(mutableSetOf()) { it.value }
        .associateWith { createLoader(it) }

    coroutineScope {
        setup.forEach { (platform, priorities) ->
            launch {
                loadUntilSuccess(
                    platform = platform,
                    priorities = priorities,
                    dateConstraints = dateConstraints,
                    contestsReceiver = contestsReceiver,
                    memoizer = memoizer,
                    getLoader = possibleLoaders::getValue
                )
            }
        }
    }
}

private suspend fun loadUntilSuccess(
    platform: Contest.Platform,
    priorities: List<ContestsLoaders>,
    dateConstraints: ContestDateConstraints,
    contestsReceiver: ContestsReceiver,
    memoizer: MultipleLoadersMemoizer,
    getLoader: (ContestsLoaders) -> ContestsLoader
) {
    require(priorities.isNotEmpty())
    contestsReceiver.onStartLoading(platform)
    for (loaderType in priorities) {
        val loader = getLoader(loaderType)
        val result: Result<List<Contest>> = withContext(Dispatchers.IO) {
            if (loader is ContestsLoaderMultiple) {
                memoizer.getContestsResult(loader).map {
                    it.getOrElse(platform) { emptyList() }
                }
            } else {
                loader.runCatching {
                    getContests(platform = platform, dateConstraints = dateConstraints)
                }
            }
        }
        contestsReceiver.onResult(
            platform = platform,
            loaderType = loaderType,
            result = result.map { it.map(::titleFix) }
        )
        if (result.isSuccess) break
    }
    contestsReceiver.onFinish(platform)
}

private fun titleFix(contest: Contest): Contest {
    val fixedTitle = when (contest.platform) {
        Contest.Platform.atcoder -> {
            contest.title
                .replace("（", " (")
                .replace("）",") ")
        }
        else -> contest.title
    }.trim()
    return if (contest.title == fixedTitle) contest
    else contest.copy(title = fixedTitle)
}

typealias ContestsLoadResult = Result<Map<Contest.Platform, List<Contest>>>

private class MultipleLoadersMemoizer(
    private val setup: Map<Contest.Platform, List<ContestsLoaders>>,
    private val dateConstraints: ContestDateConstraints
) {
    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsLoaders, Deferred<ContestsLoadResult>>()
    suspend fun getContestsResult(loader: ContestsLoaderMultiple): ContestsLoadResult {
        return coroutineScope {
            mutex.withLock {
                results.getOrPut(loader.type) {
                    async { runLoader(loader) }
                }
            }
        }.await()
    }

    private suspend fun runLoader(loader: ContestsLoaderMultiple): ContestsLoadResult {
        val platforms = setup.mapNotNull { (platform, loaderTypes) ->
            if (loader.type in loaderTypes) platform else null
        }
        return loader.runCatching {
            getContests(
                platforms = platforms,
                dateConstraints = dateConstraints
            ).groupBy { it.platform }
        }
    }
}