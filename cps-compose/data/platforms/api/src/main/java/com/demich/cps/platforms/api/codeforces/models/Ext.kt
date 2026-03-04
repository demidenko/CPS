package com.demich.cps.platforms.api.codeforces.models

fun CodeforcesParticipationType.isContestantType(): Boolean =
    this == CONTESTANT || this == OUT_OF_COMPETITION

fun CodeforcesContestStandings.CodeforcesContestParticipant.isContestant(): Boolean =
    participantType.isContestantType()

fun CodeforcesContestPhase.isSystemTestOrFinished() =
    this == SYSTEM_TEST || this == FINISHED

val CodeforcesContestPhase.title
    get() = when (this) {
        PENDING_SYSTEM_TEST -> "PENDING SYSTEM TESTING"
        SYSTEM_TEST -> "SYSTEM TESTING"
        else -> name
    }

fun CodeforcesProblemVerdict.isResult(): Boolean =
    when (this) {
        WAITING, TESTING, SKIPPED -> false
        else -> true
    }

val CodeforcesProblem.problemId: String
    get() = "$contestId$index"