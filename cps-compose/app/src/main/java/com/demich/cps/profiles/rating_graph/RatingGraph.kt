package com.demich.cps.profiles.rating_graph

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demich.cps.platforms.clients.niceMessage
import com.demich.cps.profiles.RatingChange
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.TextButtonsSelectRow
import com.demich.cps.ui.geom.RectProjector
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.FetchState
import com.demich.cps.utils.getSystemTime
import com.demich.cps.utils.minOfWithIndex
import com.demich.kotlin_stdlib_boost.partitionIndex
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant


internal enum class RatingFilterType {
    ALL,
    LAST_10,
    LAST_MONTH,
    LAST_YEAR;

    val title: String get() = name.lowercase().replace('_', ' ')
}

//TODO: info comes from different sources (rating: vm, time: remember, selected: rememberSaveable)
// move selected to vm

@Composable
fun RatingGraph(
    ratingChanges: () -> FetchState<List<RatingChange>>,
    onRetry: () -> Unit,
    manager: RatedProfileManager<*>,
    modifier: Modifier = Modifier,
    graphHeight: Dp = 240.dp,
    shape: Shape = RoundedCornerShape(5.dp)
) {
    LoadingContentBox(
        fetchState = ratingChanges,
        onRetry = onRetry,
        failedText = { it.niceMessage ?: "Failed to get rating history" },
        modifier = modifier
            .heightIn(min = graphHeight)
            .fillMaxWidth()
            .clip(shape)
            .background(cpsColors.backgroundAdditional)
    ) { ratingChanges ->
        if (ratingChanges.isEmpty()) {
            Text(text = "Rating history is empty")
        } else {
            RatingGraphWithHeader(
                ratingChanges = ratingChanges,
                manager = manager,
                graphHeight = graphHeight,
                shape = shape
            )
        }
    }
}

@Composable
private fun RatingGraphWithHeader(
    ratingChanges: List<RatingChange>,
    manager: RatedProfileManager<*>,
    shape: Shape,
    graphHeight: Dp
) {
    require(ratingChanges.isNotEmpty())

    val viewPortState = rememberViewPortState(inflateHorizontal = 10.dp)
    val rectangles = remember(manager) { RatingGraphRectangles(manager) }

    // TODO: carefully change to provide current time
    val currentTime = remember { getSystemTime() }

    //TODO: reset on ratings changes
    var filterType: RatingFilterType by rememberSaveable {
        viewPortState.setViewPort(
            createBounds(ratingChanges = ratingChanges, filterType = ALL, now = currentTime)
        )
        mutableStateOf(ALL)
    }

    //TODO: reset to null on ratings changes
    var selectedIndex: Int? by rememberSaveable {
        mutableStateOf(null)
    }

    val markVerticals: List<Instant> = remember(filterType, ratingChanges, currentTime) {
        if (filterType == ALL || ratingChanges.size < 2) emptyList()
        else {
            createBounds(ratingChanges, filterType, currentTime).run {
                listOf(startTime, endTime)
            }
        }
    }

    val animationScope = rememberCoroutineScope()

    Column(modifier = Modifier.background(cpsColors.background)) {
        RatingGraphHeader(
            manager = manager,
            header = selectedIndex?.let { RatingChangeHeader(ratingChanges.getProperly(it), rectangles) }
                ?: FilterHeader(filterType, ratingChanges, currentTime),
            onHeaderChange = { header ->
                if (header is FilterHeader) {
                    animationScope.launch {
                        viewPortState.animateToViewPort(
                            createBounds(ratingChanges = ratingChanges, filterType = header.filterType, now = currentTime)
                        )
                    }
                    filterType = header.filterType
                }
            },
            shape = shape
        )

        RatingGraphCanvas(
            ratingChanges = ratingChanges,
            manager = manager,
            rectangles = rectangles,
            viewPortState = viewPortState,
            markVerticals = markVerticals,
            selectedIndex = selectedIndex,
            modifier = Modifier
                .height(graphHeight)
                .clip(shape)
                .pointerInput(viewPortState) {
                    viewPortState.detectTransformGestures(
                        minWidth = 1.hours,
                        minHeight = 1
                    )
                }
                .pointerInput(viewPortState, ratingChanges) {
                    detectTapGestures { tapPoint ->
                        viewPortState.projectorToCanvas {
                            selectedIndex = ratingChanges.indexOfClosestOrNull(
                                tap = tapPoint,
                                tapRadius = 24.dp.toPx()
                            )
                        }
                    }
                }
        )
    }
}

