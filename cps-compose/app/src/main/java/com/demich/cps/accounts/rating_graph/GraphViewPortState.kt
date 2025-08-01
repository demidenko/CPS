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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
internal fun rememberGraphViewPortState(): GraphViewPortState  {
    val rectState = rememberSaveable(stateSaver = rectSaver()) { mutableStateOf(Rect.Zero) }
    return remember(rectState) { GraphViewPortState(rectState = rectState, borderX = 10.dp) }
}

@Stable
internal class GraphViewPortState(
    rectState: MutableState<Rect>,
    val borderX: Dp
) {
    //x is seconds, y is rating
    private var rect: Rect by rectState

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

    fun setViewPort(bounds: RatingGraphBounds) {
        rect = bounds.fixTimeWidth().toRect()
    }

    // TODO:
    // cancel ongoing
    // cancel on gesture
    suspend fun animateToViewPort(bounds: RatingGraphBounds) {
        val anim = Animatable(typeConverter = Rect.VectorConverter, initialValue = rect)
        anim.animateTo(
            targetValue = bounds.fixTimeWidth().toRect(),
        ) {
            rect = value
        }
    }

    context(density: Density)
    private fun Size.toBorderedRect(): Rect =
        toRect().inflate(horizontal = density.run { borderX.toPx() }, vertical = 0f)

    context(scope: DrawScope)
    fun canvasRect(): Rect = scope.size.toBorderedRect()

    context(scope: PointerInputScope)
    fun canvasRect(): Rect = scope.size.toSize().toBorderedRect()

    context(scope: DrawScope)
    fun translator() = GraphPointTranslator(
        viewPortRect = rect,
        canvasRect = canvasRect()
    )

    context(scope: PointerInputScope)
    fun translator() = GraphPointTranslator(
        viewPortRect = rect,
        canvasRect = canvasRect()
    )

    context(scope: PointerInputScope)
    suspend fun detectTransformGestures() {
        val minSize = Size(
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
                .coercedScale(scale = zoom, center = centroid, minSize = minSize)
        }
    }
}

internal class GraphPointTranslator(
    val viewPortRect: Rect,
    val canvasRect: Rect
) {
    fun pointXToCanvasX(x: Long) =
        x.toFloatUseInf().transformX(from = viewPortRect, to = canvasRect)

    fun pointYToCanvasY(y: Long) =
        y.toFloatUseInf().transformY(from = viewPortRect, to = canvasRect)

    fun pointToCanvas(point: GraphPoint) =
        Offset(
            x = pointXToCanvasX(point.x),
            y = pointYToCanvasY(point.y)
        )

}

internal fun GraphPointTranslator.pointsToCanvasPath(points: List<GraphPoint>): Path {
    return Path().apply {
        points.forEachIndexed { index, point ->
            val (px, py) = pointToCanvas(point = point)
            if (index == 0) moveTo(px, py)
            else lineTo(px, py)
        }
    }
}

context(scope: DrawScope)
internal inline fun GraphPointTranslator.pointRectToCanvasRect(
    bottomLeft: GraphPoint,
    topRight: GraphPoint,
    block: (Rect) -> Unit
) {
    val (width, height) = scope.size

    val left = floor(pointXToCanvasX(bottomLeft.x).coerceAtLeast(0f))
    val right = ceil(pointXToCanvasX(topRight.x).coerceAtMost(width))
    val top = floor(pointYToCanvasY(topRight.y).coerceAtLeast(0f))
    val bottom = ceil(pointYToCanvasY(bottomLeft.y).coerceAtMost(height))

    if (left <= right && top <= bottom) {
        block(Rect(left = left, top = top, right = right, bottom = bottom))
    }
}

context(scope: PointerInputScope)
internal fun GraphViewPortState.getNearestRatingChange(
    ratingChanges: List<RatingChange>,
    tap: Offset,
    tapRadius: Float
): RatingChange? {
    val translator = translator()
    val pos = ratingChanges.minOfWithIndex {
        val o = translator.pointToCanvas(it.toGraphPoint())
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

private fun Rect.coercedScale(scale: Float, center: Offset, minSize: Size): Rect {
    val scale = scale.coerceAtMost(size.maxScale(minWidth = minSize.width, minHeight = minSize.height))
    if (scale == 1f) return this
    return scale(scale = scale, center = center)
}

private fun Long.toFloatUseInf(): Float =
    when (this) {
        Long.MIN_VALUE -> Float.NEGATIVE_INFINITY
        Long.MAX_VALUE -> Float.POSITIVE_INFINITY
        else -> toFloat()
    }