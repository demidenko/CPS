package com.demich.cps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.math.round
import kotlin.time.Duration.Companion.days


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
    return remember { RatingGraphUIStates(showRatingGraph, ratingLoadingStatus, ratingChanges) }
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
fun<U: UserInfo> RatedAccountManager<U>.RatingGraph(
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

private enum class RatingFilterType {
    all,
    last10,
    lastMonth,
    lastYear
}

private data class RatingGraphBounds(
    val minRating: Int,
    val maxRating: Int,
    val startTime: Instant,
    val endTime: Instant
)

private fun createBounds(
    ratingChanges: List<RatingChange>,
    startTime: Instant = ratingChanges.first().date,
    endTime: Instant = ratingChanges.last().date
) = RatingGraphBounds(
    minRating = ratingChanges.minOf { it.rating },
    maxRating = ratingChanges.maxOf { it.rating },
    startTime = startTime,
    endTime = endTime
)

private fun createBounds(
    ratingChanges: List<RatingChange>,
    filterType: RatingFilterType
) = when (filterType) {
    RatingFilterType.all -> createBounds(ratingChanges)
    RatingFilterType.last10 -> createBounds(ratingChanges.takeLast(10))
    RatingFilterType.lastMonth, RatingFilterType.lastYear -> {
        val now = getCurrentTime()
        val startTime = now - (if (filterType == RatingFilterType.lastMonth) 30.days else 365.days)
        createBounds(
            ratingChanges = ratingChanges.filter { it.date >= startTime },
            startTime = startTime,
            endTime = now
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
    val translator = rememberCoordinateTranslator()

    var filterType by rememberSaveable(ratingChanges) {
        if (ratingChanges.isNotEmpty()) {
            translator.setWindow(createBounds(ratingChanges, RatingFilterType.all))
        }
        mutableStateOf(RatingFilterType.all)
    }

    var selectedRatingChange: RatingChange?
        by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(null) }

    val rectangles = remember(manager.type) { RatingGraphRectangles(manager) }

    Column(
        modifier = modifier
    ) {
        RatingGraphHeader(
            loadingStatus = loadingStatus,
            ratingChanges = ratingChanges,
            manager = manager,
            rectangles = rectangles,
            selectedFilterType = filterType,
            onSelectFilterType = {
                translator.setWindow(createBounds(ratingChanges, it))
                filterType = it
            },
            selectedRatingChange = selectedRatingChange,
            shape = shape
        )

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
                        DrawRatingGraph(
                            ratingChanges = ratingChanges,
                            manager = manager,
                            rectangles = rectangles,
                            translator = translator,
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
    }
}

@Composable
private fun RatingGraphHeader(
    loadingStatus: LoadingStatus,
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out UserInfo>,
    rectangles: RatingGraphRectangles,
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
        )
    } else
    if (loadingStatus == LoadingStatus.PENDING && ratingChanges.isNotEmpty()) {
        TextButtonsSelectRow(
            values = remember(ratingChanges) {
                buildList {
                    add(RatingFilterType.all)
                    if (ratingChanges.size > 10) add(RatingFilterType.last10)
                    val now = getCurrentTime()
                    val firstInMonth = ratingChanges.indexOfFirst { it.date >= now - 30.days }
                    val firstInYear = ratingChanges.indexOfFirst { it.date >= now - 365.days }
                    if (firstInMonth > 0 && firstInMonth != -1) {
                        add(RatingFilterType.lastMonth)
                    }
                    if (firstInYear > 0 && firstInYear != -1 && firstInYear != firstInMonth) {
                        add(RatingFilterType.lastYear)
                    }
                }
            },
            selectedValue = selectedFilterType,
            text = {
                when (it) {
                    RatingFilterType.all -> "all"
                    RatingFilterType.last10 -> "last 10"
                    RatingFilterType.lastMonth -> "last month"
                    RatingFilterType.lastYear -> "last year"
                }
            },
            onSelect = onSelectFilterType,
            modifier = Modifier.background(cpsColors.background)
        )
    }
}

@Composable
private fun ContestResult(
    ratingChange: RatingChange,
    manager: RatedAccountManager<out UserInfo>,
    rectangles: RatingGraphRectangles,
    modifier: Modifier = Modifier,
    titleFontSize: TextUnit = 16.sp,
    subTitleFontSize: TextUnit = 12.sp,
    majorFontSize: TextUnit = 30.sp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = ratingChange.title, fontSize = titleFontSize)
            Text(
                text = buildAnnotatedString {
                    append(ratingChange.date.format("dd.MM.yyyy HH:mm"))
                    ratingChange.rank?.let {
                        append("  rank: $it")
                    }
                },
                fontSize = subTitleFontSize,
                color = cpsColors.textColorAdditional
            )
        }
        Column(
            modifier = Modifier.padding(start = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ratingChange.rating.toString(),
                fontSize = majorFontSize,
                fontWeight = FontWeight.Bold,
                color = manager.colorFor(handleColor = rectangles.getHandleColor(ratingChange.toPoint())),
            )
            if (ratingChange.oldRating != null) {
                val change = ratingChange.rating - ratingChange.oldRating
                Text(
                    text = signedToString(change),
                    fontSize = subTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (change < 0) cpsColors.errorColor else cpsColors.success,
                )
            }
        }

    }
}

