package com.demich.cps.accounts.rating_graph

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.toSize
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.utils.minOfWithIndex
import com.demich.cps.utils.rectSaver
import kotlin.math.round
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
internal fun rememberCoordinateTranslator(): CoordinateTranslator  {
    val rectState = rememberSaveable(stateSaver = rectSaver()) { mutableStateOf(Rect.Zero) }
    return CoordinateTranslator(rectState)
}

@Stable
internal class CoordinateTranslator(
    rectState: MutableState<Rect>
) {
    //x is seconds, y is rating
    private var rect: Rect by rectState

    //TODO: get rid of this
    var borderX: Float = 0f

    private fun RatingGraphBounds.setAsWindow() {
        require(startTime < endTime)

        val ratingBorder = 100

        rect = Rect(
            left = startTime.epochSeconds.toFloat(),
            right = endTime.epochSeconds.toFloat(),
            top = (minRating - ratingBorder).toFloat(),
            bottom = (maxRating + ratingBorder).toFloat()
        )
    }

    fun setWindow(bounds: RatingGraphBounds) {
        with(bounds) {
            if (startTime == endTime) {
                copy(
                    startTime = bounds.startTime - 1.days,
                    endTime = bounds.endTime + 1.days
                ).setAsWindow()
            } else {
                setAsWindow()
            }
        }
    }

    private fun transformX(
        x: Float,
        fromWidth: Float,
        toWidth: Float
    ) = (x - fromWidth/2) * (fromWidth / toWidth) + (fromWidth / 2)

    fun pointXToCanvasX(x: Long, canvasSize: Size) =
        transformX(
            x = (x - rect.left) / rect.size.width * canvasSize.width,
            fromWidth = canvasSize.width,
            toWidth = canvasSize.width + borderX * 2
        )

    fun pointYToCanvasY(y: Long, canvasSize: Size) =
        canvasSize.height - ((y - rect.top) / rect.size.height * canvasSize.height)

    fun pointToCanvas(point: Point, canvasSize: Size) = Offset(
        x = pointXToCanvasX(point.x, canvasSize),
        y = pointYToCanvasY(point.y, canvasSize)
    )

    private fun offsetToPoint(offset: Offset, canvasSize: Size) = Offset(
        x = transformX(
            x = offset.x,
            fromWidth = canvasSize.width + borderX * 2,
            toWidth = canvasSize.width
        ) / canvasSize.width * rect.size.width + rect.left,
        y = (canvasSize.height - offset.y) / canvasSize.height * rect.size.height + rect.top
    )

    private fun Rect.move(offset: Offset, canvasSize: Size): Rect {
        val offset = offsetToPoint(offset, canvasSize) - offsetToPoint(Offset.Zero, canvasSize)
        return translate(-offset)
    }

    private fun Rect.scale(center: Offset, scale: Float, canvasSize: Size): Rect {
        val scale = scale.coerceAtMost(rect.size.maxScale(minWidth = 1.hours.inWholeSeconds.toFloat(), minHeight = 1f))
        val center = offsetToPoint(offset = center, canvasSize = canvasSize)
        return Rect(
            topLeft = (topLeft - center) / scale + center,
            bottomRight = (bottomRight - center) / scale + center
        )
    }

    context(scope: PointerInputScope)
    suspend fun detectTransformGestures() {
        scope.detectTransformGestures { centroid, pan, zoom, _ ->
            val canvasSize = scope.size.toSize()
            rect = rect
                .move(offset = pan, canvasSize = canvasSize)
                .scale(center = centroid, scale = zoom, canvasSize = canvasSize)
        }
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
    block: (Rect) -> Unit
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
        block(Rect(left = minX, top = minY, right = maxX, bottom = maxY))
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