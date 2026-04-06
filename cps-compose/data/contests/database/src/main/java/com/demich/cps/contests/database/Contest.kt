package com.demich.cps.contests.database

import androidx.room.Entity
import kotlin.time.Duration
import kotlin.time.Instant

@Entity(
    tableName = contestsTableName,
    primaryKeys = ["platform", "id"]
)
data class Contest (
    val platform: ContestPlatform,
    val id: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val link: String? = null,
    val host: String? = null
) {
    val eventDuration: Duration get() = endTime - startTime

    fun phaseAt(time: Instant): Phase = when {
        time >= endTime -> FINISHED
        time < startTime -> UPCOMING
        else -> RUNNING
    }

    constructor(
        platform: ContestPlatform,
        id: String,
        title: String,
        startTime: Instant,
        endTime: Instant,
        link: String? = null,
        host: String? = null
    ): this(
        platform = platform,
        id = id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        duration = endTime - startTime,
        link = link,
        host = host
    )

    constructor(
        platform: ContestPlatform,
        id: String,
        title: String,
        startTime: Instant,
        duration: Duration,
        link: String? = null,
        host: String? = null
    ): this(
        platform = platform,
        id = id,
        title = title,
        startTime = startTime,
        endTime = startTime + duration,
        duration = duration,
        link = link,
        host = host
    )

    enum class Phase {
        UPCOMING,
        RUNNING,
        FINISHED
    }

    companion object {
        val platformsExceptUnknown: List<ContestPlatform> = ContestPlatform.entries - ContestPlatform.unknown

        fun comparatorAt(time: Instant) = Comparator<Contest> { c1, c2 ->
            val phase1 = c1.phaseAt(time)
            val phase2 = c2.phaseAt(time)
            if (phase1 != phase2) {
                compareValuesBy(phase1, phase2) {
                    when (it) {
                        FINISHED -> 0
                        RUNNING -> 1
                        UPCOMING -> 2
                    }
                }
            } else {
                when (phase1) {
                    UPCOMING -> compareValues(c1.startTime, c2.startTime)
                    RUNNING -> compareValues(c1.endTime, c2.endTime)
                    FINISHED -> -compareValues(c1.endTime, c2.endTime)
                }
            }
        }.thenBy { it.platform }.thenBy { it.id }.thenBy { it.duration }
    }
}