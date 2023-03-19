package com.demich.cps.contests.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.settings.ContestDateConstraints
import com.demich.cps.platforms.api.niceMessage


enum class ContestsLoaders(val supportedPlatforms: Set<Contest.Platform>) {
    clist(Contest.platforms.toSet()),
    codeforces(Contest.Platform.codeforces),
    atcoder(Contest.Platform.atcoder),
    dmoj(Contest.Platform.dmoj)
    ;

    constructor(platform: Contest.Platform): this(setOf(platform))
}

abstract class ContestsLoader(val type: ContestsLoaders) {
    protected open suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints.Current
    ): List<Contest> {
        return loadContests(platform = platform).filterWith(dateConstraints)
    }

    protected open suspend fun loadContests(
        platform: Contest.Platform
    ): List<Contest> {
        return loadContests(
            platform = platform,
            dateConstraints = ContestDateConstraints.Current()
        )
    }

    suspend fun getContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints.Current
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
        dateConstraints: ContestDateConstraints.Current
    ): List<Contest> {
        return loadContests(platforms = platforms).filterWith(dateConstraints)
    }

    protected open suspend fun loadContests(
        platforms: Set<Contest.Platform>
    ): List<Contest> {
        return loadContests(
            platforms = platforms,
            dateConstraints = ContestDateConstraints.Current()
        )
    }

    suspend fun getContests(
        platforms: Collection<Contest.Platform>,
        dateConstraints: ContestDateConstraints.Current
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
        dateConstraints: ContestDateConstraints.Current
    ) = loadContests(platforms = setOf(platform), dateConstraints = dateConstraints)
}

private fun List<Contest>.filterWith(dateConstraints: ContestDateConstraints.Current) =
    filter { contest -> dateConstraints.check(startTime = contest.startTime, duration = contest.duration) }

fun makeCombinedMessage(
    errors: List<Pair<ContestsLoaders, Throwable>>,
    exposeAll: Boolean
): String {
    if (errors.isEmpty()) return ""
    return errors.groupBy(
        keySelector = { (_, e) ->
            e.niceMessage ?: if (exposeAll) "$e" else "Some error..."
        },
        valueTransform = { it.first }
    ).entries.joinToString(separator = "; ") { (msg, list) -> "${list.distinct()}: $msg" }
}