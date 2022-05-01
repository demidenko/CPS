package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestTimePrefs
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend fun getContests(
    setup: Map<Contest.Platform, List<ContestsLoaders>>,
    timeLimits: ContestTimePrefs.Limits,
    settings: ContestsSettingsDataStore
): Map<Contest.Platform, List<Result<List<Contest>>>> {
    val groupedResults = coroutineScope {
        val loaders = getAllLoaders().associateBy { it.type }
        val memorizer = MultipleLoadersMemorizer(setup, timeLimits, settings)
        setup.map { (platform, priorities) ->
            require(priorities.isNotEmpty())
            async {
                val result = loadUntilSuccess(
                    platform = platform,
                    priorities = priorities,
                    loaders = loaders,
                    memorizer = memorizer,
                    timeLimits = timeLimits,
                    settings = settings
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
    timeLimits: ContestTimePrefs.Limits,
    settings: ContestsSettingsDataStore
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
                loader.getContests(platform, timeLimits, settings)
            }
        }
        results.add(result)
        if (result.isSuccess) break
    }
    return results
}

private class MultipleLoadersMemorizer(
    private val setup: Map<Contest.Platform, List<ContestsLoaders>>,
    private val timeLimits: ContestTimePrefs.Limits,
    private val settings: ContestsSettingsDataStore
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
                timeLimits = timeLimits,
                settings = settings
            )
        }
    }
}

private fun getAllLoaders(): List<ContestsLoader> = listOf(
    ClistContestsLoader(),
    CodeforcesContestsLoader()
)