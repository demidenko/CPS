package com.example.test3.contests

import com.example.test3.utils.*

data class Contest (
    val platform: Platform,
    val id: String,
    val title: String,
    val startTimeSeconds: Long,
    val durationSeconds: Long,
    val link: String? = null
) {

    val endTimeSeconds: Long get() = startTimeSeconds + durationSeconds

    fun getPhase(currentTimesSeconds: Long) = getPhase(currentTimesSeconds, startTimeSeconds, endTimeSeconds)

    constructor(contest: CodeforcesContest): this(
        Platform.codeforces,
        contest.id.toString(),
        contest.name,
        contest.startTimeSeconds,
        contest.durationSeconds,
        link = CodeforcesURLFactory.contestOuter(contest.id)
    )

    constructor(contest: ClistContest): this(contest, contest.getPlatform())
    constructor(contest: ClistContest, platform: Platform): this(
        platform,
        extractContestId(contest, platform),
        contest.event,
        CListAPI.dateToSeconds(contest.start),
        contest.duration,
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
        atcoder
    }

    companion object {
        fun getPhase(currentTimeSeconds: Long, startTimeSeconds: Long, endTimeSeconds: Long): Phase {
            if(currentTimeSeconds < startTimeSeconds) return Phase.BEFORE
            if(currentTimeSeconds >= endTimeSeconds) return Phase.FINISHED
            return Phase.RUNNING
        }

        fun getComparator(currentTimeSeconds: Long) = compareBy<Contest> {
                when(it.getPhase(currentTimeSeconds)) {
                    Phase.BEFORE -> ComparablePair(1, it.startTimeSeconds)
                    Phase.RUNNING -> ComparablePair(0, it.endTimeSeconds)
                    Phase.FINISHED -> ComparablePair(2, -it.endTimeSeconds)
                }
            }.thenBy { it.durationSeconds }.thenBy { it.platform }.thenBy { it.id }

        fun extractContestId(contest: ClistContest, platform: Platform): String {
            return when (platform) {
                Platform.codeforces -> {
                    contest.href.removePrefix("http://").removePrefix("https://").removePrefix("codeforces.com/contests/")
                        .toIntOrNull()?.toString()
                }
                else -> null
            } ?: contest.id.toString()
        }
    }
}