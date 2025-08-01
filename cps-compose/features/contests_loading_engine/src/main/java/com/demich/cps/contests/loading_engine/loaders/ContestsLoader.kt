package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.kotlin_stdlib_boost.toEnumSet


abstract class ContestsLoader {
    abstract val type: ContestsLoaderType

    protected open suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return loadContests(platform = platform).filterBy(dateConstraints)
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
            check(all { it.platform == platform })
        }
    }
}

abstract class ContestsLoaderMultiple: ContestsLoader() {

    protected open suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return loadContests(platforms = platforms).filterBy(dateConstraints)
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
        val platforms = platforms.toEnumSet()
        return loadContests(
            platforms = platforms,
            dateConstraints = dateConstraints
        ).apply {
            check(all { it.platform in platforms })
        }
    }

    final override suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ) = loadContests(platforms = setOf(platform), dateConstraints = dateConstraints)
}

internal fun ContestDateConstraints.check(contest: Contest): Boolean =
    check(startTime = contest.startTime, duration = contest.duration)

private fun List<Contest>.filterBy(dateConstraints: ContestDateConstraints): List<Contest> =
    if (all { dateConstraints.check(it) }) this
    else filter { dateConstraints.check(it) }

