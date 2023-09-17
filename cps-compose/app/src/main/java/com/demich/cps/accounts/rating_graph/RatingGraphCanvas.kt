package com.demich.cps.accounts.rating_graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.colorFor
import com.demich.cps.accounts.userinfo.RatedUserInfo
import kotlinx.datetime.Instant
import kotlin.math.round

@Composable
internal fun RatingGraphCanvas(
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out RatedUserInfo>,
    rectangles: RatingGraphRectangles,
    translator: CoordinateTranslator,
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

    val timeMarkers: List<Instant> = remember(key1 = filterType, key2 = ratingChanges) {
        if (filterType == RatingFilterType.ALL || ratingChanges.size < 2) emptyList()
        else {
            createBounds(ratingChanges, filterType, currentTime).run {
                listOf(startTime, endTime)
            }
        }
    }

    RatingGraphCanvas(
        ratingPoints = ratingChanges.map { it.toPoint() }.sortedBy { it.x },
        translator = translator,
        selectedPoint = selectedRatingChange?.toPoint(),
        markVerticals = timeMarkers.map { it.epochSeconds },
        colorsMap = managerSupportedColors.associateWith { manager.colorFor(handleColor = it) },
        rectangles = rectangles,
        modifier = modifier
    )
}

@Composable
private fun RatingGraphCanvas(
    ratingPoints: List<Point>,
    translator: CoordinateTranslator,
    selectedPoint: Point?,
    markVerticals: List<Long>,
    colorsMap: Map<HandleColor, Color>,
    rectangles: RatingGraphRectangles,
    modifier: Modifier = Modifier,
    circleRadius: Float = 6f,
    circleBorderWidth: Float = 3f,
    pathWidth: Float = 4f,
    shadowOffset: Offset = Offset(4f, 4f),
    shadowAlpha: Float = 0.3f,
    selectedPointScale: Float = 1.5f
) {
    fun radius(point: Point, radius: Float) =
        if (selectedPoint == point) selectedPointScale * radius else radius

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        translator.canvasSize = size
        translator.borderX = circleRadius * 5

        val ratingPath = Path().apply {
            ratingPoints.forEachIndexed { index, point ->
                val (px, py) = translator.pointToOffset(point)
                if (index == 0) moveTo(px, py)
                else lineTo(px, py)
            }
        }

        //rating filled areas
        rectangles.forEach { point, handleColor ->
            val (px, py) = translator.pointToOffset(point).let {
                round(it.x) to round(it.y)
            }
            drawRect(
                color = colorsMap.getValue(handleColor),
                topLeft = Offset.Zero,
                size = Size(px, py)
            )
        }

        //time dashes
        if (selectedPoint != null) {
            val p = translator.pointToOffset(selectedPoint)
            drawLine(
                color = Color.Black,
                start = Offset(p.x, 0f),
                end = p,
                pathEffect = dashEffect
            )
        } else {
            markVerticals.forEach { x ->
                val p = translator.pointToOffset(x, 0)
                drawLine(
                    color = Color.Black,
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, size.height),
                    pathEffect = dashEffect
                )
            }
        }

        //shadow
        translate(left = shadowOffset.x, top = shadowOffset.y) {
            //shadow of rating path
            drawPath(
                path = ratingPath,
                color = Color.Black,
                style = Stroke(width = pathWidth),
                alpha = shadowAlpha
            )
            //shadow of rating points
            ratingPoints.forEach { point ->
                val center = translator.pointToOffset(point)
                drawCircle(
                    color = Color.Black,
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
            color = Color.Black,
            style = Stroke(width = pathWidth)
        )

        //rating points
        rectangles.iterateWithHandleColor(ratingPoints) { point, handleColor ->
            val center = translator.pointToOffset(point)
            drawCircle(
                color = Color.Black,
                radius = radius(point, circleRadius + circleBorderWidth),
                center = center,
                style = Fill
            )
            drawCircle(
                color = colorsMap.getValue(handleColor),
                radius = radius(point, circleRadius),
                center = center,
                style = Fill
            )
            if (point == selectedPoint) {
                drawCircle(
                    color = Color.Black,
                    radius = radius(point, circleRadius / 2),
                    center = center,
                    style = Fill
                )
            }
        }
    }
}

internal data class Point(val x: Long, val y: Long)

internal fun RatingChange.toPoint() = Point(x = date.epochSeconds, y = rating.toLong())