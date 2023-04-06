package com.demich.cps.accounts.rating_graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.*
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.TextButtonsSelectRow
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.saver
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.math.round
import kotlin.time.Duration.Companion.days


@Composable
fun<U: RatedUserInfo> RatedAccountManager<U>.RatingLoadButton(
    userInfo: U,
    ratingGraphUIStates: RatingGraphUIStates
) {
    val scope = rememberCoroutineScope()
    CPSIconButton(
        icon = CPSIcons.RatingGraph,
        enabled = !ratingGraphUIStates.showRatingGraph || ratingGraphUIStates.loadingStatus == LoadingStatus.FAILED
    ) {
        ratingGraphUIStates.loadingStatus = LoadingStatus.LOADING
        ratingGraphUIStates.showRatingGraph = true
        scope.launch {
            this@RatingLoadButton.runCatching {
                getRatingHistory(userInfo)
            }.onFailure {
                ratingGraphUIStates.loadingStatus = LoadingStatus.FAILED
            }.onSuccess {
                ratingGraphUIStates.ratingChanges = it
                ratingGraphUIStates.loadingStatus = LoadingStatus.PENDING
            }
        }
    }
}

@Composable
fun<U: RatedUserInfo> RatedAccountManager<U>.RatingGraph(
    ratingGraphUIStates: RatingGraphUIStates,
    modifier: Modifier = Modifier
) = RatingGraph(
    ratingGraphUIStates = ratingGraphUIStates,
    manager = this,
    modifier = modifier
)

@Composable
fun RatingGraph(
    ratingGraphUIStates: RatingGraphUIStates,
    manager: RatedAccountManager<out RatedUserInfo>,
    modifier: Modifier = Modifier
) {
    if (ratingGraphUIStates.showRatingGraph) {
        RatingGraph(
            loadingStatus = ratingGraphUIStates.loadingStatus,
            ratingChanges = ratingGraphUIStates.ratingChanges,
            manager = manager,
            modifier = modifier
        )
    }
}

internal enum class RatingFilterType {
    ALL,
    LAST_10,
    LAST_MONTH,
    LAST_YEAR;

    val title: String get() = when (this) {
        ALL -> "all"
        LAST_10 -> "last 10"
        LAST_MONTH -> "last month"
        LAST_YEAR -> "last year"
    }
}

@Composable
private fun RatingGraph(
    loadingStatus: LoadingStatus,
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out RatedUserInfo>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(5.dp)
) {
    val translator = rememberCoordinateTranslator()

    val currentTime = remember(ratingChanges) { getCurrentTime() }
    var filterType by rememberSaveable(ratingChanges) {
        if (ratingChanges.isNotEmpty()) {
            translator.setWindow(
                createBounds(ratingChanges = ratingChanges, filterType = RatingFilterType.ALL, now = currentTime)
            )
        }
        mutableStateOf(RatingFilterType.ALL)
    }

    var selectedRatingChange: RatingChange?
        by rememberSaveable(stateSaver = jsonCPS.saver()) { mutableStateOf(null) }

    val rectangles = remember(manager.type) { RatingGraphRectangles(manager) }

    Column(
        modifier = modifier
    ) {
        RatingGraphHeader(
            loadingStatus = loadingStatus,
            ratingChanges = ratingChanges,
            manager = manager,
            rectangles = rectangles,
            currentTime = currentTime,
            selectedFilterType = filterType,
            onSelectFilterType = {
                translator.setWindow(
                    createBounds(ratingChanges = ratingChanges, filterType = it, now = currentTime)
                )
                filterType = it
            },
            selectedRatingChange = selectedRatingChange,
            shape = shape
        )

        LoadingContentBox(
            loadingStatus = loadingStatus,
            failedText = "Failed to load rating history",
            modifier = Modifier
                .height(240.dp)
                .fillMaxWidth()
                .background(cpsColors.backgroundAdditional, shape)
                .clip(shape)
        ) {
            if (ratingChanges.isEmpty()) {
                Text(text = "Rating history is empty")
            } else {
                DrawRatingGraph(
                    ratingChanges = ratingChanges,
                    manager = manager,
                    rectangles = rectangles,
                    translator = translator,
                    currentTime = currentTime,
                    filterType = filterType,
                    selectedRatingChange = selectedRatingChange,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                translator.move(pan)
                                translator.scale(centroid, zoom)
                            }
                        }
                        .pointerInput(ratingChanges) {
                            detectTapGestures { tapPoint ->
                                selectedRatingChange = translator.getNearestRatingChange(
                                    ratingChanges = ratingChanges,
                                    tap = tapPoint
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun RatingGraphHeader(
    loadingStatus: LoadingStatus,
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out RatedUserInfo>,
    rectangles: RatingGraphRectangles,
    currentTime: Instant,
    selectedFilterType: RatingFilterType,
    onSelectFilterType: (RatingFilterType) -> Unit,
    selectedRatingChange: RatingChange?,
    shape: Shape
) {
    if (selectedRatingChange != null) {
        ContestResult(
            ratingChange = selectedRatingChange,
            manager = manager,
            rectangles = rectangles,
            modifier = Modifier
                .padding(bottom = 3.dp)
                .background(cpsColors.backgroundAdditional, shape)
                .padding(all = 5.dp)
                .fillMaxWidth()
        )
    } else
    if (loadingStatus == LoadingStatus.PENDING && ratingChanges.isNotEmpty()) {
        TextButtonsSelectRow(
            values = remember(ratingChanges, currentTime) {
                buildList {
                    if (ratingChanges.size > 10) add(RatingFilterType.LAST_10)
                    val firstInMonth = ratingChanges.indexOfFirst { it.date >= currentTime - 30.days }
                    val firstInYear = ratingChanges.indexOfFirst { it.date >= currentTime - 365.days }
                    if (firstInMonth != -1 && firstInMonth > 0) {
                        add(RatingFilterType.LAST_MONTH)
                    }
                    if (firstInYear != -1 && firstInYear > 0 && firstInYear != firstInMonth) {
                        add(RatingFilterType.LAST_YEAR)
                    }
                    if (isNotEmpty()) add(index = 0, RatingFilterType.ALL)
                }
            },
            selectedValue = selectedFilterType,
            text = RatingFilterType::title,
            onSelect = onSelectFilterType,
            modifier = Modifier.background(cpsColors.background)
        )
    }
}

@Composable
private fun DrawRatingGraph(
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
        HandleColor.values().filter {
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

    DrawRatingGraph(
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
private fun DrawRatingGraph(
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
    selectedPointRadiusMultiplier: Float = 1.5f
) {
    fun radiusMultiplier(point: Point) = if (selectedPoint == point) selectedPointRadiusMultiplier else 1f

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
                    radius = (circleRadius + circleBorderWidth) * radiusMultiplier(point),
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
                radius = (circleRadius + circleBorderWidth) * radiusMultiplier(point),
                center = center,
                style = Fill
            )
            drawCircle(
                color = colorsMap.getValue(handleColor),
                radius = circleRadius * radiusMultiplier(point),
                center = center,
                style = Fill
            )
            if (point == selectedPoint) {
                drawCircle(
                    color = Color.Black,
                    radius = (circleRadius / 2) * radiusMultiplier(point),
                    center = center,
                    style = Fill
                )
            }
        }
    }
}


internal data class Point(val x: Long, val y: Long)

internal fun RatingChange.toPoint() = Point(x = date.epochSeconds, y = rating.toLong())
