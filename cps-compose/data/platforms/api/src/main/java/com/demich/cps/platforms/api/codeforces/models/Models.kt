package com.demich.cps.platforms.api.codeforces.models

import com.demich.cps.platforms.api.DurationAsSecondsSerializer
import com.demich.cps.platforms.api.InstantAsSecondsSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

enum class CodeforcesLocale {
    EN, RU;

    override fun toString(): String =
        when(this) {
            EN -> "en"
            RU -> "ru"
        }
}

enum class CodeforcesColorTag {
    BLACK,
    GRAY,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    ORANGE,
    RED,
    LEGENDARY,
    ADMIN;

    companion object {
        fun fromRating(rating: Int?): CodeforcesColorTag =
            when {
                rating == null -> BLACK
                rating < 1200 -> GRAY
                rating < 1400 -> GREEN
                rating < 1600 -> CYAN
                rating < 1900 -> BLUE
                rating < 2100 -> VIOLET
                rating < 2400 -> ORANGE
                rating < 3000 -> RED
                else -> LEGENDARY
            }
    }
}

@Serializable
data class CodeforcesUser(
    val handle: String,
    val rating: Int? = null,
    val contribution: Int = 0,
    @SerialName("lastOnlineTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val lastOnlineTime: Instant = Instant.DISTANT_PAST
)

@Serializable
data class CodeforcesContest(
    val id: Int,
    val name: String,
    val phase: CodeforcesContestPhase,
    val type: CodeforcesContestType,
    @SerialName("durationSeconds")
    @Serializable(with = DurationAsSecondsSerializer::class)
    val duration: Duration,
    @SerialName("startTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val startTime: Instant
)

@Serializable
data class CodeforcesContestStandings(
    val contest: CodeforcesContest,
    val problems: List<CodeforcesProblem>,
    val rows: List<CodeforcesContestStandingsRow>
) {
    @Serializable
    data class CodeforcesContestStandingsRow(
        val rank: Int,
        val points: Double,
        val party: CodeforcesContestParticipant,
        val problemResults: List<CodeforcesProblemResult>
    )

    @Serializable
    data class CodeforcesContestParticipant(
        val contestId: Int,
        val participantType: CodeforcesParticipationType,
        val members: List<CodeforcesUser>
    )
}

@Serializable
data class CodeforcesProblem(
    val name: String,
    val index: String,
    val contestId: Int = -1,
    val points: Double = 0.0
) {
    val problemId: String get() = "$contestId$index"
}

@Serializable
data class CodeforcesProblemResult(
    val points: Double,
    val type: CodeforcesProblemStatus,
    val rejectedAttemptCount: Int
)

@Serializable
data class CodeforcesSubmission(
    val contestId: Int,
    val problem: CodeforcesProblem,
    val author: CodeforcesContestStandings.CodeforcesContestParticipant,
    val verdict: CodeforcesProblemVerdict = CodeforcesProblemVerdict.WAITING,
    val passedTestCount: Int,
    val id: Long,
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,
    val testset: CodeforcesTestset
) {
    fun makeVerdict(): String {
        if (verdict == CodeforcesProblemVerdict.OK) return "OK"
        return "${verdict.name} #${passedTestCount+1}"
    }
}

@Serializable
data class CodeforcesBlogEntry(
    val id: Int,
    val title: String,
    val authorHandle: String,
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,
    val rating: Int = 0,
    val commentsCount: Int = 0,
    val authorColorTag: CodeforcesColorTag = CodeforcesColorTag.BLACK
)

@Serializable
data class CodeforcesRatingChange(
    val contestId: Int,
    val contestName: String,
    val handle: String,
    val rank: Int,
    val oldRating: Int,
    val newRating: Int,
    @SerialName("ratingUpdateTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val ratingUpdateTime: Instant
)

@Serializable
data class CodeforcesComment(
    val id: Long,
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,
    val commentatorHandle: String,
    val html: String,
    val rating: Int,
    val commentatorHandleColorTag: CodeforcesColorTag = CodeforcesColorTag.BLACK
)

@Serializable
data class CodeforcesRecentAction(
    @SerialName("timeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val time: Instant,
    val blogEntry: CodeforcesBlogEntry? = null,
    val comment: CodeforcesComment
)

enum class CodeforcesContestPhase {
    UNDEFINED,
    BEFORE,
    CODING,
    PENDING_SYSTEM_TEST,
    SYSTEM_TEST,
    FINISHED;

    val title: String
        get() = when (this) {
            PENDING_SYSTEM_TEST -> "PENDING SYSTEM TESTING"
            SYSTEM_TEST -> "SYSTEM TESTING"
            else -> name
        }

    fun isSystemTestOrFinished() =
        this == SYSTEM_TEST || this == FINISHED
}

enum class CodeforcesContestType {
    UNDEFINED,
    CF, ICPC, IOI
}

enum class CodeforcesParticipationType {
    NOT_PARTICIPATED,
    CONTESTANT, PRACTICE, VIRTUAL, MANAGER, OUT_OF_COMPETITION;

    fun contestParticipant(): Boolean = (this == CONTESTANT || this == OUT_OF_COMPETITION)
}

enum class CodeforcesProblemStatus {
    FINAL, PRELIMINARY
}

enum class CodeforcesProblemVerdict {
    WAITING,
    FAILED, OK, PARTIAL, COMPILATION_ERROR, RUNTIME_ERROR, WRONG_ANSWER, PRESENTATION_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, IDLENESS_LIMIT_EXCEEDED, SECURITY_VIOLATED, CRASHED, INPUT_PREPARATION_CRASHED, CHALLENGED,
    SKIPPED, TESTING, REJECTED
    ;

    fun isResult(): Boolean = (this != WAITING && this != TESTING && this != SKIPPED)
}

enum class CodeforcesTestset {
    SAMPLES, PRETESTS, TESTS, CHALLENGES,
    TESTS1, TESTS2, TESTS3, TESTS4, TESTS5, TESTS6, TESTS7, TESTS8, TESTS9, TESTS10
}