package com.demich.cps.profiles

import kotlin.time.Instant

data class RatingBound(
    val colorTag: RatingColorTag,
    val upperBound: Int
)

infix fun RatingColorTag.until(rating: Int): RatingBound =
    RatingBound(colorTag = this, upperBound = rating)

class RatingBounds(
    source: Collection<RatingBound>
): AbstractList<RatingBound>() {
    private val bounds = source.sortedBy { it.upperBound }
        .also {
            for (i in 1 until it.size) {
                check(it[i-1].colorTag < it[i].colorTag) {
                    "colors must be strictly increasing but: ${it.joinToString { it.colorTag.name }}"
                }
            }
        }

    constructor(vararg bounds: RatingBound): this(bounds.asList())

    override val size get() = bounds.size

    override fun get(index: Int) = bounds[index]

    fun colorTagFor(rating: Int): RatingColorTag =
        bounds
            .firstOrNull { rating < it.upperBound }
            ?.colorTag ?: RED
}

class RatingBoundsArchived(
    val bounds: RatingBounds,
    val endTime: Instant
)

infix fun RatingBounds.until(endTime: Instant) =
    RatingBoundsArchived(bounds = this, endTime = endTime)

interface RatingBoundsHistory {
    val current: RatingBounds

    val archived: List<RatingBoundsArchived>
}