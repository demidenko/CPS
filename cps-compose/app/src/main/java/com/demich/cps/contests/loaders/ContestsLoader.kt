package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestTimePrefs
import com.demich.cps.contests.settings.ContestsSettingsDataStore


enum class ContestsLoaders {
    clist,
    codeforces
}

abstract class ContestsLoader(
    val supportedPlatforms: Set<Contest.Platform>,
    val type: ContestsLoaders
) {
    abstract suspend fun loadContests(
        platform: Contest.Platform,
        timeLimits: ContestTimePrefs.Limits,
        settings: ContestsSettingsDataStore
    ): List<Contest>

    suspend fun getContests(
        platform: Contest.Platform,
        timeLimits: ContestTimePrefs.Limits,
        settings: ContestsSettingsDataStore
    ): List<Contest> {
        require(platform in supportedPlatforms)
        return loadContests(platform, timeLimits, settings).filterWith(timeLimits)
    }
}

abstract class ContestsLoaderMultiple(
    supportedPlatforms: Set<Contest.Platform>,
    type: ContestsLoaders
): ContestsLoader(supportedPlatforms, type) {

    protected abstract suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        timeLimits: ContestTimePrefs.Limits,
        settings: ContestsSettingsDataStore
    ): List<Contest>

    suspend fun getContests(
        platforms: Collection<Contest.Platform>,
        timeLimits: ContestTimePrefs.Limits,
        settings: ContestsSettingsDataStore
    ): List<Contest> {
        if (platforms.isEmpty()) return emptyList()
        require(supportedPlatforms.containsAll(platforms))
        return loadContests(platforms.toSet(), timeLimits, settings).filterWith(timeLimits)
    }

    final override suspend fun loadContests(
        platform: Contest.Platform,
        timeLimits: ContestTimePrefs.Limits,
        settings: ContestsSettingsDataStore
    ) = loadContests(platforms = setOf(platform), timeLimits = timeLimits, settings = settings)
}

private fun List<Contest>.filterWith(timeLimits: ContestTimePrefs.Limits) =
    filter { contest ->
        contest.duration <= timeLimits.maxDuration
        &&
        contest.startTime <= timeLimits.maxStartTime
        &&
        contest.endTime >= timeLimits.minEndTime
    }