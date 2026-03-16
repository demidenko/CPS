package com.demich.cps.profiles.rating_graph

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
import com.demich.cps.profiles.managers.RatingChange
import com.demich.cps.ui.inflate
import com.demich.cps.ui.scale
import com.demich.cps.ui.transform
import com.demich.cps.ui.transformVector
import com.demich.cps.ui.transformX
import com.demich.cps.ui.transformY
import com.demich.cps.utils.minOfWithIndex
import com.demich.cps.utils.rectSaver
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Instant

@Composable
internal fun rememberGraphViewPortState(
    inflateHorizontal: Dp = 0.dp,
    inflateVertical: Dp = 0.dp,
): GraphViewPortState  {
    val rectState = rememberSaveable(stateSaver = rectSaver()) { mutableStateOf(Rect.Zero) }
    return remember(rectState) {
        GraphViewPortState(
            rectState = rectState,
            inflateHorizontal = inflateHorizontal,
            inflateVertical = inflateVertical
        )
    }
}

//TODO: make inflate as inner state with set method

@Stable
internal class GraphViewPortState(
    private val rectState: MutableState<Rect>,
    private val inflateHorizontal: Dp,
    private val inflateVertical: Dp,
) {

    //TODO: precision loss (make DoubleRect?)
    //x is seconds, y is rating
    val rect: Rect by rectState

    fun setViewPort(rect: Rect) {
        rectState.value = rect
    }

    // TODO:
    // cancel ongoing
    // cancel on gesture
    suspend fun animateToViewPort(targetRect: Rect) {
        val anim = Animatable(typeConverter = Rect.VectorConverter, initialValue = rect)
        anim.animateTo(targetValue = targetRect) {
            setViewPort(rect = value)
        }
    }

    context(density: Density)
    private fun Size.toInflatedRect(): Rect =
        with(density) {
            toRect().inflate(
                horizontal = inflateHorizontal.toPx(),
                vertical = inflateVertical.toPx()
            )
        }

    context(scope: DrawScope)
    fun canvasRect(): Rect = scope.size.toInflatedRect()

    context(scope: PointerInputScope)
    fun canvasRect(): Rect = scope.size.toSize().toInflatedRect()
}

internal class GraphPointTranslator(
    val viewPortRect: Rect,
    val canvasRect: Rect
) {
    fun pointXToCanvasX(x: Instant) =
        x.toGraphX().transformX(from = viewPortRect, to = canvasRect)

    fun pointYToCanvasY(y: Int) =
        y.toGraphY().transformY(from = viewPortRect, to = canvasRect)

    fun pointToCanvas(point: GraphPoint) =
        Offset(
            x = pointXToCanvasX(point.x),
            y = pointYToCanvasY(point.y)
        )

}

context(scope: DrawScope)
internal fun GraphViewPortState.translator() = GraphPointTranslator(
    viewPortRect = rect,
    canvasRect = canvasRect()
)

context(scope: PointerInputScope)
internal fun GraphViewPortState.translator() = GraphPointTranslator(
    viewPortRect = rect,
    canvasRect = canvasRect()
)

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
internal suspend fun GraphViewPortState.detectTransformGestures(
    minWidth: Duration,
    minHeight: Int
) {
    val minSize = Size(
        width = minWidth.inWholeSeconds.toFloat(),
        height = minHeight.toFloat()
    )

    scope.detectTransformGestures { centroid, pan, zoom, _ ->
        val canvasRect = canvasRect()

        val rect = rect
        val pan = pan.transformVector(from = canvasRect, to = rect)
        val centroid = centroid.transform(from = canvasRect, to = rect)

        setViewPort(
            rect = rect
                .translate(-pan)
                .coercedScale(scale = zoom, center = centroid, minSize = minSize)
        )
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

internal fun Instant.toGraphX(): Float =
    when (this) {
        Instant.DISTANT_PAST -> Float.NEGATIVE_INFINITY
        Instant.DISTANT_FUTURE -> Float.POSITIVE_INFINITY
        else -> epochSeconds.toFloat()
    }

internal fun Int.toGraphY(): Float =
    when (this) {
        Int.MIN_VALUE -> Float.NEGATIVE_INFINITY
        Int.MAX_VALUE -> Float.POSITIVE_INFINITY
        else -> toFloat()
    }
