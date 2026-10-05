package com.demich.cps.profiles.rating_graph

import androidx.compose.runtime.Immutable
import com.demich.cps.profiles.RatingBounds
import com.demich.cps.profiles.RatingColor
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.managers.RatingRevolutionsProvider
import com.demich.cps.utils.forEachRangeEqualBy
import kotlin.time.Instant

@Immutable
internal class RatingGraphRectangles(
    manager: RatedProfileManager<*>
) {
    //point is upperBound (endTime, ratingUpperBound)
    private val upperBounds: List<Pair<GraphPoint, RatingColor>> = buildList {
        fun addBounds(x: Instant, bounds: RatingBounds) {
            bounds.forEach {
                add(GraphPoint(x = x, y = it.upperBound) to it.ratingColor)
            }
            add(GraphPoint(x = x, y = Int.MAX_VALUE) to RatingColor.RED)
        }
        if (manager is RatingRevolutionsProvider) {
            manager.ratingUpperBoundRevolutions()
                .sortedBy { it.endTime }
                .forEach {
                    addBounds(x = it.endTime, bounds = it.bounds)
                }
        }
        addBounds(x = Instant.DISTANT_FUTURE, bounds = manager.ratingsUpperBounds)
    }.apply {
        check(isSortedWith(compareBy({ it.first.x }, { it.first.y })))
    }

    fun getRatingColor(point: GraphPoint): RatingColor =
        upperBounds.first { (r, _) -> point.x < r.x && point.y < r.y }.second

    inline fun forEachUpperBound(block: (GraphPoint, RatingColor) -> Unit) =
        upperBounds.asReversed().forEach { block(it.first, it.second) }

    private val rectangles: List<Triple<GraphPoint, GraphPoint, RatingColor>> = buildList {
        var prevX: Instant = Instant.DISTANT_PAST
        upperBounds.forEachRangeEqualBy(selector = { it.first.x }) { l, r ->
            var prevY: Int = Int.MIN_VALUE
            upperBounds.subList(l, r).forEach { (point, ratingColor) ->
                add(Triple(GraphPoint(prevX, prevY), point, ratingColor))
                prevY = point.y
            }
            prevX = upperBounds[l].first.x
        }
    }

    inline fun forEachRect(block: (GraphPoint, GraphPoint, RatingColor) -> Unit) {
        rectangles.forEach { block(it.first, it.second, it.third) }
    }
}
