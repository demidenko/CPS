package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestTimePrefs


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
        timeLimits: ContestTimePrefs.Limits
    ): List<Contest>

    suspend fun getContests(
        platform: Contest.Platform,
        timeLimits: ContestTimePrefs.Limits
    ): List<Contest> {
        require(platform in supportedPlatforms)
        return loadContests(
            platform = platform,
            timeLimits = timeLimits
        ).filterWith(timeLimits)
    }
}

abstract class ContestsLoaderMultiple(
    supportedPlatforms: Set<Contest.Platform>,
    type: ContestsLoaders
): ContestsLoader(supportedPlatforms, type) {

    protected abstract suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        timeLimits: ContestTimePrefs.Limits
    ): List<Contest>

    suspend fun getContests(
        platforms: Collection<Contest.Platform>,
        timeLimits: ContestTimePrefs.Limits
    ): List<Contest> {
        if (platforms.isEmpty()) return emptyList()
        require(supportedPlatforms.containsAll(platforms))
        return loadContests(
            platforms = platforms.toSet(),
            timeLimits = timeLimits
        ).filterWith(timeLimits)
    }

    final override suspend fun loadContests(
        platform: Contest.Platform,
        timeLimits: ContestTimePrefs.Limits
    ) = loadContests(platforms = setOf(platform), timeLimits = timeLimits)
}

private fun List<Contest>.filterWith(timeLimits: ContestTimePrefs.Limits) =
    filter { contest ->
        contest.duration <= timeLimits.maxDuration
        &&
        contest.startTime <= timeLimits.maxStartTime
        &&
        contest.endTime >= timeLimits.minEndTime
    }

fun makeCombinedMessage(
    errors: List<Pair<ContestsLoaders,Throwable>>,
    developEnabled: Boolean
): String {
    if (errors.isEmpty()) return ""
    val g = errors.groupBy(
        valueTransform = { it.first },
        keySelector = { (loaderType, e) ->
            when {
                e is UnknownHostException || e is SocketException
                    -> "Connection failed"
                e is ClientRequestException && e.response.status == HttpStatusCode.Unauthorized
                    -> "Unauthorized"
                e is ClientRequestException && e.response.status == HttpStatusCode.TooManyRequests
                    -> "Too many requests"
                else -> {
                    if (developEnabled) "$e" else null
                }
            }
        }
    )

    return g.mapNotNull { (msg, list) ->
        if (msg == null) null else msg to list.distinct()
    }.joinToString { (msg, list) -> "$list: $msg" }
}