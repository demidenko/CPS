package com.demich.cps.profiles

data class RatingColorBound(
    val ratingColor: RatingColor,
    val ratingUpperBound: Int
)

infix fun RatingColor.until(rating: Int): RatingColorBound =
    RatingColorBound(ratingColor = this, ratingUpperBound = rating)

class RatingBounds(
    source: Collection<RatingColorBound>
): AbstractList<RatingColorBound>() {
    private val bounds = source.sortedBy { it.ratingUpperBound }
        .also {
            for (i in 1 until it.size) {
                check(it[i-1].ratingColor < it[i].ratingColor) {
                    "colors must be strictly increasing but: ${it.joinToString { it.ratingColor.name }}"
                }
            }
        }

    constructor(vararg bound: RatingColorBound): this(bound.asList())

    override val size get() = bounds.size

    override fun get(index: Int) = bounds[index]
}