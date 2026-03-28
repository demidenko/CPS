package com.demich.cps.platforms.api.codeforces.models

import kotlin.time.Instant

fun CodeforcesParticipationType.isContestantType(): Boolean =
    when (this) {
        CONTESTANT, OUT_OF_COMPETITION -> true
        else -> false
    }

fun CodeforcesContestParticipant.isContestant(): Boolean =
    participantType.isContestantType()

fun CodeforcesContestPhase.isSystemTestOrFinished() =
    when (this) {
        SYSTEM_TEST, FINISHED -> true
        else -> false
    }

val CodeforcesContestPhase.title
    get() = when (this) {
        PENDING_SYSTEM_TEST -> "PENDING SYSTEM TESTING"
        SYSTEM_TEST -> "SYSTEM TESTING"
        else -> name
    }

fun CodeforcesProblemVerdict.isResult(): Boolean =
    when (this) {
        PENDING, TESTING, SKIPPED -> false
        else -> true
    }

val CodeforcesProblem.problemId: String
    get() = "$contestId$index"

val CodeforcesContest.endTime: Instant
    get() = startTime + duration