package com.demich.cps.accounts.rating_graph

import androidx.compose.runtime.Immutable
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.HandleColorBound
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingRevolutionsProvider
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.utils.firstTrue
import com.demich.cps.utils.forEachRangeEqualBy
import com.demich.cps.utils.isSortedWith

@Immutable
internal class RatingGraphRectangles(
    manager: RatedAccountManager<out RatedUserInfo>
) {
    //point is upperBound (endTime, ratingUpperBound)
    private val rectangles: List<Pair<Point, HandleColor>> = buildList {
        fun addBounds(x: Long, bounds: Array<HandleColorBound>) {
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
        require(isSortedWith(compareBy<Pair<Point, HandleColor>> { it.first.x }.thenBy { it.first.y }))
    }

    inline fun forEachUpperBound(block: (Point, HandleColor) -> Unit) =
        rectangles.asReversed().forEach { block(it.first, it.second) }

    inline fun forEachRect(block: (Point, Point, HandleColor) -> Unit) {
        var prevX: Long = Long.MIN_VALUE
        forEachXRange { l, r ->
            var prevY: Long = Long.MIN_VALUE
            rectangles.subList(l, r).forEach { (point, handleColor) ->
                block(Point(prevX, prevY), point, handleColor)
                prevY = point.y
            }
            prevX = rectangles[l].first.x
        }
    }

    fun getHandleColor(point: Point): HandleColor =
        rectangles.first { (r, _) -> point.x < r.x && point.y < r.y }.second

    fun iterateWithHandleColor(points: List<Point>, block: (Point, HandleColor) -> Unit) {
        /*
        fast version of
        points.forEach { point -> block(point, getHandleColor(point)) }
         */
        require(points.isSortedWith(compareBy { it.x }))
        var k = 0
        forEachXRange { l, r ->
            while (k < points.size && points[k].x < rectangles[l].first.x) {
                val point = points[k++]
                val i = firstTrue(l, r) { point.y < rectangles[it].first.y }
                block(point, rectangles[i].second)
            }
        }
    }

    private inline fun forEachXRange(block: (Int, Int) -> Unit) =
        rectangles.forEachRangeEqualBy(selector = { it.first.x }, block = block)
}