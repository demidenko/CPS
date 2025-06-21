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
        when (this) {
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
    ADMIN
}

@Serializable
data class CodeforcesUser(
    // Codeforces user handle
    val handle: String,

    val rating: Int? = null,

    // User contribution
    val contribution: Int,

    // Time, when user was last seen online, in unix format
    @SerialName("lastOnlineTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val lastOnlineTime: Instant = Instant.DISTANT_PAST
)

@Serializable
data class CodeforcesContest(
    val id: Int,

    // Localized
    val name: String,

    val phase: CodeforcesContestPhase,

    // Scoring system used for the contest
    val type: CodeforcesContestType,

    // Duration of the contest in seconds
    @SerialName("durationSeconds")
    @Serializable(with = DurationAsSecondsSerializer::class)
    val duration: Duration,

    // Contest start time in unix format. Can be absent.
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
        // Party place in the contest
        val rank: Int,

        // Total amount of points, scored by the party
        val points: Double,

        // Party that took a corresponding place in the contest
        val party: CodeforcesContestParticipant,

        // Party results for each problem. Order of the problems is the same as in "problems" field of the returned object
        val problemResults: List<CodeforcesProblemResult>
    )

    @Serializable
    data class CodeforcesContestParticipant(
        val participantType: CodeforcesParticipationType,

        // Members of the party
        val members: List<CodeforcesUser>
    )
}

@Serializable
data class CodeforcesProblem(
    // Localized
    val name: String,

    // Usually, a letter or letter with digit(s) indicating the problem index in a contest
    val index: String,

    // Id of the contest, containing the problem. Can be absent.
    val contestId: Int = -1
) {
    val problemId: String get() = "$contestId$index"
}

@Serializable
data class CodeforcesProblemResult(
    val points: Double,

    // If type is PRELIMINARY then points can decrease (if, for example, solution will fail during system test). Otherwise, party can only increase points for this problem by submitting better solutions
    val type: CodeforcesProblemStatus,

    // Number of incorrect submissions
    val rejectedAttemptCount: Int
)

@Serializable
data class CodeforcesSubmission(
    // Can be absent.
    val contestId: Int = -1,

    val problem: CodeforcesProblem,

    val author: CodeforcesContestStandings.CodeforcesContestParticipant,

    // Can be absent.
    val verdict: CodeforcesProblemVerdict = CodeforcesProblemVerdict.WAITING,

    // Number of passed tests
    val passedTestCount: Int,

    val id: Long,

    // Time, when submission was created, in unix-format
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,

    // Testset used for judging the submission
    val testset: CodeforcesTestset
)

@Serializable
data class CodeforcesBlogEntry(
    val id: Int,

    // Localized
    val title: String,

    // Author user handle
    val authorHandle: String,

    // Time, when blog entry was created, in unix format
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,

    val rating: Int,

    val commentsCount: Int? = null,

    val authorColorTag: CodeforcesColorTag = CodeforcesColorTag.BLACK
)

@Serializable
data class CodeforcesRatingChange(
    val contestId: Int,

    // Localized
    val contestName: String,

    // Codeforces user handle
    val handle: String,

    // Place of the user in the contest. This field contains user rank on the moment of rating update. If afterwards rank changes (e.g. someone get disqualified), this field will not be update and will contain old rank
    val rank: Int,

    // User rating before the contest
    val oldRating: Int,

    // User rating after the contest
    val newRating: Int,

    // Time, when rating for the contest was update, in unix-format
    @SerialName("ratingUpdateTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val ratingUpdateTime: Instant
)

@Serializable
data class CodeforcesComment(
    val id: Long,

    // Time, when comment was created, in unix format
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,

    val commentatorHandle: String,

    @SerialName("text")
    val html: String,

    val rating: Int,

    val commentatorHandleColorTag: CodeforcesColorTag = CodeforcesColorTag.BLACK
)

@Serializable
data class CodeforcesRecentAction(
    // Action time, in unix format
    @SerialName("timeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val time: Instant,

    // In short form. Can be absent.
    val blogEntry: CodeforcesBlogEntry? = null,

    // Can be absent.
    val comment: CodeforcesComment? = null
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