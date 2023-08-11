package com.demich.cps.contests.loading

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class ContestDateBaseConstraints(
    val maxDuration: Duration,
    val nowToStartTimeMaxDuration: Duration,
    val endTimeToNowMaxDuration: Duration,
) {
    fun at(currentTime: Instant) = ContestDateConstraints(
        maxStartTime = currentTime + nowToStartTimeMaxDuration,
        minEndTime = currentTime - endTimeToNowMaxDuration,
        maxDuration = maxDuration
    )
}

data class ContestDateConstraints(
    val maxStartTime: Instant = Instant.DISTANT_FUTURE,
    val minEndTime: Instant = Instant.DISTANT_PAST,
    val maxDuration: Duration = Duration.INFINITE
) {
    fun check(startTime: Instant, duration: Duration): Boolean {
        return duration <= maxDuration && startTime <= maxStartTime && startTime + duration >= minEndTime
    }
}