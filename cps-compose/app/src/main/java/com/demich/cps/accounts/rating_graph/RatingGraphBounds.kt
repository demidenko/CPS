package com.demich.cps.accounts.rating_graph

import com.demich.cps.accounts.managers.RatingChange
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

internal data class RatingGraphBounds(
    val minRating: Int,
    val maxRating: Int,
    val startTime: Instant,
    val endTime: Instant
) {
    init {
        require(minRating <= maxRating)
        require(startTime <= endTime)
    }
}

private fun createBounds(
    ratingChanges: List<RatingChange>,
    startTime: Instant = ratingChanges.first().date,
    endTime: Instant = ratingChanges.last().date
) = RatingGraphBounds(
    minRating = ratingChanges.minOf { it.rating },
    maxRating = ratingChanges.maxOf { it.rating },
    startTime = startTime,
    endTime = endTime
)

internal fun createBounds(
    ratingChanges: List<RatingChange>,
    filterType: RatingFilterType,
    now: Instant
) = when (filterType) {
    RatingFilterType.ALL -> createBounds(ratingChanges)
    RatingFilterType.LAST_10 -> createBounds(ratingChanges.takeLast(10))
    RatingFilterType.LAST_MONTH, RatingFilterType.LAST_YEAR -> {
        val startTime = now - (if (filterType == RatingFilterType.LAST_MONTH) 30.days else 365.days)
        createBounds(
            ratingChanges = ratingChanges.filter { it.date >= startTime },
            startTime = startTime,
            endTime = now
        )
    }
}