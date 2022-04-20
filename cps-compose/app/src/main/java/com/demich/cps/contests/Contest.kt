package com.demich.cps.contests

import androidx.compose.runtime.Immutable
import com.demich.cps.utils.CListUtils
import com.demich.cps.utils.ClistContest
import com.demich.cps.utils.ComparablePair
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Immutable //TODO: wtf this MAKE NOT SENSE?? (because of Instant field?)
data class Contest (
    val platform: Platform,
    val id: String,
    val title: String,
    val startTime: Instant,
    private val durationSeconds: Long,
    val link: String? = null
) {
    val compositeId get() = platform to id

    val duration: Duration get() = durationSeconds.seconds
    val endTime: Instant get() = startTime + duration

    fun getPhase(currentTime: Instant): Phase {
        if (currentTime >= endTime) return Phase.FINISHED
        if (currentTime < startTime) return Phase.BEFORE
        return Phase.RUNNING
    }

    constructor(contest: ClistContest): this(
        contest = contest,
        platform = getPlatforms()
            .find { CListUtils.getClistApiResourceId(it) == contest.resource_id }
            ?: Platform.unknown
    )
    private constructor(contest: ClistContest, platform: Platform): this(
        platform = platform,
        id = CListUtils.extractContestId(contest, platform),
        title = contest.event,
        startTime = Instant.parse(contest.start+"Z"),
        durationSeconds = (Instant.parse(contest.end+"Z") - Instant.parse(contest.start+"Z")).inWholeSeconds,
        link = contest.href
    )

    enum class Phase {
        BEFORE,
        RUNNING,
        FINISHED
    }

    enum class Platform {
        unknown,
        codeforces,
        atcoder,
        codechef,
        topcoder,
        dmoj,
        google
        ;
    }

    companion object {
        fun getPlatforms(): List<Platform> = Platform.values().filter { it != Platform.unknown }

        fun getComparator(currentTime: Instant) = compareBy<Contest> { contest ->
            when(contest.getPhase(currentTime)) {
                Phase.BEFORE -> ComparablePair(1, contest.startTime.epochSeconds)
                Phase.RUNNING -> ComparablePair(0, contest.endTime.epochSeconds)
                Phase.FINISHED -> ComparablePair(2, -contest.endTime.epochSeconds)
            }
        }.thenBy { it.durationSeconds }.thenBy { it.platform }.thenBy { it.id }
    }
}