@Composable
private fun<U: UserInfo> DrawRatingGraph(
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<U>,
    rectangles: RatingGraphRectangles,
    translator: CoordinateTranslator,
    filterType: RatingFilterType,
    selectedRatingChange: RatingChange?,
    modifier: Modifier = Modifier
) {
    val managerSupportedColors = remember(manager.type) {
        HandleColor.values().filter {
            runCatching { manager.originalColor(it) }.isSuccess
        }
    }

    val timeRange = remember(key1 = filterType, key2 = ratingChanges) {
        createBounds(ratingChanges, filterType).run { startTime to endTime }
    }

    DrawRatingGraph(
        ratingPoints = ratingChanges.map { it.toPoint() }.sortedBy { it.x },
        translator = translator,
        timeRange = timeRange,
        selectedPoint = selectedRatingChange?.toPoint(),
        colorsMap = managerSupportedColors.associateWith { manager.colorFor(handleColor = it) },
        rectangles = rectangles,
        modifier = modifier
    )
}

@Composable
private fun DrawRatingGraph(
    ratingPoints: List<Point>,
    translator: CoordinateTranslator,
    timeRange: Pair<Instant, Instant>,
    selectedPoint: Point?,
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
        translator.size = size
        translator.borderX = circleRadius * 5

        val ratingPath = Path().apply {
            ratingPoints.forEachIndexed { index, point ->
                val (px, py) = translator.pointToOffset(point)
                if (index == 0) moveTo(px, py)
                else lineTo(px, py)
            }
        }

        //rating filled areas
        rectangles.rectangles.forEach { (point, handleColor) ->
            val (px, py) = translator.pointToOffset(point).let {
                round(it.x) to round(it.y)
            }
            drawRect(
                color = colorsMap[handleColor]!!,
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
            listOf(timeRange.first, timeRange.second).forEach {
                val p = translator.pointToOffset(it.epochSeconds, 0)
                drawLine(
                    color = Color.Black,
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, size.height),
                    pathEffect = dashEffect
                )
            }
        }

        //shadow of rating path
        drawPath(
            path = Path().apply { addPath(ratingPath, shadowOffset) },
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
                center = center + shadowOffset,
                style = Fill,
                alpha = shadowAlpha
            )
        }

        //rating path
        drawPath(
            path = ratingPath,
            color = Color.Black,
            style = Stroke(width = pathWidth)
        )

        //rating points
        ratingPoints.forEach { point ->
            val center = translator.pointToOffset(point)
            drawCircle(
                color = Color.Black,
                radius = (circleRadius + circleBorderWidth) * radiusMultiplier(point),
                center = center,
                style = Fill
            )
            drawCircle(
                color = colorsMap[rectangles.getHandleColor(point)]!!,
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


@Composable
private fun rememberCoordinateTranslator(): CoordinateTranslator {
    val minXState = rememberSaveable { mutableStateOf(0f) }
    val maxXState = rememberSaveable { mutableStateOf(0f) }
    val minYState = rememberSaveable { mutableStateOf(0f) }
    val maxYState = rememberSaveable { mutableStateOf(0f) }
    return remember { CoordinateTranslator(minXState, maxXState, minYState, maxYState) }
}

private class CoordinateTranslator(
    minXState: MutableState<Float>,
    maxXState: MutableState<Float>,
    minYState: MutableState<Float>,
    maxYState: MutableState<Float>,
) {
    private var minY: Float by minYState
    private var maxY: Float by maxYState
    private var minX: Float by minXState
    private var maxX: Float by maxXState

    var size: Size = Size.Unspecified
    var borderX: Float = 0f

    fun setWindow(bounds: RatingGraphBounds) {
        if (bounds.startTime == bounds.endTime) {
            setWindow(bounds.copy(
                startTime = bounds.startTime - 1.days,
                endTime = bounds.endTime + 1.days
            ))
            return
        }
        with(bounds) {
            minY = minRating.toFloat() - 100f
            maxY = maxRating.toFloat() + 100f
            minX = startTime.epochSeconds.toFloat()
            maxX = endTime.epochSeconds.toFloat()
        }
    }

    private fun transformX(
        x: Float,
        fromWidth: Float,
        toWidth: Float
    ) = (x - fromWidth/2) * (fromWidth / toWidth) + (fromWidth / 2)

    fun pointToOffset(x: Long, y: Long) = Offset(
        x = transformX(
            x = (x - minX) / (maxX - minX) * size.width,
            fromWidth = size.width,
            toWidth = size.width + borderX * 2
        ),
        y = size.height - ((y - minY) / (maxY - minY) * size.height)
    )

    fun pointToOffset(point: Point) = pointToOffset(point.x, point.y)

    private fun offsetToPoint(offset: Offset) = Offset(
        x = transformX(
            x = offset.x,
            fromWidth = size.width + borderX * 2,
            toWidth = size.width
        ) / size.width * (maxX - minX) + minX,
        y = (size.height - offset.y) / size.height * (maxY - minY) + minY
    )

    fun move(offset: Offset) {
        val (dx, dy) = offsetToPoint(offset) - offsetToPoint(Offset.Zero)
        minX -= dx
        maxX -= dx
        minY -= dy
        maxY -= dy
    }

    fun scale(center: Offset, scale: Float) {
        val (cx, cy) = offsetToPoint(center)
        minX = (minX - cx) / scale + cx
        maxX = (maxX - cx) / scale + cx
        minY = (minY - cy) / scale + cy
        maxY = (maxY - cy) / scale + cy
    }

    fun getNearestRatingChange(
        ratingChanges: List<RatingChange>,
        tap: Offset,
        tapRadius: Float = 50f
    ): RatingChange? {
        var pos = -1
        var minDist = Float.POSITIVE_INFINITY
        for (i in ratingChanges.indices) {
            val o = pointToOffset(ratingChanges[i].toPoint())
            val dist = (o - tap).getDistance()
            if (dist <= tapRadius && dist < minDist) {
                pos = i
                minDist = dist
            }
        }
        if (pos == -1) return null
        val res = ratingChanges[pos]
        if (pos == 0 || res.oldRating != null) return res
        return res.copy(oldRating = ratingChanges[pos-1].rating)
    }
}

@Immutable
private class RatingGraphRectangles(
    manager: RatedAccountManager<out UserInfo>
) {
    val rectangles: List<Pair<Point,HandleColor>> = buildList {
        fun addBounds(bounds: Array<Pair<HandleColor, Int>>, x: Long) {
            bounds.sortedBy { it.second }.let { list ->
                for (i in list.indices) {
                    val y = if (i == 0) Int.MIN_VALUE else list[i-1].second
                    add(Point(x = x, y = y.toLong()) to list[i].first)
                }
                add(Point(x = x, y = list.last().second.toLong()) to HandleColor.RED)
            }
        }
        addBounds(x = Long.MAX_VALUE, bounds = manager.ratingsUpperBounds)
        if (manager is RatingRevolutionsProvider) {
            manager.ratingUpperBoundRevolutions
                .sortedByDescending { it.first }
                .forEach { (time, bounds) ->
                    addBounds(x = time.epochSeconds, bounds = bounds)
                }
        }
    }

    fun getHandleColor(point: Point): HandleColor =
        rectangles.last { (r, _) -> point.x < r.x && point.y >= r.y }.second

}

private data class Point(val x: Long, val y: Long)

private fun RatingChange.toPoint() = Point(x = date.epochSeconds, y = rating.toLong())
