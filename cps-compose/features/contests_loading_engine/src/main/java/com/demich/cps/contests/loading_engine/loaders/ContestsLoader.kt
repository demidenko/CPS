package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaders


abstract class ContestsLoader(val type: ContestsLoaders) {
    protected open suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return loadContests(platform = platform).filterWith(dateConstraints)
    }

    protected open suspend fun loadContests(
        platform: Contest.Platform
    ): List<Contest> {
        return loadContests(
            platform = platform,
            dateConstraints = ContestDateConstraints()
        )
    }

    suspend fun getContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        require(platform in type.supportedPlatforms)
        return loadContests(
            platform = platform,
            dateConstraints = dateConstraints
        ).apply {
            require(all { it.platform == platform })
        }
    }
}

abstract class ContestsLoaderMultiple(type: ContestsLoaders): ContestsLoader(type) {

    protected open suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return loadContests(platforms = platforms).filterWith(dateConstraints)
    }

    protected open suspend fun loadContests(
        platforms: Set<Contest.Platform>
    ): List<Contest> {
        return loadContests(
            platforms = platforms,
            dateConstraints = ContestDateConstraints()
        )
    }

    suspend fun getContests(
        platforms: Collection<Contest.Platform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        if (platforms.isEmpty()) return emptyList()
        require(type.supportedPlatforms.containsAll(platforms))
        val setOfPlatforms = platforms.toSet()
        return loadContests(
            platforms = setOfPlatforms,
            dateConstraints = dateConstraints
        ).apply {
            require(all { it.platform in setOfPlatforms })
        }
    }

    final override suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ) = loadContests(platforms = setOf(platform), dateConstraints = dateConstraints)
}

private fun List<Contest>.filterWith(dateConstraints: ContestDateConstraints) =
    filter { contest -> dateConstraints.check(startTime = contest.startTime, duration = contest.duration) }
