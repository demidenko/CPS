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
    private val rectangles: List<Pair<Point, HandleColor>> = buildList {
        fun addBounds(bounds: Array<HandleColorBound>, x: Long) {
            bounds.sortedBy { it.ratingUpperBound }.let { list ->
                for (i in list.indices) {
                    val y = if (i == 0) Long.MIN_VALUE else list[i-1].ratingUpperBound.toLong()
                    add(Point(x = x, y = y) to list[i].handleColor)
                }
                add(Point(x = x, y = list.last().ratingUpperBound.toLong()) to HandleColor.RED)
            }
        }
        addBounds(x = Long.MAX_VALUE, bounds = manager.ratingsUpperBounds)
        if (manager is RatingRevolutionsProvider) {
            manager.ratingUpperBoundRevolutions
                .sortedByDescending { it.first }
                .forEach { (time, bounds) ->
                    addBounds(x = time.epochSeconds, bounds = bounds)
                }
        }
    }.apply {
        require(isSortedWith(compareByDescending<Pair<Point, HandleColor>> { it.first.x }.thenBy { it.first.y }))
    }

    inline fun forEach(block: (Point, HandleColor) -> Unit) =
        rectangles.forEach { block(it.first, it.second) }

    fun getHandleColor(point: Point): HandleColor =
        rectangles.last { (r, _) -> point.x < r.x && point.y >= r.y }.second

    fun iterateWithHandleColor(points: List<Point>, block: (Point, HandleColor) -> Unit) {
        /*
        fast version of
        points.forEach { point -> block(point, getHandleColor(point)) }
         */
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