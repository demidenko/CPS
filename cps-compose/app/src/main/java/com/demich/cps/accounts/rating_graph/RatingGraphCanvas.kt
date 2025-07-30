package com.demich.cps.accounts.rating_graph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.colorFor
import com.demich.cps.accounts.userinfo.RatedUserInfo
import kotlin.time.Instant

@Composable
internal fun RatingGraphCanvas(
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out RatedUserInfo>,
    rectangles: RatingGraphRectangles,
    viewPortState: GraphViewPortState,
    currentTime: Instant,
    filterType: RatingFilterType,
    selectedRatingChange: RatingChange?,
    modifier: Modifier = Modifier
) {
    val managerSupportedColors = remember(manager.type) {
        HandleColor.entries.filter {
            runCatching { manager.originalColor(it) }.isSuccess
        }
    }

    val colorsMap = managerSupportedColors.associateWith { manager.colorFor(handleColor = it) }

    val timeMarkers: List<Instant> = remember(filterType, ratingChanges, currentTime) {
        if (filterType == RatingFilterType.ALL || ratingChanges.size < 2) emptyList()
        else {
            createBounds(ratingChanges, filterType, currentTime).run {
                listOf(startTime, endTime)
            }
        }
    }

    RatingGraphCanvas(
        ratingPoints = ratingChanges.map { it.toGraphPoint() }.sortedBy { it.x },
        selectedPoint = selectedRatingChange?.toGraphPoint(),
        markVerticals = timeMarkers.map { it.epochSeconds },
        getColor = colorsMap::getValue,
        viewPortState = viewPortState,
        rectangles = rectangles,
        lineColor = Color.Black,
        modifier = modifier
    )
}

@Composable
private fun spawnDpState(value: Dp): State<Dp> {
    val anim = remember { Animatable(initialValue = 0.dp, typeConverter = Dp.VectorConverter) }
    LaunchedEffect(anim, value) {
        anim.animateTo(value)
    }
    return anim.asState()
}

@Composable
private fun RatingGraphCanvas(
    ratingPoints: List<GraphPoint>,
    selectedPoint: GraphPoint?,
    markVerticals: List<Long>,
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
    val circleBorderWidth by spawnDpState(circleBorderWidth)
    val circleRadius by spawnDpState(circleRadius)
    val pathWidth by spawnDpState(pathWidth)

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    val pointsWithColors = remember(ratingPoints, rectangles, getColor) {
        ratingPoints.map { it to getColor(rectangles.getHandleColor(it)) }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        fun radius(point: GraphPoint, radius: Float) =
            if (selectedPoint == point) selectedPointScale * radius else radius

        val circleBorderWidth = circleBorderWidth.toPx()
        val circleRadius = circleRadius.toPx()
        val pathWidth = pathWidth.toPx()

        val translator = viewPortState.translator()

        val ratingPath = translator.pointsToCanvasPath(ratingPoints)

        //rating filled areas
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

        //time dashes
        if (selectedPoint != null) {
            val p = translator.pointToCanvas(selectedPoint)
            drawLine(
                color = markerColor,
                start = Offset(p.x, 0f),
                end = p,
                pathEffect = dashEffect
            )
        } else {
            markVerticals.forEach { x ->
                val px = translator.pointXToCanvasX(x)
                drawLine(
                    color = markerColor,
                    start = Offset(px, 0f),
                    end = Offset(px, size.height),
                    pathEffect = dashEffect
                )
            }
        }

        //shadow
        translate(left = shadowOffset.x.toPx(), top = shadowOffset.y.toPx()) {
            //shadow of rating path
            drawPath(
                path = ratingPath,
                color = shadowColor,
                style = Stroke(width = pathWidth),
                alpha = shadowAlpha
            )
            //shadow of rating points
            ratingPoints.forEach { point ->
                val center = translator.pointToCanvas(point)
                drawCircle(
                    color = shadowColor,
                    radius = radius(point, circleRadius + circleBorderWidth),
                    center = center,
                    style = Fill,
                    alpha = shadowAlpha
                )
            }
        }

        //rating path
        drawPath(
            path = ratingPath,
            color = lineColor,
            style = Stroke(width = pathWidth)
        )

        //rating points
        pointsWithColors.forEach { (point, color) ->
            val center = translator.pointToCanvas(point)
            drawCircle(
                color = lineColor,
                radius = radius(point, circleRadius + circleBorderWidth),
                center = center,
                style = Fill
            )
            drawCircle(
                color = color,
                radius = radius(point, circleRadius),
                center = center,
                style = Fill
            )
            if (point == selectedPoint) {
                drawCircle(
                    color = lineColor,
                    radius = radius(point, circleRadius / 2),
                    center = center,
                    style = Fill
                )
            }
        }
    }
}

internal data class GraphPoint(val x: Long, val y: Long)

internal fun RatingChange.toGraphPoint() = GraphPoint(x = date.epochSeconds, y = rating.toLong())