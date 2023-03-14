package com.demich.cps.contests

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import com.demich.cps.room.ContestsListDao
import com.demich.cps.utils.ComparablePair
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Entity(
    tableName = ContestsListDao.contestsListTableName,
    primaryKeys = ["platform", "id"]
)
@Immutable
data class Contest (
    val platform: Platform,
    val id: String,
    val title: String,
    val startTime: Instant,
    val durationSeconds: Long,
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
        durationSeconds = (endTime - startTime).inWholeSeconds,
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
        val platforms: List<Platform> by lazy { Platform.values().toList() }
        val platformsExceptUnknown: List<Platform> by lazy { Platform.values().filter { it != Platform.unknown } }

        fun getComparator(currentTime: Instant) = compareBy<Contest> { contest ->
            when(contest.getPhase(currentTime)) {
                Phase.BEFORE -> ComparablePair(1, contest.startTime.epochSeconds)
                Phase.RUNNING -> ComparablePair(0, contest.endTime.epochSeconds)
                Phase.FINISHED -> ComparablePair(2, -contest.endTime.epochSeconds)
            }
        }.thenBy { it.durationSeconds }.thenBy { it.platform }.thenBy { it.id }
    }
}