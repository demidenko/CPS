package com.demich.cps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.jsonSaver
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant


@Stable
data class RatingGraphUIStates (
    val showRatingGraphState: MutableState<Boolean>,
    val loadingStatusState: MutableState<LoadingStatus>,
    val ratingChangesState: MutableState<List<RatingChange>>
)

@Composable
fun rememberRatingGraphUIStates(): RatingGraphUIStates {
    val showRatingGraph = rememberSaveable { mutableStateOf(false) }
    val ratingLoadingStatus = rememberSaveable { mutableStateOf(LoadingStatus.PENDING) }
    val ratingChanges = rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(emptyList<RatingChange>()) }
    return RatingGraphUIStates(showRatingGraph, ratingLoadingStatus, ratingChanges)
}

@Composable
fun<U: UserInfo> RatedAccountManager<U>.RatingLoadButton(
    ratingGraphUIStates: RatingGraphUIStates
) {
    val scope = rememberCoroutineScope()
    var showRatingGraph by ratingGraphUIStates.showRatingGraphState
    var loadingStatus by ratingGraphUIStates.loadingStatusState
    CPSIconButton(
        icon = Icons.Default.Timeline,
        enabled = !showRatingGraph || loadingStatus == LoadingStatus.FAILED
    ) {
        ratingGraphUIStates.loadingStatusState.value = LoadingStatus.LOADING
        showRatingGraph = true
        scope.launch {
            val ratingChanges = getRatingHistory(getSavedInfo())
            if (ratingChanges == null) {
                loadingStatus = LoadingStatus.FAILED
            } else {
                loadingStatus = LoadingStatus.PENDING
                ratingGraphUIStates.ratingChangesState.value = ratingChanges
            }
        }
    }
}


@Composable
fun RatingGraph(
    ratingGraphUIStates: RatingGraphUIStates,
    manager: RatedAccountManager<out UserInfo>,
    modifier: Modifier = Modifier
) {
    if (ratingGraphUIStates.showRatingGraphState.value) {
        RatingGraph(
            loadingStatus = ratingGraphUIStates.loadingStatusState.value,
            ratingChanges = ratingGraphUIStates.ratingChangesState.value,
            manager = manager,
            modifier = modifier
        )
    }
}

@Composable
private fun RatingGraph(
    loadingStatus: LoadingStatus,
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out UserInfo>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(5.dp)
) {
    Column(
        modifier = modifier
    ) {
        Box(modifier = Modifier
            .height(240.dp)
            .fillMaxWidth()
            .background(cpsColors.backgroundAdditional, shape)
            .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            when (loadingStatus) {
                LoadingStatus.LOADING -> CircularProgressIndicator(color = cpsColors.textColor, strokeWidth = 3.dp)
                LoadingStatus.FAILED -> Text(
                    text = "Failed to load rating history",
                    color = cpsColors.errorColor,
                    fontWeight = FontWeight.Bold
                )
                LoadingStatus.PENDING -> {
                    if (ratingChanges.isEmpty()) {
                        Text(text = "Rating history is empty")
                    } else {
                        val minRating = ratingChanges.minOf { it.rating } - 100
                        val maxRating = ratingChanges.maxOf { it.rating } + 100
                        val startTime = ratingChanges.minOf { it.date }
                        val endTime = ratingChanges.maxOf { it.date }
                        DrawRatingGraph(
                            manager = manager,
                            ratingChanges = ratingChanges,
                            minRating, maxRating, startTime, endTime
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun<U: UserInfo> DrawRatingGraph(
    manager: RatedAccountManager<U>,
    ratingChanges: List<RatingChange>,
    minRating: Int,
    maxRating: Int,
    startTime: Instant,
    endTime: Instant
) {
    val rectangles = remember(manager.type) {
        buildList {
            (manager.ratingsUpperBounds + Pair(HandleColor.RED, Int.MAX_VALUE))
                .sortedByDescending { it.second }
                .forEach {
                    add(PointWithColor(
                        x = Long.MAX_VALUE,
                        y = it.second.toLong(),
                        handleColor = it.first
                    ))
                }
            if (manager is RatingRevolutionsProvider) {
                manager.ratingUpperBoundRevolutions
                    .sortedByDescending { it.first }
                    .forEach { (time, bounds) ->
                        (bounds + Pair(HandleColor.RED, Int.MAX_VALUE))
                            .sortedByDescending { it.second }
                            .forEach {
                                add(PointWithColor(
                                    x = time.epochSeconds,
                                    y = it.second.toLong(),
                                    handleColor = it.first
                                ))
                            }
                    }
            }
        }
    }

    val managerSupportedColors = remember(manager.type) {
        HandleColor.values().filter {
            runCatching { manager.originalColor(it) }.isSuccess
        }
    }

    DrawRatingGraph(
        ratingChanges = ratingChanges.map { it.date.epochSeconds to it.rating.toLong() }.sortedBy { it.first },
        translator = CoordinateTranslator(
            rangeX = startTime.epochSeconds.toFloat() to endTime.epochSeconds.toFloat(),
            rangeY = minRating.toFloat() to maxRating.toFloat(),
        ),
        colorsMap = managerSupportedColors.associateWith { manager.colorFor(handleColor = it) },
        rectangles = rectangles
    )
}

@Composable
private fun DrawRatingGraph(
    ratingChanges: List<Pair<Long, Long>>,
    translator: CoordinateTranslator,
    colorsMap: Map<HandleColor, Color>,
    rectangles: List<PointWithColor>,
    circleRadius: Float = 6f,
    circleBorderWidth: Float = 3f,
    pathWidth: Float = 4f
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        scale(scaleX = 1f, scaleY = -1f) {
            //rating colors
            rectangles.forEach { (rx, ry, handleColor) ->
                val (px, py) = translator.pointToWindow(rx, ry, size)
                drawRect(
                    color = colorsMap[handleColor]!!,
                    topLeft = Offset.Zero,
                    size = Size(px, py)
                )
            }

            val ratingPath = Path().apply {
                ratingChanges.forEachIndexed { index, point ->
                    val (px, py) = translator.pointToWindow(point.first, point.second, size)
                    if (index == 0) moveTo(px, py)
                    else lineTo(px, py)
                }
            }

            //rating path
            drawPath(
                path = ratingPath,
                color = Color.Black,
                style = Stroke(width = pathWidth)
            )

            //rating points
            ratingChanges.forEach { (x, y) ->
                val coveredBy = rectangles.last { (rx, ry) -> x < rx && y < ry }
                val (px, py) = translator.pointToWindow(x, y, size)
                drawCircle(
                    color = Color.Black,
                    radius = circleRadius + circleBorderWidth,
                    center = Offset(px, py),
                    style = Fill
                )
                drawCircle(
                    color = colorsMap[coveredBy.handleColor]!!,
                    radius = circleRadius,
                    center = Offset(px, py),
                    style = Fill
                )
            }

        }
    }
}

@Immutable
private data class CoordinateTranslator(
    private val rangeX: Pair<Float, Float>,
    private val rangeY: Pair<Float, Float>,
) {
    fun pointToWindow(x: Long, y: Long, size: Size): Pair<Float, Float> {
        val px = (x - rangeX.first) / (rangeX.second - rangeX.first) * size.width
        val py = (y - rangeY.first) / (rangeY.second - rangeY.first) * size.height
        return Pair(px, py)
    }
}

private data class PointWithColor(
    val x: Long,
    val y: Long,
    val handleColor: HandleColor
)