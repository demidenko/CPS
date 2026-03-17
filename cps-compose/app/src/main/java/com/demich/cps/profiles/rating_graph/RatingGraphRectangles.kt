package com.demich.cps.profiles.rating_graph

import androidx.compose.runtime.Immutable
import com.demich.cps.profiles.HandleColor
import com.demich.cps.profiles.HandleColorBound
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.managers.RatingRevolutionsProvider
import com.demich.cps.utils.forEachRangeEqualBy
import com.demich.kotlin_stdlib_boost.isSortedWith
import kotlin.time.Instant

@Immutable
internal class RatingGraphRectangles(
    manager: RatedProfileManager<*>
) {
    //point is upperBound (endTime, ratingUpperBound)
    private val upperBounds: List<Pair<GraphPoint, HandleColor>> = buildList {
        fun addBounds(x: Instant, bounds: List<HandleColorBound>) {
            bounds.sortedBy { it.ratingUpperBound }.forEach {
                add(GraphPoint(x = x, y = it.ratingUpperBound) to it.handleColor)
            }
            add(GraphPoint(x = x, y = Int.MAX_VALUE) to HandleColor.RED)
        }
        if (manager is RatingRevolutionsProvider) {
            manager.ratingUpperBoundRevolutions
                .sortedBy { it.first }
                .forEach { (endTime, bounds) ->
                    addBounds(x = endTime, bounds = bounds)
                }
        }
        addBounds(x = Instant.DISTANT_FUTURE, bounds = manager.ratingsUpperBounds)
    }.apply {
        check(isSortedWith(compareBy<Pair<GraphPoint, HandleColor>> { it.first.x }.thenBy { it.first.y }))
    }

    fun getHandleColor(point: GraphPoint): HandleColor =
        upperBounds.first { (r, _) -> point.x < r.x && point.y < r.y }.second

    inline fun forEachUpperBound(block: (GraphPoint, HandleColor) -> Unit) =
        upperBounds.asReversed().forEach { block(it.first, it.second) }

    inline fun forEachRect(block: (GraphPoint, GraphPoint, HandleColor) -> Unit) {
        var prevX: Instant = Instant.DISTANT_PAST
        upperBounds.forEachRangeEqualBy(selector = { it.first.x }) { l, r ->
            var prevY: Int = Int.MIN_VALUE
            upperBounds.forEach(l, r) { (point, handleColor) ->
                block(GraphPoint(prevX, prevY), point, handleColor)
                prevY = point.y
            }
            prevX = upperBounds[l].first.x
        }
    }
}

private inline fun <T> List<T>.forEach(from: Int, to: Int = size, block: (T) -> Unit) {
    var i = from
    while (i < to) {
        block(get(i))
        ++i
    }
}