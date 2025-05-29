package com.demich.cps.contests.loading_engine

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.contests.loading.ContestsReceiver
import com.demich.cps.contests.loading_engine.loaders.ContestsLoader
import com.demich.cps.contests.loading_engine.loaders.ContestsLoaderMultiple
import com.demich.cps.contests.loading_engine.loaders.correctAtCoderTitle
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend fun launchContestsLoading(
    setup: Map<Contest.Platform, List<ContestsLoaderType>>,
    dateConstraints: ContestDateConstraints,
    contestsReceiver: ContestsReceiver,
    createLoader: suspend (ContestsLoaderType) -> ContestsLoader
) {
    val memoizer = MultipleLoadersMemoizer(setup, dateConstraints)

    val possibleLoaders = setup.flatMapTo(mutableSetOf()) { it.value }
        .associateWith { createLoader(it) }

    coroutineScope {
        setup.forEach { (platform, priorities) ->
            loadUntilSuccess(
                platform = platform,
                priorities = priorities,
                dateConstraints = dateConstraints,
                memoizer = memoizer,
                getLoader = possibleLoaders::getValue
            ).onStart {
                contestsReceiver.onStartLoading(platform)
            }.onEach {
                contestsReceiver.onResult(
                    platform = platform,
                    loaderType = it.loaderType,
                    result = it.result
                )
            }.onCompletion {
                if (it != null) throw it
                contestsReceiver.onFinish(platform)
            }.launchIn(this)
        }
    }
}

private class LoadingResult(
    val loaderType: ContestsLoaderType,
    val result: Result<List<Contest>>
)

private fun loadUntilSuccess(
    platform: Contest.Platform,
    priorities: List<ContestsLoaderType>,
    dateConstraints: ContestDateConstraints,
    memoizer: MultipleLoadersMemoizer,
    getLoader: (ContestsLoaderType) -> ContestsLoader
) = flow {
    require(priorities.isNotEmpty())
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
        emit(LoadingResult(
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

typealias ContestsLoadResult = Result<Map<Contest.Platform, List<Contest>>>

private class MultipleLoadersMemoizer(
    private val setup: Map<Contest.Platform, List<ContestsLoaderType>>,
    private val dateConstraints: ContestDateConstraints
) {
    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsLoaderType, Deferred<ContestsLoadResult>>()
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