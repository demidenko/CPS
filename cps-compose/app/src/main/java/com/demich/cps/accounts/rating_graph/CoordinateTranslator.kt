package com.demich.cps.accounts.rating_graph

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.toSize
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.utils.minOfWithIndex
import kotlin.math.round
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

    var borderX: Float = 0f

    private fun RatingGraphBounds.setAsWindow() {
        require(startTime < endTime)

        val ratingBorder = 100f

        o = Offset(
            x = startTime.epochSeconds.toFloat(),
            y = minRating.toFloat() - ratingBorder
        )

        size = Size(
            width = (endTime - startTime).inWholeSeconds.toFloat(),
            height = maxRating - minRating + ratingBorder * 2
        )
    }

    fun setWindow(bounds: RatingGraphBounds) {
        if (bounds.startTime == bounds.endTime) {
            bounds.copy(
                startTime = bounds.startTime - 1.days,
                endTime = bounds.endTime + 1.days
            ).setAsWindow()
        } else {
            bounds.setAsWindow()
        }
    }

    private fun transformX(
        x: Float,
        fromWidth: Float,
        toWidth: Float
    ) = (x - fromWidth/2) * (fromWidth / toWidth) + (fromWidth / 2)

    fun pointXToCanvasX(x: Long, canvasSize: Size) =
        transformX(
            x = (x - o.x) / size.width * canvasSize.width,
            fromWidth = canvasSize.width,
            toWidth = canvasSize.width + borderX * 2
        )

    fun pointYToCanvasY(y: Long, canvasSize: Size) =
        canvasSize.height - ((y - o.y) / size.height * canvasSize.height)

    fun pointToCanvas(point: Point, canvasSize: Size) = Offset(
        x = pointXToCanvasX(point.x, canvasSize),
        y = pointYToCanvasY(point.y, canvasSize)
    )

    private fun offsetToPoint(offset: Offset, canvasSize: Size) = Offset(
        x = transformX(
            x = offset.x,
            fromWidth = canvasSize.width + borderX * 2,
            toWidth = canvasSize.width
        ) / canvasSize.width * size.width + o.x,
        y = (canvasSize.height - offset.y) / canvasSize.height * size.height + o.y
    )

    private fun move(offset: Offset, canvasSize: Size) {
        o -= offsetToPoint(offset, canvasSize) - offsetToPoint(Offset.Zero, canvasSize)
    }

    private fun scale(center: Offset, scale: Float, canvasSize: Size) {
        val scale = scale.coerceAtMost(size.maxScale(minWidth = 1.hours.inWholeSeconds.toFloat(), minHeight = 1f))
        val center = offsetToPoint(offset = center, canvasSize = canvasSize)
        o = (o - center) / scale + center
        size /= scale
    }

    context(scope: PointerInputScope)
    suspend fun detectTransformGestures() {
        scope.detectTransformGestures { centroid, pan, zoom, _ ->
            val canvasSize = scope.size.toSize()
            move(offset = pan, canvasSize = canvasSize)
            if (zoom != 1f) scale(center = centroid, scale = zoom, canvasSize = canvasSize)
        }
    }

    companion object {
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

context(scope: DrawScope)
internal fun CoordinateTranslator.pointXToCanvasX(x: Long) =
    pointXToCanvasX(x = x, canvasSize = scope.size)

context(scope: DrawScope)
internal fun CoordinateTranslator.pointYToCanvasY(y: Long) =
    pointYToCanvasY(y = y, canvasSize = scope.size)

context(scope: DrawScope)
internal fun CoordinateTranslator.pointToCanvas(point: Point) =
    pointToCanvas(point = point, canvasSize = scope.size)

context(scope: DrawScope)
internal inline fun CoordinateTranslator.pointRectToCanvasRect(
    bottomLeft: Point,
    topRight: Point,
    block: (Offset, Size) -> Unit
) {
    val minX = with(bottomLeft) {
        if (x == Long.MIN_VALUE) 0f else round(pointXToCanvasX(x)).coerceAtLeast(0f)
    }

    val maxX = with(topRight) {
        val width = scope.size.width
        if (x == Long.MAX_VALUE) width else round(pointXToCanvasX(x)).coerceAtMost(width)
    }

    val minY = with(topRight) {
        if (y == Long.MAX_VALUE) 0f else round(pointYToCanvasY(y)).coerceAtLeast(0f)
    }

    val maxY = with(bottomLeft) {
        val height = scope.size.height
        if (y == Long.MIN_VALUE) height else round(pointYToCanvasY(y)).coerceAtMost(height)
    }

    if (minX <= maxX && minY <= maxY) {
        block(
            Offset(minX, minY),
            Size(maxX - minX, maxY - minY)
        )
    }
}

context(scope: PointerInputScope)
internal fun CoordinateTranslator.getNearestRatingChange(
    ratingChanges: List<RatingChange>,
    tap: Offset,
    tapRadius: Float
): RatingChange? {
    val pos = ratingChanges.minOfWithIndex {
        val o = pointToCanvas(point = it.toPoint(), canvasSize = scope.size.toSize())
        (o - tap).getDistance()
    }.takeIf { it.value <= tapRadius }?.index ?: return null
    val res = ratingChanges[pos]
    if (pos == 0 || res.oldRating != null) return res
    return res.copy(oldRating = ratingChanges[pos-1].rating)
}

private fun Size.maxScale(minWidth: Float, minHeight: Float): Float {
    //width / scale >= minWidth
    //width / minWidth >= scale
    //height / scale >= minHeight
    //height / minHeight >= scale
    return minOf(width / minWidth, height / minHeight)
}