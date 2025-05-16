package com.demich.cps.accounts.rating_graph

import androidx.compose.runtime.Immutable
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.HandleColorBound
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingRevolutionsProvider
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.utils.forEachRangeEqualBy
import com.demich.kotlin_stdlib_boost.isSortedWith

@Immutable
internal class RatingGraphRectangles(
    manager: RatedAccountManager<out RatedUserInfo>
) {
    //point is upperBound (endTime, ratingUpperBound)
    private val rectangles: List<Pair<Point, HandleColor>> = buildList {
        fun addBounds(x: Long, bounds: List<HandleColorBound>) {
            bounds.sortedBy { it.ratingUpperBound }.forEach {
                add(Point(x = x, y = it.ratingUpperBound.toLong()) to it.handleColor)
            }
            add(Point(x = x, y = Long.MAX_VALUE) to HandleColor.RED)
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
        check(isSortedWith(compareBy<Pair<Point, HandleColor>> { it.first.x }.thenBy { it.first.y }))
    }

    fun getHandleColor(point: Point): HandleColor =
        rectangles.first { (r, _) -> point.x < r.x && point.y < r.y }.second

    inline fun forEachUpperBound(block: (Point, HandleColor) -> Unit) =
        rectangles.asReversed().forEach { block(it.first, it.second) }

    inline fun forEachRect(block: (Point, Point, HandleColor) -> Unit) {
        var prevX: Long = Long.MIN_VALUE
        rectangles.forEachRangeEqualBy(selector = { it.first.x }) { l, r ->
            var prevY: Long = Long.MIN_VALUE
            rectangles.forEach(l, r) { (point, handleColor) ->
                block(Point(prevX, prevY), point, handleColor)
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