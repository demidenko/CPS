package com.demich.cps.accounts.rating_graph

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.utils.minOfWithIndex
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
internal fun rememberCoordinateTranslator(): CoordinateTranslator =
    rememberSaveable(saver = CoordinateTranslator.saver) {
        CoordinateTranslator(minX = 0f, maxX = 0f, minY = 0f, maxY = 0f)
    }

@Stable
internal class CoordinateTranslator(minX: Float, maxX: Float, minY: Float, maxY: Float) {
    //x is seconds, y is rating
    private var o: Offset by mutableStateOf(Offset(x = minX, y = minY))
    private var size: Size by mutableStateOf(Size(width = maxX - minX, height = maxY - minY))

    var canvasSize: Size = Size.Unspecified
    var borderX: Float = 0f

    fun setWindow(bounds: RatingGraphBounds) {
        if (bounds.startTime == bounds.endTime) {
            setWindow(bounds.copy(
                startTime = bounds.startTime - 1.days,
                endTime = bounds.endTime + 1.days
            ))
            return
        }
        with(bounds) {
            o = Offset(x = startTime.epochSeconds.toFloat(), y = minRating.toFloat() - 100f)
            size = Size(
                width = (endTime - startTime).inWholeSeconds.toFloat(),
                height = maxRating - minRating + 200f
            )
        }
    }

    private fun transformX(
        x: Float,
        fromWidth: Float,
        toWidth: Float
    ) = (x - fromWidth/2) * (fromWidth / toWidth) + (fromWidth / 2)

    fun pointToOffset(x: Long, y: Long) = Offset(
        x = transformX(
            x = (x - o.x) / size.width * canvasSize.width,
            fromWidth = canvasSize.width,
            toWidth = canvasSize.width + borderX * 2
        ),
        y = canvasSize.height - ((y - o.y) / size.height * canvasSize.height)
    )

    fun pointToOffset(point: Point) = pointToOffset(point.x, point.y)

    private fun offsetToPoint(offset: Offset) = Offset(
        x = transformX(
            x = offset.x,
            fromWidth = canvasSize.width + borderX * 2,
            toWidth = canvasSize.width
        ) / canvasSize.width * size.width + o.x,
        y = (canvasSize.height - offset.y) / canvasSize.height * size.height + o.y
    )

    fun move(offset: Offset) {
        o -= offsetToPoint(offset) - offsetToPoint(Offset.Zero)
    }

    fun scale(center: Offset, scale: Float) {
        size.maxScale(minWidth = 1.hours.inWholeSeconds.toFloat(), minHeight = 1f).let {
            if (scale > it) return scale(center, it)
        }
        val c = offsetToPoint(center)
        o = (o - c) / scale + c
        size /= scale
    }

    fun getNearestRatingChange(
        ratingChanges: List<RatingChange>,
        tap: Offset,
        tapRadius: Float = 50f
    ): RatingChange? {
        val pos = ratingChanges.minOfWithIndex {
            val o = pointToOffset(it.toPoint())
            (o - tap).getDistance()
        }.takeIf { it.value <= tapRadius }?.index ?: return null
        val res = ratingChanges[pos]
        if (pos == 0 || res.oldRating != null) return res
        return res.copy(oldRating = ratingChanges[pos-1].rating)
    }

    companion object {
        private fun Size.maxScale(minWidth: Float, minHeight: Float): Float {
            //width / scale >= minWidth
            //width / minWidth >= scale
            //height / scale >= minHeight
            //height / minHeight >= scale
            return minOf(width / minWidth, height / minHeight)
        }

        internal val saver get() = listSaver<CoordinateTranslator, Float>(
            save = {
                listOf(it.o.x, it.o.y, it.size.width, it.size.height)
            },
            restore = {
                CoordinateTranslator(
                    minX = it[0],
                    minY = it[1],
                    maxX = it[0] + it[2],
                    maxY = it[1] + it[3]
                )
            }
        )
    }
}