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
import com.demich.cps.ui.geom.inflate
import com.demich.cps.ui.geom.projectPoint
import com.demich.cps.ui.geom.projectVector
import com.demich.cps.ui.geom.projectX
import com.demich.cps.ui.geom.projectY
import com.demich.cps.ui.geom.rectProjector
import com.demich.cps.ui.geom.scale
import com.demich.cps.utils.rectSaver
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Instant

@Composable
internal fun rememberViewPortState(
    inflateHorizontal: Dp = 0.dp,
    inflateVertical: Dp = 0.dp,
): ViewPortState  {
    val rectState = rememberSaveable(stateSaver = rectSaver()) { mutableStateOf(Rect.Zero) }
    return remember(rectState) {
        ViewPortState(
            rectState = rectState,
            inflateHorizontal = inflateHorizontal,
            inflateVertical = inflateVertical
        )
    }
}

//TODO: make inflate as inner state with set method

@Stable
internal class ViewPortState(
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
    private val from: Rect,
    private val to: Rect
) {
    fun projectX(x: Float) =
        x.projectX(from = from, to = to)

    fun projectY(y: Float) =
        y.projectY(from = from, to = to)
}

context(translator: GraphPointTranslator)
internal fun Instant.toCanvasX() = translator.projectX(x = toGraphX())

context(translator: GraphPointTranslator)
internal fun Int.toCanvasY() = translator.projectY(y = toGraphY())

context(translator: GraphPointTranslator)
internal fun GraphPoint.toCanvasPoint() =
    Offset(
        x = x.toCanvasX(),
        y = y.toCanvasY()
    )

context(translator: GraphPointTranslator)
internal inline fun <R> GraphPoint.toCanvasPoint(block: (Float, Float) -> R) =
    block(
        x.toCanvasX(),
        y.toCanvasY()
    )

internal inline fun <R> ViewPortState.withTranslator(
    canvasRect: Rect,
    block: GraphPointTranslator.() -> R
): R {
    return GraphPointTranslator(
        from = rect,
        to = canvasRect
    ).block()
}

context(scope: DrawScope)
internal inline fun <R> ViewPortState.withTranslator(
    block: GraphPointTranslator.() -> R
): R = withTranslator(canvasRect = canvasRect(), block = block)

context(scope: PointerInputScope)
internal inline fun <R> ViewPortState.withTranslator(
    block: GraphPointTranslator.() -> R
): R = withTranslator(canvasRect = canvasRect(), block = block)

context(translator: GraphPointTranslator)
internal fun List<GraphPoint>.toCanvasPath(path: Path) {
    path.reset()
    forEachIndexed { index, point ->
        point.toCanvasPoint { px, py ->
            if (index == 0) path.moveTo(px, py)
            else path.lineTo(px, py)
        }
    }
}

context(translator: GraphPointTranslator)
internal fun List<GraphPoint>.toCanvasPath(): Path =
    Path().also { toCanvasPath(it) }

context(scope: DrawScope, translator: GraphPointTranslator)
internal inline fun toCanvasRect(
    bottomLeft: GraphPoint,
    topRight: GraphPoint,
    block: (Rect) -> Unit
) {
    val (width, height) = scope.size

    val left = floor(bottomLeft.x.toCanvasX().coerceAtLeast(0f))
    val right = ceil(topRight.x.toCanvasX().coerceAtMost(width))
    val top = floor(topRight.y.toCanvasY().coerceAtLeast(0f))
    val bottom = ceil(bottomLeft.y.toCanvasY().coerceAtMost(height))

    if (left <= right && top <= bottom) {
        block(Rect(left = left, top = top, right = right, bottom = bottom))
    }
}

context(scope: PointerInputScope)
internal suspend fun ViewPortState.detectTransformGestures(
    minWidth: Duration,
    minHeight: Int
) {
    val minSize = Size(
        width = minWidth.inWholeSeconds.toFloat(),
        height = minHeight.toFloat()
    )

    scope.detectTransformGestures { centroid, pan, zoom, _ ->
        val viewPort = rect
        rectProjector(from = canvasRect(), to = viewPort) {
            val pan = pan.projectVector()
            val centroid = centroid.projectPoint()

            setViewPort(
                rect = viewPort
                    .translate(-pan)
                    .coercedScale(scale = zoom, center = centroid, minSize = minSize)
            )
        }
    }
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
