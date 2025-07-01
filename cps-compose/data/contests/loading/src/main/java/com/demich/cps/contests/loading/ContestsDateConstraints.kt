package com.demich.cps.contests.loading

import kotlinx.datetime.Instant
import kotlin.time.Duration

data class ContestDateConstraints(
    val maxStartTime: Instant = Instant.DISTANT_FUTURE,
    val minEndTime: Instant = Instant.DISTANT_PAST,
    val maxDuration: Duration = Duration.INFINITE
) {
    fun check(startTime: Instant, duration: Duration): Boolean {
        return duration <= maxDuration && startTime <= maxStartTime && startTime + duration >= minEndTime
    }
}