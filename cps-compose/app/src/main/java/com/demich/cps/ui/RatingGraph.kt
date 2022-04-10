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
import androidx.compose.ui.graphics.drawscope.scale
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

private enum class RatingFilterType {
    all,
    last10,
    lastMonth,
    lastYear
}

@Composable
private fun RatingGraph(
    loadingStatus: LoadingStatus,
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out UserInfo>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(5.dp)
) {
    val translator by remember { mutableStateOf(CoordinateTranslator()) }

    var filterType by remember { mutableStateOf(RatingFilterType.all) }
    var timeRange by remember {
        mutableStateOf(Instant.DISTANT_PAST .. Instant.DISTANT_FUTURE)
    }

    fun setFilterData(
        filteredRatingChanges: List<RatingChange>,
        startTime: Instant = filteredRatingChanges.first().date,
        endTime: Instant = filteredRatingChanges.last().date
    ) {
        translator.setWindow(
            minRating = filteredRatingChanges.minOf { it.rating },
            maxRating = filteredRatingChanges.maxOf { it.rating },
            startTime = startTime,
            endTime = endTime
        )
        timeRange = startTime .. endTime
    }

    var selectedRatingChange: RatingChange? by remember{ mutableStateOf(null) }

    //TODO: save translator somehow
    DisposableEffect(key1 = filterType, key2 = ratingChanges, effect = {
        if (ratingChanges.isNotEmpty())
            when (val type = filterType) {
                RatingFilterType.all ->
                    setFilterData(filteredRatingChanges = ratingChanges)
                RatingFilterType.last10 ->
                    setFilterData(filteredRatingChanges = ratingChanges.takeLast(10))
                RatingFilterType.lastMonth, RatingFilterType.lastYear -> {
                    val now = getCurrentTime()
                    val startTime = now - (if (type == RatingFilterType.lastMonth) 30.days else 365.days)
                    setFilterData(
                        filteredRatingChanges = ratingChanges.filter { it.date >= startTime },
                        startTime = startTime,
                        endTime = now
                    )
                }
            }
        onDispose {  }
    })

    Column(
        modifier = modifier
    ) {
        RatingGraphHeader(
            loadingStatus = loadingStatus,
            ratingChanges = ratingChanges,
            manager = manager,
            selectedFilterType = filterType,
            onSelectFilterType = { filterType = it },
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
                            manager = manager,
                            ratingChanges = ratingChanges,
                            translator = translator,
                            timeRange = timeRange,
                            selectedRatingChange = selectedRatingChange,
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        translator.move(pan)
                                        translator.scale(centroid, zoom)
                                    }
                                }
                                .pointerInput(Unit) {
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
    selectedFilterType: RatingFilterType,
    onSelectFilterType: (RatingFilterType) -> Unit,
    selectedRatingChange: RatingChange?,
    shape: Shape
) {
    if (selectedRatingChange != null) {
        ContestResult(
            ratingChange = selectedRatingChange,
            manager = manager,
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
                //TODO: color with revolutions
                color = manager.colorFor(rating = ratingChange.rating),
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
    manager: RatedAccountManager<U>,
    ratingChanges: List<RatingChange>,
    translator: CoordinateTranslator,
    timeRange: ClosedRange<Instant>,
    selectedRatingChange: RatingChange?,
    modifier: Modifier = Modifier
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
        ratingChanges = ratingChanges.map { it.toLongPoint() }.sortedBy { it.first },
        translator = translator,
        timeRange = timeRange,
        selectedRatingChange = selectedRatingChange?.toLongPoint(),
        colorsMap = managerSupportedColors.associateWith { manager.colorFor(handleColor = it) },
        rectangles = rectangles,
        modifier = modifier
    )
}

@Composable
private fun DrawRatingGraph(
    ratingChanges: List<Pair<Long, Long>>,
    translator: CoordinateTranslator,
    timeRange: ClosedRange<Instant>,
    selectedRatingChange: Pair<Long, Long>?,
    colorsMap: Map<HandleColor, Color>,
    rectangles: List<PointWithColor>,
    modifier: Modifier = Modifier,
    circleRadius: Float = 6f,
    circleBorderWidth: Float = 3f,
    pathWidth: Float = 4f,
    shadowOffset: Offset = Offset(4f, -4f),
    shadowAlpha: Float = 0.3f
) {
    fun radiusMultiplier(x: Long, y: Long) =
        if (selectedRatingChange != null && x to y == selectedRatingChange) 1.5f else 1f

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        translator.size = size
        scale(scaleX = 1f, scaleY = -1f) {
            val ratingPath = Path().apply {
                ratingChanges.forEachIndexed { index, point ->
                    val (px, py) = translator.pointToWindow(point.first, point.second)
                    if (index == 0) moveTo(px, py)
                    else lineTo(px, py)
                }
            }

            //rating filled areas
            rectangles.forEach { (rx, ry, handleColor) ->
                val (px, py) = translator.pointToWindow(rx, ry)
                drawRect(
                    color = colorsMap[handleColor]!!,
                    topLeft = Offset.Zero,
                    size = Size(round(px), round(py))
                )
            }

            //time dashes
            if (selectedRatingChange != null) {
                val p = selectedRatingChange.let { translator.pointToWindow(it.first, it.second) }
                drawLine(
                    color = Color.Black,
                    start = Offset(p.x, size.height),
                    end = p,
                    pathEffect = dashEffect
                )
            } else {
                listOf(timeRange.start, timeRange.endInclusive).forEach {
                    val x = translator.pointToWindow(it.epochSeconds, 0).x
                    drawLine(
                        color = Color.Black,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
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
            ratingChanges.forEach { (x, y) ->
                val center = translator.pointToWindow(x, y)
                drawCircle(
                    color = Color.Black,
                    radius = (circleRadius + circleBorderWidth) * radiusMultiplier(x, y),
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
            ratingChanges.forEach { (x, y) ->
                val coveredBy = rectangles.last { (rx, ry) -> x < rx && y < ry }
                val center = translator.pointToWindow(x, y)
                drawCircle(
                    color = Color.Black,
                    radius = (circleRadius + circleBorderWidth) * radiusMultiplier(x, y),
                    center = center,
                    style = Fill
                )
                drawCircle(
                    color = colorsMap[coveredBy.handleColor]!!,
                    radius = circleRadius * radiusMultiplier(x, y),
                    center = center,
                    style = Fill
                )
            }

        }
    }
}

private class CoordinateTranslator {
    private var minY: Float by mutableStateOf(0f)
    private var maxY: Float by mutableStateOf(0f)
    private var minX: Float by mutableStateOf(0f)
    private var maxX: Float by mutableStateOf(0f)
    var size: Size = Size.Unspecified

    fun setWindow(
        minRating: Int,
        maxRating: Int,
        startTime: Instant,
        endTime: Instant
    ) {
        if (startTime == endTime) {
            setWindow(minRating, maxRating, startTime - 1.days, endTime + 1.days)
            return
        }
        minY = minRating.toFloat() - 100f
        maxY = maxRating.toFloat() + 100f

        //TODO x insets
        minX = startTime.epochSeconds.toFloat()
        maxX = endTime.epochSeconds.toFloat()
    }

    fun pointToWindow(
        x: Long,
        y: Long,
        reverseY: Boolean = false
    ): Offset {
        val px = (x - minX) / (maxX - minX) * size.width
        val py = ((y - minY) / (maxY - minY) * size.height).let {
            if (reverseY) size.height - it else it
        }
        return Offset(px, py)
    }

    fun move(offset: Offset) {
        val dx = offset.x / size.width * (maxX - minX)
        minX -= dx
        maxX -= dx
        val dy = -offset.y / size.height * (maxY - minY)
        minY -= dy
        maxY -= dy
    }

    fun scale(center: Offset, scale: Float) {
        val cx = center.x / size.width * (maxX - minX) + minX
        val cy = center.y / size.height * (maxY - minY) + minY
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
            val o = ratingChanges[i].toLongPoint().let {
                pointToWindow(x = it.first, y = it.second, reverseY = true)
            }
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

private data class PointWithColor(
    val x: Long,
    val y: Long,
    val handleColor: HandleColor
)

private fun RatingChange.toLongPoint() = date.epochSeconds to rating.toLong()