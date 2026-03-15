package com.demich.cps.profiles.rating_graph

import androidx.compose.runtime.Immutable
import com.demich.cps.profiles.HandleColor
import com.demich.cps.profiles.HandleColorBound
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.managers.RatingRevolutionsProvider
import com.demich.cps.profiles.userinfo.RatedUserInfo
import com.demich.cps.utils.forEachRangeEqualBy
import com.demich.kotlin_stdlib_boost.isSortedWith

@Immutable
internal class RatingGraphRectangles(
    manager: RatedProfileManager<out RatedUserInfo>
) {
    //point is upperBound (endTime, ratingUpperBound)
    private val rectangles: List<Pair<GraphPoint, HandleColor>> = buildList {
        fun addBounds(x: Long, bounds: List<HandleColorBound>) {
            bounds.sortedBy { it.ratingUpperBound }.forEach {
                add(GraphPoint(x = x, y = it.ratingUpperBound.toLong()) to it.handleColor)
            }
            add(GraphPoint(x = x, y = Long.MAX_VALUE) to HandleColor.RED)
        }
        if (manager is RatingRevolutionsProvider) {
            manager.ratingUpperBoundRevolutions
                .sortedBy { it.first }
                .forEach { (endTime, bounds) ->
                    addBounds(x = endTime.epochSeconds, bounds = bounds)
                }
        }
        addBounds(x = Long.MAX_VALUE, bounds = manager.ratingsUpperBounds)
    }.apply {
        check(isSortedWith(compareBy<Pair<GraphPoint, HandleColor>> { it.first.x }.thenBy { it.first.y }))
    }

    fun getHandleColor(point: GraphPoint): HandleColor =
        rectangles.first { (r, _) -> point.x < r.x && point.y < r.y }.second

    inline fun forEachUpperBound(block: (GraphPoint, HandleColor) -> Unit) =
        rectangles.asReversed().forEach { block(it.first, it.second) }

    inline fun forEachRect(block: (GraphPoint, GraphPoint, HandleColor) -> Unit) {
        var prevX: Long = Long.MIN_VALUE
        rectangles.forEachRangeEqualBy(selector = { it.first.x }) { l, r ->
            var prevY: Long = Long.MIN_VALUE
            rectangles.forEach(l, r) { (point, handleColor) ->
                block(GraphPoint(prevX, prevY), point, handleColor)
                prevY = point.y
            }
            prevX = rectangles[l].first.x
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