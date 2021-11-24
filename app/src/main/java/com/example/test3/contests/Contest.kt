package com.example.test3.contests

import androidx.room.Entity
import com.example.test3.R
import com.example.test3.room.contestsListTableName
import com.example.test3.utils.ClistContest
import com.example.test3.utils.CodeforcesContest
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.ComparablePair
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

@Entity(
    tableName = contestsListTableName,
    primaryKeys = ["platform", "id"]
)
data class Contest (
    val platform: Platform,
    val id: String,
    val title: String,
    val startTime: Instant,
    val durationSeconds: Long,
    val link: String? = null
) {

    val endTime: Instant get() = startTime + durationSeconds.seconds

    fun getPhase(currentTime: Instant) = getPhase(currentTime, startTime, endTime)

    fun getCompositeId() = platform to id

    constructor(contest: CodeforcesContest): this(
        Platform.codeforces,
        contest.id.toString(),
        contest.name,
        contest.startTime,
        contest.durationSeconds,
        link = CodeforcesURLFactory.contestOuter(contest.id)
    )

    constructor(contest: ClistContest): this(contest, contest.getPlatform())
    constructor(contest: ClistContest, platform: Platform): this(
        platform,
        extractContestId(contest, platform),
        contest.event,
        Instant.parse(contest.start+"Z"),
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
        atcoder,
        topcoder,
        codechef,
        google
        ;

        fun getIcon(): Int {
            return when(this) {
                codeforces -> R.drawable.ic_logo_codeforces
                atcoder -> R.drawable.ic_logo_atcoder
                topcoder -> R.drawable.ic_logo_topcoder
                codechef -> R.drawable.ic_logo_codechef
                google -> R.drawable.ic_logo_google
                else -> R.drawable.ic_cup
            }
        }
        
        companion object {
            fun getAll(): List<Platform> = values().filter { it != unknown }
        }
    }

    companion object {
        fun getPhase(currentTime: Instant, startTime: Instant, endTime: Instant): Phase {
            if(currentTime < startTime) return Phase.BEFORE
            if(currentTime >= endTime) return Phase.FINISHED
            return Phase.RUNNING
        }

        fun getComparator(currentTime: Instant) = compareBy<Contest> {
                when(it.getPhase(currentTime)) {
                    Phase.BEFORE -> ComparablePair(1, it.startTime.epochSeconds)
                    Phase.RUNNING -> ComparablePair(0, it.endTime.epochSeconds)
                    Phase.FINISHED -> ComparablePair(2, -it.endTime.epochSeconds)
                }
            }.thenBy { it.durationSeconds }.thenBy { it.platform }.thenBy { it.id }

        private fun String.removePrefixHttp() = removePrefix("http://").removePrefix("https://")
        fun extractContestId(contest: ClistContest, platform: Platform): String {
            return when (platform) {
                Platform.codeforces -> {
                    contest.href.removePrefixHttp().removePrefix("codeforces.com/contests/")
                        .toIntOrNull()?.toString()
                }
                Platform.atcoder -> {
                    contest.href.removePrefixHttp().removePrefix("atcoder.jp/contests/")
                }
                Platform.codechef -> {
                    contest.href.removePrefixHttp().removePrefix("www.codechef.com/")
                }
                else -> null
            } ?: contest.id.toString()
        }
    }
}