context(projector: RectProjector)
private fun List<RatingChange>.indexOfClosestOrNull(
    tap: Offset,
    tapRadius: Float
): Int? =
    minOfWithIndex {
        val o = it.toGraphPoint().toCanvasPoint()
        (o - tap).getDistance()
    }.takeIf { it.value <= tapRadius }?.index

private fun List<RatingChange>.getProperly(index: Int): RatingChange {
    val res = get(index)
    if (index == 0 || res.oldRating != null) return res
    return res.copy(oldRating = get(index-1).rating)
}

private fun ViewPortState.setViewPort(bounds: RatingGraphBounds) {
    setViewPort(rect = bounds.toViewPortRect())
}

private suspend fun ViewPortState.animateToViewPort(bounds: RatingGraphBounds) {
    animateToViewPort(targetRect = bounds.toViewPortRect())
}

private fun RatingGraphBounds.toViewPortRect(): Rect =
    fixTimeWidth(border = 1.days).addRatingBorder(border = 100)
        .apply { require(startTime < endTime) }
        .toGraphRect()

private fun RatingGraphBounds.fixTimeWidth(border: Duration) =
    if (startTime == endTime) {
        copy(
            startTime = startTime - border,
            endTime = endTime + border
        )
    } else {
        this
    }

private fun RatingGraphBounds.addRatingBorder(border: Int) =
    copy(
        minRating = minRating - border,
        maxRating = maxRating + border
    )

private sealed interface Header

private class RatingChangeHeader(
    val ratingChange: RatingChange,
    val rectangles: RatingGraphRectangles
): Header

private data class FilterHeader(
    val filterType: RatingFilterType,
    val ratingChanges: List<RatingChange>,
    val time: Instant
): Header

@Composable
private fun RatingGraphHeader(
    manager: RatedProfileManager<*>,
    header: Header,
    onHeaderChange: (Header) -> Unit,
    shape: Shape
) {
    when (header) {
        is RatingChangeHeader -> {
            ContestResult(
                ratingChange = header.ratingChange,
                manager = manager,
                rectangles = header.rectangles,
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .background(cpsColors.backgroundAdditional, shape)
                    .padding(all = 5.dp)
                    .fillMaxWidth()
            )
        }
        is FilterHeader -> {
            check(header.ratingChanges.isNotEmpty())
            TextButtonsSelectRow(
                values = remember(header.ratingChanges, header.time) {
                    makeValidFilters(header.ratingChanges, header.time)
                },
                selectedValue = header.filterType,
                text = RatingFilterType::title,
                onSelect = { onHeaderChange(header.copy(filterType = it)) },
                modifier = Modifier.background(cpsColors.background)
            )
        }
    }
}

private fun makeValidFilters(ratingChanges: List<RatingChange>, currentTime: Instant): List<RatingFilterType> =
    buildList {
        if (ratingChanges.size > 10) add(LAST_10)
        val firstInMonth = ratingChanges.partitionIndex { it.date < currentTime - 30.days }
        val firstInYear = ratingChanges.partitionIndex { it.date < currentTime - 365.days }
        if (firstInMonth < ratingChanges.size && firstInMonth > 0) {
            add(LAST_MONTH)
        }
        if (firstInYear < ratingChanges.size && firstInYear > 0 && firstInYear != firstInMonth) {
            add(LAST_YEAR)
        }
        if (isNotEmpty()) add(index = 0, ALL)
    }

