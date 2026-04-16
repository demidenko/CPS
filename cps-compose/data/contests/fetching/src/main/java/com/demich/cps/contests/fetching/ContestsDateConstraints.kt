package com.demich.cps.contests.fetching

import kotlin.time.Duration
import kotlin.time.Instant

data class ContestDateConstraints(
    val maxStartTime: Instant = Instant.DISTANT_FUTURE,
    val minEndTime: Instant = Instant.DISTANT_PAST,
    val maxDuration: Duration = Duration.INFINITE
) {
    fun check(startTime: Instant, duration: Duration): Boolean {
        return duration <= maxDuration && startTime <= maxStartTime && startTime + duration >= minEndTime
    }
}