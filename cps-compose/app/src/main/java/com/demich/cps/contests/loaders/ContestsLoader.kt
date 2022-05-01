package com.demich.cps.contests.loaders

import android.content.Context
import com.demich.cps.contests.Contest

abstract class ContestsLoader(
    val supportedPlatforms: Set<Contest.Platform>
) {
    abstract suspend fun loadContests(platform: Contest.Platform, context: Context): List<Contest>

    suspend fun getContests(platform: Contest.Platform, context: Context): List<Contest> {
        require(platform in supportedPlatforms)
        return loadContests(platform, context)
    }
}

abstract class ContestsLoaderMultiple(
    supportedPlatforms: Set<Contest.Platform>
): ContestsLoader(supportedPlatforms) {

    protected abstract suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        context: Context
    ): List<Contest>

    suspend fun getContests(
        platforms: Collection<Contest.Platform>,
        context: Context
    ): List<Contest> {
        if (platforms.isEmpty()) return emptyList()
        require(supportedPlatforms.containsAll(platforms))
        return loadContests(platforms.toSet(), context)
    }

    final override suspend fun loadContests(platform: Contest.Platform, context: Context) =
        loadContests(platforms = setOf(platform), context = context)
}