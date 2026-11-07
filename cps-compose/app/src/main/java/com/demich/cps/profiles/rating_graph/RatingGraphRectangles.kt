package com.demich.cps.profiles.rating_graph

import androidx.compose.runtime.Immutable
import com.demich.cps.profiles.RatingBounds
import com.demich.cps.profiles.RatingBoundsHistory
import com.demich.cps.profiles.RatingColorTag
import com.demich.cps.utils.forEachRangeEqualBy
import kotlin.time.Instant

@Immutable
internal class RatingGraphRectangles(
    history: RatingBoundsHistory
) {
    //point is upperBound (endTime, ratingUpperBound)
    private val upperBounds: List<Pair<GraphPoint, RatingColorTag>> = buildList {
        fun addBounds(x: Instant, bounds: RatingBounds) {
            bounds.forEach {
                add(GraphPoint(x = x, y = it.upperBound) to it.ratingColor)
            }
            add(GraphPoint(x = x, y = Int.MAX_VALUE) to RatingColorTag.RED)
        }

        history.archived
            .sortedBy { it.endTime }
            .forEach {
                addBounds(x = it.endTime, bounds = it.bounds)
            }

        addBounds(x = Instant.DISTANT_FUTURE, bounds = history.current)
    }.apply {
        check(isSortedWith(compareBy({ it.first.x }, { it.first.y })))
    }

    fun getRatingColor(point: GraphPoint): RatingColorTag =
        upperBounds.first { (val r = first) -> point.x < r.x && point.y < r.y }.second

    inline fun forEachUpperBound(block: (GraphPoint, RatingColorTag) -> Unit) =
        upperBounds.asReversed().forEach { block(it.first, it.second) }

    private val rectangles: List<Triple<GraphPoint, GraphPoint, RatingColorTag>> = buildList {
        var prevX: Instant = Instant.DISTANT_PAST
        upperBounds.forEachRangeEqualBy(selector = { it.first.x }) { l, r ->
            var prevY: Int = Int.MIN_VALUE
            upperBounds.subList(l, r).forEach { [point, ratingColor] ->
                add(Triple(GraphPoint(prevX, prevY), point, ratingColor))
                prevY = point.y
            }
            prevX = upperBounds[l].first.x
        }
    }

    inline fun forEachRect(block: (GraphPoint, GraphPoint, RatingColorTag) -> Unit) {
        rectangles.forEach { block(it.first, it.second, it.third) }
    }
}
