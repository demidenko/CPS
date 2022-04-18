package com.demich.cps.contests

import androidx.compose.runtime.Immutable
import com.demich.cps.utils.ClistContest
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Immutable
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

    //fun getPhase(currentTime: Instant) = getPhase(currentTime, startTime, endTime)

    constructor(contest: ClistContest): this(contest, contest.getPlatform())
    private constructor(contest: ClistContest, platform: Platform): this(
        platform,
        contest.extractContestId(platform),
        contest.event,
        Instant.parse(contest.start+"Z"),
        (Instant.parse(contest.end+"Z") - Instant.parse(contest.start+"Z")).inWholeSeconds,
        link = contest.href
    )



    enum class Platform {
        unknown,
        codeforces,
        atcoder,
        topcoder,
        codechef,
        google,
        dmoj
        ;

        /*fun getIcon(): Int {
            return when(this) {
                codeforces -> R.drawable.ic_logo_codeforces
                atcoder -> R.drawable.ic_logo_atcoder
                topcoder -> R.drawable.ic_logo_topcoder
                codechef -> R.drawable.ic_logo_codechef
                google -> R.drawable.ic_logo_google
                dmoj -> R.drawable.ic_logo_dmoj
                else -> R.drawable.ic_cup
            }
        }*/

        companion object {
            fun getAll(): List<Platform> = values().filter { it != unknown }
        }
    }

}