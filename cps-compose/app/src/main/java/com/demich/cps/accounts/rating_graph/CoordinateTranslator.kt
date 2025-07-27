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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.utils.inflate
import com.demich.cps.utils.minOfWithIndex
import com.demich.cps.utils.rectSaver
import com.demich.cps.utils.scale
import com.demich.cps.utils.transform
import com.demich.cps.utils.transformVector
import com.demich.cps.utils.transformX
import com.demich.cps.utils.transformY
import kotlin.math.absoluteValue
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
            top = (maxRating + ratingBorder).toFloat(),
            bottom = (minRating - ratingBorder).toFloat()
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

    fun pointXToCanvasX(x: Long, canvasRect: Rect) =
        x.toFloat().transformX(from = rect, to = canvasRect)

    fun pointYToCanvasY(y: Long, canvasRect: Rect) =
        y.toFloat().transformY(from = rect, to = canvasRect)

    fun pointToCanvas(point: Point, canvasRect: Rect) =
        Offset(
            x = pointXToCanvasX(point.x, canvasRect),
            y = pointYToCanvasY(point.y, canvasRect)
        )

    context(density: Density)
    private fun Rect.inflateBorders(): Rect =
        inflate(horizontal = borderX, vertical = 0f)

    context(scope: DrawScope)
    fun canvasRect(): Rect = scope.size.toRect().inflateBorders()

    context(scope: PointerInputScope)
    fun canvasRect(): Rect = scope.size.toSize().toRect().inflateBorders()

    context(scope: PointerInputScope)
    suspend fun detectTransformGestures() {
        val limitScaleSize = Size(
            width = 1.hours.inWholeSeconds.toFloat(),
            height = 1f
        )

        scope.detectTransformGestures { centroid, pan, zoom, _ ->
            val canvasRect = canvasRect()

            val rect = rect
            val pan = pan.transformVector(from = canvasRect, to = rect)
            val centroid = centroid.transform(from = canvasRect, to = rect)

            this.rect = rect
                .translate(-pan)
                .coercedScale(scale = zoom, center = centroid, limitSize = limitScaleSize)
        }
    }
}

context(scope: DrawScope)
internal fun CoordinateTranslator.pointXToCanvasX(x: Long) =
    pointXToCanvasX(x = x, canvasRect = canvasRect())

context(scope: DrawScope)
internal fun CoordinateTranslator.pointYToCanvasY(y: Long) =
    pointYToCanvasY(y = y, canvasRect = canvasRect())

context(scope: DrawScope)
internal fun CoordinateTranslator.pointToCanvas(point: Point) =
    pointToCanvas(point = point, canvasRect = canvasRect())

context(scope: DrawScope)
internal inline fun CoordinateTranslator.pointRectToCanvasRect(
    bottomLeft: Point,
    topRight: Point,
    block: (Rect) -> Unit
) {
    val canvasRect = canvasRect()
    val (width, height) = scope.size

    val minX = with(bottomLeft) {
        if (x == Long.MIN_VALUE) 0f
        else round(pointXToCanvasX(x, canvasRect)).coerceAtLeast(0f)
    }

    val maxX = with(topRight) {
        if (x == Long.MAX_VALUE) width
        else round(pointXToCanvasX(x, canvasRect)).coerceAtMost(width)
    }

    val minY = with(topRight) {
        if (y == Long.MAX_VALUE) 0f
        else round(pointYToCanvasY(y, canvasRect)).coerceAtLeast(0f)
    }

    val maxY = with(bottomLeft) {
        if (y == Long.MIN_VALUE) height
        else round(pointYToCanvasY(y, canvasRect)).coerceAtMost(height)
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
    val canvasRect = canvasRect()
    val pos = ratingChanges.minOfWithIndex {
        val o = pointToCanvas(point = it.toPoint(), canvasRect = canvasRect)
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
    require(minWidth > 0)
    require(minHeight > 0)
    // |width| / scale >= minWidth
    // |width| / minWidth >= scale
    // |height| / scale >= minHeight
    // |height| / minHeight >= scale
    return minOf(width.absoluteValue / minWidth, height.absoluteValue / minHeight)
}

private fun Rect.coercedScale(scale: Float, center: Offset, limitSize: Size): Rect {
    val scale = scale.coerceAtMost(size.maxScale(minWidth = limitSize.width, minHeight = limitSize.height))
    if (scale == 1f) return this
    return scale(scale = scale, center = center)
}