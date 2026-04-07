package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.kotlin_stdlib_boost.toEnumSet

sealed interface ContestsFetcher {
    val fetchSource: ContestsFetchSource
}

abstract class ContestsSinglePlatformFetcher: ContestsFetcher {

    abstract val platform: ContestPlatform

    protected open suspend fun getContests(
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return getContests().filterBy(dateConstraints)
    }

    protected open suspend fun getContests(): List<Contest> {
        return getContests(dateConstraints = ContestDateConstraints())
    }

    suspend fun fetchContests(
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        require(platform in fetchSource.platforms) { "$fetchSource does not supports $platform" }
        return getContests(dateConstraints = dateConstraints)
    }
}

abstract class ContestsMultiplatformFetcher: ContestsFetcher {

    protected open suspend fun getContests(
        platforms: Set<ContestPlatform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        return getContests(platforms = platforms).filterBy(dateConstraints)
    }

    protected open suspend fun getContests(
        platforms: Set<ContestPlatform>
    ): List<Contest> {
        return getContests(
            platforms = platforms,
            dateConstraints = ContestDateConstraints()
        )
    }

    suspend fun fetchContests(
        platforms: Collection<ContestPlatform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        if (platforms.isEmpty()) return emptyList()
        require(fetchSource.platforms.containsAll(platforms)) { "$fetchSource does not supports $platforms" }
        val platforms = platforms.toEnumSet()
        return getContests(
            platforms = platforms,
            dateConstraints = dateConstraints
        )
    }
}

internal fun ContestDateConstraints.check(contest: Contest): Boolean =
    check(startTime = contest.startTime, duration = contest.duration)

private fun List<Contest>.filterBy(dateConstraints: ContestDateConstraints): List<Contest> =
    if (all { dateConstraints.check(it) }) this
    else filter { dateConstraints.check(it) }

