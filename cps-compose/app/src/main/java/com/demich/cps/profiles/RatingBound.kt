package com.demich.cps.profiles

import kotlin.time.Instant

data class RatingBound(
    val ratingColor: RatingColor,
    val upperBound: Int
)

infix fun RatingColor.until(rating: Int): RatingBound =
    RatingBound(ratingColor = this, upperBound = rating)

class RatingBounds(
    source: Collection<RatingBound>
): AbstractList<RatingBound>() {
    private val bounds = source.sortedBy { it.upperBound }
        .also {
            for (i in 1 until it.size) {
                check(it[i-1].ratingColor < it[i].ratingColor) {
                    "colors must be strictly increasing but: ${it.joinToString { it.ratingColor.name }}"
                }
            }
        }

    constructor(vararg bounds: RatingBound): this(bounds.asList())

    override val size get() = bounds.size

    override fun get(index: Int) = bounds[index]
}

class ArchiveRatingBounds(
    val bounds: RatingBounds,
    val endTime: Instant
)

infix fun RatingBounds.until(endTime: Instant) =
    ArchiveRatingBounds(bounds = this, endTime = endTime)