package com.demich.cps.contests.loaders

import android.content.Context
import com.demich.cps.contests.Contest
import kotlinx.coroutines.*

suspend fun getContests(
    setup: Map<Contest.Platform, List<ContestsLoaders>>,
    context: Context
): List<Contest> {
    val groupedResults = coroutineScope {
        val loaders = getAllLoaders().associateBy { it.type }
        val memorizer = MultipleLoadersMemorizer(setup = setup, context = context)
        setup.map { (platform, priorities) ->
            require(priorities.isNotEmpty())
            async {
                val result = loadUntilSuccess(
                    platform = platform,
                    priorities = priorities,
                    loaders = loaders,
                    memorizer = memorizer,
                    context = context
                )
                platform to result
            }
        }.awaitAll().toMap()
    }
    return groupedResults.flatMap { (platform, results) ->
        //TODO: throwables
        results.last().getOrDefault(emptyList())
    }
}

private suspend fun loadUntilSuccess(
    platform: Contest.Platform,
    priorities: List<ContestsLoaders>,
    loaders: Map<ContestsLoaders, ContestsLoader>,
    memorizer: MultipleLoadersMemorizer,
    context: Context
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
                loader.getContests(platform = platform, context = context)
            }
        }
        results.add(result)
        if (result.isSuccess) break
    }
    return results
}

private class MultipleLoadersMemorizer(
    private val setup: Map<Contest.Platform, List<ContestsLoaders>>,
    private val context: Context
) {
    private val results = mutableMapOf<ContestsLoaders, Deferred<Result<List<Contest>>>>()
    suspend fun get(loader: ContestsLoaderMultiple): Result<List<Contest>> {
        return runBlocking {
            results.getOrPut(loader.type) {
                async { runLoader(loader) }
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
                context = context
            )
        }
    }
}

private fun getAllLoaders(): List<ContestsLoader> = listOf(
    ClistContestsLoader(),
    CodeforcesContestsLoader()
)