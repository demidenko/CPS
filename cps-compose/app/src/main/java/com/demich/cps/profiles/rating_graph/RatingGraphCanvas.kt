package com.demich.cps.profiles.rating_graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.demich.cps.profiles.HandleColor
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.managers.RatingChange
import com.demich.cps.profiles.managers.availableHandleColors
import com.demich.cps.profiles.managers.colorFor
import com.demich.cps.ui.geom.toOffset
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.spawnDpState
import kotlin.time.Instant

@Composable
internal fun RatingGraphCanvas(
    ratingChanges: List<RatingChange>,
    manager: RatedProfileManager<*>,
    rectangles: RatingGraphRectangles,
    viewPortState: GraphViewPortState,
    currentTime: Instant,
    filterType: RatingFilterType,
    selectedRatingChange: RatingChange?,
    modifier: Modifier = Modifier
) {
    val cpsColors = cpsColors
    val colorsMap = remember(manager, cpsColors) {
        manager.run {
            availableHandleColors().associateWith { cpsColors.colorFor(handleColor = it) }
        }
    }

    val markVerticals: List<Instant> = remember(filterType, ratingChanges, currentTime) {
        if (filterType == ALL || ratingChanges.size < 2) emptyList()
        else {
            createBounds(ratingChanges, filterType, currentTime).run {
                listOf(startTime, endTime)
            }
        }
    }

    val ratingPoints = remember(ratingChanges) {
        ratingChanges.map { it.toGraphPoint() }.sortedBy { it.x }
    }

    RatingGraphCanvas(
        ratingPoints = ratingPoints,
        selectedPoint = selectedRatingChange?.toGraphPoint(),
        markVerticals = markVerticals,
        getColor = colorsMap::getValue,
        viewPortState = viewPortState,
        rectangles = rectangles,
        lineColor = Color.Black,
        modifier = modifier
    )
}

@Composable
private fun RatingGraphCanvas(
    ratingPoints: List<GraphPoint>,
    selectedPoint: GraphPoint?,
    markVerticals: List<Instant>,
    getColor: (HandleColor) -> Color,
    viewPortState: GraphViewPortState,
    rectangles: RatingGraphRectangles,
    lineColor: Color,
    markerColor: Color = lineColor,
    modifier: Modifier = Modifier,
    circleRadius: Dp = 2.25.dp,
    circleBorderWidth: Dp = 1.25.dp,
    pathWidth: Dp = 1.5.dp,
    shadowOffset: DpOffset = DpOffset(1.5.dp, 1.5.dp),
    shadowColor: Color = Color.Black,
    shadowAlpha: Float = 0.3f,
    selectedPointScale: Float = 1.5f
) {
    val circleRadius by spawnDpState(circleRadius)
    val circleBorderWidth by spawnDpState(circleBorderWidth)
    val pathWidth by spawnDpState(pathWidth)

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    val pointsWithColors = remember(ratingPoints, rectangles, getColor) {
        ratingPoints.map { it to getColor(rectangles.getHandleColor(it)) }
    }

    val shadowLayer = rememberGraphicsLayer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val circleBorderWidth = circleBorderWidth.toPx()
        val circleRadius = circleRadius.toPx()
        val pathWidth = pathWidth.toPx()
        val shadowOffset = shadowOffset.toOffset()

        viewPortState.withTranslator {
            val ratingPath = pointsToCanvasPath(ratingPoints)

            //rating filled areas
            drawRatingBackground(
                rectangles = rectangles,
                getColor = getColor
            )

            //time dashes
            if (selectedPoint != null) {
                val p = selectedPoint.toCanvasPoint()
                drawVertical(
                    x = p.x,
                    bottomY = p.y,
                    color = markerColor,
                    pathEffect = dashEffect
                )
            } else {
                markVerticals.forEach { x ->
                    drawVertical(
                        x = x.toCanvasX(),
                        color = markerColor,
                        pathEffect = dashEffect
                    )
                }
            }

            drawWithShadow(
                shadowColor = shadowColor,
                shadowAlpha = shadowAlpha,
                offset = shadowOffset,
                graphicsLayer = shadowLayer
            ) {
                //rating path
                drawPath(
                    path = ratingPath,
                    color = lineColor,
                    style = Stroke(width = pathWidth)
                )

                //rating points
                pointsWithColors.forEach { (point, color) ->
                    drawPoint(
                        center = point.toCanvasPoint(),
                        color = color,
                        borderColor = lineColor,
                        radius = circleRadius,
                        borderWidth = circleBorderWidth,
                        isSelected = point == selectedPoint,
                        selectedScale = selectedPointScale
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPoint(
    center: Offset,
    color: Color,
    borderColor: Color,
    radius: Float,
    borderWidth: Float,
    isSelected: Boolean,
    selectedScale: Float
) {
    val multiplier = if (isSelected) selectedScale else 1f
    drawCircle(
        color = borderColor,
        radius = (radius + borderWidth) * multiplier,
        center = center,
        style = Fill
    )
    drawCircle(
        color = color,
        radius = radius * multiplier,
        center = center,
        style = Fill
    )
    if (isSelected) {
        drawCircle(
            color = borderColor,
            radius = radius / 2 * multiplier,
            center = center,
            style = Fill
        )
    }
}

context(translator: GraphPointTranslator)
private inline fun DrawScope.drawRatingBackground(
    rectangles: RatingGraphRectangles,
    getColor: (HandleColor) -> Color
) {
    rectangles.forEachRect { bottomLeft, topRight, handleColor ->
        translator.pointRectToCanvasRect(
            bottomLeft = bottomLeft,
            topRight = topRight
        ) { rect ->
            drawRect(
                color = getColor(handleColor),
                topLeft = rect.topLeft,
                size = rect.size
            )
        }
    }
}

private inline fun DrawScope.drawWithShadow(
    shadowColor: Color,
    shadowAlpha: Float,
    offset: Offset,
    graphicsLayer: GraphicsLayer,
    crossinline block: DrawScope.() -> Unit
) {
    graphicsLayer.apply {
        colorFilter = ColorFilter.tint(color = shadowColor)
        alpha = shadowAlpha
        record {
            translate(left = offset.x, top = offset.y) {
                block()
            }
        }
    }

    drawLayer(graphicsLayer)
    block()
}

private fun DrawScope.drawVertical(
    x: Float,
    topY: Float = 0f,
    bottomY: Float = size.height,
    color: Color,
    pathEffect: PathEffect? = null
) {
    drawLine(
        color = color,
        start = Offset(x = x, y = topY),
        end = Offset(x = x, y = bottomY),
        pathEffect = pathEffect
    )
}
