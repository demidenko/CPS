package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestTimePrefs
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend fun getContests(
    setup: Map<Contest.Platform, List<ContestsLoaders>>,
    settings: ContestsSettingsDataStore
): Map<Contest.Platform, List<Result<List<Contest>>>> {
    val timeLimits = settings.contestsTimePrefs().createLimits(now = getCurrentTime())
    val loaders = settings.createLoaders().associateBy { it.type }
    val groupedResults = coroutineScope {
        val memorizer = MultipleLoadersMemorizer(setup, timeLimits)
        setup.map { (platform, priorities) ->
            require(priorities.isNotEmpty())
            async {
                val result = loadUntilSuccess(
                    platform = platform,
                    priorities = priorities,
                    loaders = loaders,
                    memorizer = memorizer,
                    timeLimits = timeLimits
                )
                platform to result
            }
        }.awaitAll().toMap()
    }
    return groupedResults
}

private suspend fun loadUntilSuccess(
    platform: Contest.Platform,
    priorities: List<ContestsLoaders>,
    loaders: Map<ContestsLoaders, ContestsLoader>,
    memorizer: MultipleLoadersMemorizer,
    timeLimits: ContestTimePrefs.Limits
): List<Result<List<Contest>>> {
    val results = mutableListOf<Result<List<Contest>>>()
    for (loaderType in priorities) {
        val loader = loaders.getValue(loaderType)
        val result: Result<List<Contest>> = if (loader is ContestsLoaderMultiple) {
            memorizer.get(loader = loader).map { contests ->
                contests.filter { it.platform == platform }
            }
        } else {
            runCatching {
                loader.getContests(platform = platform, timeLimits = timeLimits)
            }
        }
        results.add(result)
        if (result.isSuccess) break
    }
    return results
}

private class MultipleLoadersMemorizer(
    private val setup: Map<Contest.Platform, List<ContestsLoaders>>,
    private val timeLimits: ContestTimePrefs.Limits
) {
    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsLoaders, Deferred<Result<List<Contest>>>>()
    suspend fun get(loader: ContestsLoaderMultiple): Result<List<Contest>> {
        return coroutineScope {
            mutex.withLock {
                results.getOrPut(loader.type) {
                    async { runLoader(loader) }
                }
            }
        }.await()
    }

    private suspend fun runLoader(loader: ContestsLoaderMultiple): Result<List<Contest>> {
        val platforms = setup.mapNotNull { (platform, loaderTypes) ->
            if (loader.type in loaderTypes) platform else null
        }
        return kotlin.runCatching {
            loader.getContests(
                platforms = platforms,
                timeLimits = timeLimits
            )
        }
    }
}

private suspend fun ContestsSettingsDataStore.createLoaders(): List<ContestsLoader> = listOf(
        ClistContestsLoader(
            apiAccess = clistApiAccess(),
            includeResourceIds = { clistAdditionalResources().map { it.id } }
        ),
        CodeforcesContestsLoader()
    )