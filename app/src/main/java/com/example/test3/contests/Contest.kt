package com.example.test3.contests

import androidx.room.Entity
import com.example.test3.R
import com.example.test3.room.contestsListTableName
import com.example.test3.utils.*

@Entity(
    tableName = contestsListTableName,
    primaryKeys = ["platform", "id"]
)
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

    fun getCompositeId() = platform to id

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
                Platform.atcoder -> {
                    contest.href.removePrefix("http://").removePrefix("https://").removePrefix("atcoder.jp/contests/")
                }
                Platform.codechef -> {
                    contest.href.removePrefix("http://").removePrefix("https://").removePrefix("www.codechef.com/")
                }
                else -> null
            } ?: contest.id.toString()
        }
    }
}