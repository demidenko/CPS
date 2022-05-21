package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestDateConstraints
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import java.net.SocketException
import java.net.UnknownHostException


enum class ContestsLoaders(val supportedPlatforms: Set<Contest.Platform>) {
    clist(Contest.platforms.toSet()),
    codeforces(Contest.Platform.codeforces),
    atcoder(Contest.Platform.atcoder),
    dmoj(Contest.Platform.dmoj)
    ;

    constructor(platform: Contest.Platform): this(setOf(platform))
}

abstract class ContestsLoader(val type: ContestsLoaders) {
    abstract suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints.Current
    ): List<Contest>

    suspend fun getContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints.Current
    ): List<Contest> {
        require(platform in type.supportedPlatforms)
        return loadContests(
            platform = platform,
            dateConstraints = dateConstraints
        ).filterWith(dateConstraints)
    }
}

abstract class ContestsLoaderMultiple(type: ContestsLoaders): ContestsLoader(type) {

    protected abstract suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints.Current
    ): List<Contest>

    suspend fun getContests(
        platforms: Collection<Contest.Platform>,
        dateConstraints: ContestDateConstraints.Current
    ): List<Contest> {
        if (platforms.isEmpty()) return emptyList()
        require(type.supportedPlatforms.containsAll(platforms))
        return loadContests(
            platforms = platforms.toSet(),
            dateConstraints = dateConstraints
        ).filterWith(dateConstraints)
    }

    final override suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints.Current
    ) = loadContests(platforms = setOf(platform), dateConstraints = dateConstraints)
}

private fun List<Contest>.filterWith(dateConstraints: ContestDateConstraints.Current) =
    filter { contest ->
        contest.duration <= dateConstraints.maxDuration
        &&
        contest.startTime <= dateConstraints.maxStartTime
        &&
        contest.endTime >= dateConstraints.minEndTime
    }

fun makeCombinedMessage(
    errors: List<Pair<ContestsLoaders, Throwable>>,
    developEnabled: Boolean
): String {
    if (errors.isEmpty()) return ""
    val g = errors.groupBy(
        valueTransform = { it.first },
        keySelector = { (_, e) ->
            when {
                e is UnknownHostException || e is SocketException || e is SocketTimeoutException
                    -> "Connection failed"
                e is ClientRequestException && e.response.status == HttpStatusCode.Unauthorized
                    -> "Unauthorized"
                e is ClientRequestException && e.response.status == HttpStatusCode.TooManyRequests
                    -> "Too many requests"
                else -> {
                    if (developEnabled) "$e" else "Some kind of error..."
                }
            }
        }
    )

    return g.entries.joinToString { (msg, list) -> "${list.distinct()}: $msg" }
}