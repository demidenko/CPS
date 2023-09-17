package com.demich.cps.accounts.rating_graph

import androidx.compose.runtime.Immutable
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.HandleColorBound
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingRevolutionsProvider
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.utils.firstFalse
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

    inline fun forEach(block: (Point, HandleColor) -> Unit) =
        rectangles.asReversed().forEach { block(it.first, it.second) }

    fun getHandleColor(point: Point): HandleColor =
        rectangles.first { (r, _) -> point.x < r.x && point.y < r.y }.second

    fun iterateWithHandleColor(points: List<Point>, block: (Point, HandleColor) -> Unit) {
        /*
        fast version of
        points.forEach { point -> block(point, getHandleColor(point)) }
         */
        points.forEach { point -> block(point, getHandleColor(point)) }
        return
        require(points.isSortedWith(compareBy { it.x }))
        var r = rectangles.size
        var l = r
        points.forEach { point ->
            while (l == rectangles.size || point.x >= rectangles[l].first.x) {
                r = l
                do --l while (l > 0 && rectangles[l-1].first.x == rectangles[r-1].first.x)
            }
            val i = firstFalse(l, r) { rectangles[it].first.y <= point.y }
            block(point, rectangles[i-1].second)
        }
    }
}