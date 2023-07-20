package com.demich.cps.contests.database

import androidx.room.Entity
import kotlinx.datetime.Instant
import kotlin.time.Duration

@Entity(
    tableName = contestsTableName,
    primaryKeys = ["platform", "id"]
)
data class Contest (
    val platform: Platform,
    val id: String,
    val title: String,
    val startTime: Instant,
    val duration: Duration,
    val link: String? = null
) {
    val compositeId get() = platform to id

    val endTime: Instant get() = startTime + duration

    fun getPhase(currentTime: Instant): Phase {
        if (currentTime >= endTime) return Phase.FINISHED
        if (currentTime < startTime) return Phase.BEFORE
        return Phase.RUNNING
    }

    constructor(
        platform: Platform,
        id: String,
        title: String,
        startTime: Instant,
        endTime: Instant,
        link: String? = null
    ): this(
        platform = platform,
        id = id,
        title = title,
        startTime = startTime,
        duration = endTime - startTime,
        link = link
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
        val platforms: List<Platform> get() = Platform.entries
        val platformsExceptUnknown: List<Platform> = platforms - Platform.unknown

        fun getComparator(currentTime: Instant) = Comparator<Contest> { c1, c2 ->
            val phase1 = c1.getPhase(currentTime)
            val phase2 = c2.getPhase(currentTime)
            if (phase1 != phase2) compareValuesBy(phase1, phase2) {
                when (it) {
                    Phase.RUNNING -> 0
                    Phase.BEFORE -> 1
                    Phase.FINISHED -> 2
                }
            }
            else when (phase1) {
                Phase.RUNNING -> compareValues(c1.endTime, c2.endTime)
                Phase.BEFORE -> compareValues(c1.startTime, c2.startTime)
                Phase.FINISHED -> -compareValues(c1.endTime, c2.endTime)
            }
        }.thenBy { it.duration }.thenBy { it.platform }.thenBy { it.id }
    }
}