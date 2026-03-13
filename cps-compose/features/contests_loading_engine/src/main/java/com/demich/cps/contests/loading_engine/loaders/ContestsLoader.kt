package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.kotlin_stdlib_boost.toEnumSet


abstract class ContestsLoader {
    abstract val fetchSource: ContestsFetchSource

    protected open suspend fun getContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return getContests(platform = platform).filterBy(dateConstraints)
    }

    protected open suspend fun getContests(
        platform: Contest.Platform
    ): List<Contest> {
        return getContests(
            platform = platform,
            dateConstraints = ContestDateConstraints()
        )
    }

    suspend fun fetchContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        require(platform in fetchSource.supportedPlatforms)
        return getContests(
            platform = platform,
            dateConstraints = dateConstraints
        ).apply {
            check(all { it.platform == platform })
        }
    }
}

abstract class ContestsLoaderMultiple: ContestsLoader() {
    protected open suspend fun getContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return getContests(platforms = platforms).filterBy(dateConstraints)
    }

    protected open suspend fun getContests(
        platforms: Set<Contest.Platform>
    ): List<Contest> {
        return getContests(
            platforms = platforms,
            dateConstraints = ContestDateConstraints()
        )
    }

    suspend fun fetchContests(
        platforms: Collection<Contest.Platform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        if (platforms.isEmpty()) return emptyList()
        require(fetchSource.supportedPlatforms.containsAll(platforms))
        val platforms = platforms.toEnumSet()
        return getContests(
            platforms = platforms,
            dateConstraints = dateConstraints
        ).apply {
            check(all { it.platform in platforms })
        }
    }

    final override suspend fun getContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ) = getContests(platforms = setOf(platform), dateConstraints = dateConstraints)
}

internal fun ContestDateConstraints.check(contest: Contest): Boolean =
    check(startTime = contest.startTime, duration = contest.duration)

private fun List<Contest>.filterBy(dateConstraints: ContestDateConstraints): List<Contest> =
    if (all { dateConstraints.check(it) }) this
    else filter { dateConstraints.check(it) }

