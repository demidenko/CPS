package com.demich.cps.accounts.rating_graph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.toSize
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.utils.inflate
import com.demich.cps.utils.minOfWithIndex
import com.demich.cps.utils.rectSaver
import com.demich.cps.utils.scale
import com.demich.cps.utils.transform
import com.demich.cps.utils.transformVector
import kotlin.math.round
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
internal fun rememberCoordinateTranslator(): CoordinateTranslator  {
    val rectState = rememberSaveable(stateSaver = rectSaver()) { mutableStateOf(Rect.Zero) }
    return remember(rectState) { CoordinateTranslator(rectState) }
}

@Stable
internal class CoordinateTranslator(
    rectState: MutableState<Rect>
) {
    //x is seconds, y is rating
    private var rect: Rect by rectState

    //TODO: get rid of this
    var borderX: Float = 0f

    private fun RatingGraphBounds.toRect(): Rect {
        require(startTime < endTime)

        val ratingBorder = 100

        return Rect(
            left = startTime.epochSeconds.toFloat(),
            right = endTime.epochSeconds.toFloat(),
            top = (minRating - ratingBorder).toFloat(),
            bottom = (maxRating + ratingBorder).toFloat()
        )
    }

    fun setWindow(bounds: RatingGraphBounds) {
        rect = bounds.fixTimeWidth().toRect()
    }

    // TODO:
    // cancel ongoing
    // cancel on gesture
    suspend fun animateToWindow(bounds: RatingGraphBounds) {
        val anim = Animatable(typeConverter = Rect.VectorConverter, initialValue = rect)
        anim.animateTo(
            targetValue = bounds.fixTimeWidth().toRect(),
        ) {
            rect = value
        }
    }

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

    context(scope: PointerInputScope)
    suspend fun detectTransformGestures() {
        scope.detectTransformGestures { centroid, pan, zoom, _ ->
            val canvasRect = scope.size.toSize().toRect()
                .inflate(horizontal = borderX, vertical = 0f)

            rect = rect
                .move(offset = pan, canvasRect = canvasRect)
                .scale(scale = zoom, canvasCenter = centroid, canvasRect = canvasRect)
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


private fun RatingGraphBounds.fixTimeWidth() =
    if (startTime == endTime) {
        copy(
            startTime = startTime - 1.days,
            endTime = endTime + 1.days
        )
    } else {
        this
    }

private fun Size.maxScale(minWidth: Float, minHeight: Float): Float {
    //width / scale >= minWidth
    //width / minWidth >= scale
    //height / scale >= minHeight
    //height / minHeight >= scale
    return minOf(width / minWidth, height / minHeight)
}

private fun transformX(
    x: Float,
    fromWidth: Float,
    toWidth: Float
) = x.scale(scale = toWidth / fromWidth, center = fromWidth / 2)


private fun Rect.move(offset: Offset, canvasRect: Rect): Rect {
    val offset = offset.transformVector(from = canvasRect, to = this)
    return translate(-offset)
}

private fun Rect.scale(scale: Float, canvasCenter: Offset, canvasRect: Rect): Rect {
    val scale = scale.coerceAtMost(size.maxScale(minWidth = 1.hours.inWholeSeconds.toFloat(), minHeight = 1f))
    if (scale == 1f) return this
    val center = canvasCenter.transform(from = canvasRect, to = this)
    return scale(scale = scale, center = center)
}