package com.example.test3.contests

import com.example.test3.utils.CodeforcesContest
import com.example.test3.utils.CodeforcesURLFactory

data class Contest (
    val platform: Platform,
    val id: String,
    val title: String,
    val startTimeSeconds: Long,
    val durationSeconds: Long,
    val link: String? = null
) {

    val endTimeSeconds: Long get() = startTimeSeconds + durationSeconds

    fun getPhase(currentTimesSeconds: Long): Phase {
        if(currentTimesSeconds < startTimeSeconds) return Phase.BEFORE
        if(currentTimesSeconds >= endTimeSeconds) return Phase.FINISHED
        return Phase.RUNNING
    }

    constructor(contest: CodeforcesContest): this(
        Platform.codeforces,
        contest.id.toString(),
        contest.name,
        contest.startTimeSeconds,
        contest.durationSeconds,
        link = CodeforcesURLFactory.contestOuter(contest.id)
    )

    enum class Phase {
        BEFORE,
        RUNNING,
        FINISHED
    }

    enum class Platform {
        unknown,
        codeforces
    }
}