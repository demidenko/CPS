package com.demich.cps.accounts.rating_graph

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.TextButtonsSelectRow
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.getCurrentXTime
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.saver
import com.demich.kotlin_stdlib_boost.partitionIndex
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days


internal enum class RatingFilterType {
    ALL,
    LAST_10,
    LAST_MONTH,
    LAST_YEAR;

    val title: String get() = name.lowercase().replace('_', ' ')
}

//TODO: info comes from different sources (rating: vm, time: remember, selected: rememberSaveable)

@Composable
fun RatingGraph(
    ratingChangesResult: () -> Result<List<RatingChange>>?,
    onRetry: () -> Unit,
    manager: RatedAccountManager<out RatedUserInfo>,
    modifier: Modifier = Modifier,
    graphHeight: Dp = 240.dp,
    shape: Shape = RoundedCornerShape(5.dp)
) {
    LoadingContentBox(
        dataResult = ratingChangesResult,
        onRetry = onRetry,
        failedText = { "Failed to get rating history" },
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
    manager: RatedAccountManager<out RatedUserInfo>,
    shape: Shape,
    graphHeight: Dp
) {
    require(ratingChanges.isNotEmpty())

    //TODO: saveables not reset after ratings changes
    val translator = rememberCoordinateTranslator()

    val currentTime = remember { getCurrentXTime() }
    var filterType by rememberSaveable {
        translator.setWindow(
            createBounds(ratingChanges = ratingChanges, filterType = RatingFilterType.ALL, now = currentTime)
        )
        mutableStateOf(RatingFilterType.ALL)
    }

    var selectedRatingChange: RatingChange?
            by rememberSaveable(stateSaver = jsonCPS.saver()) { mutableStateOf(null) }

    val rectangles = remember(manager.type) { RatingGraphRectangles(manager) }

    Column(modifier = Modifier.background(cpsColors.background)) {
        RatingGraphHeader(
            manager = manager,
            header = selectedRatingChange
                ?.let { RatingChangeHeader(it, rectangles) }
                ?: FilterHeader(filterType, ratingChanges, currentTime),
            onHeaderChange = { header ->
                if (header is FilterHeader) {
                    translator.setWindow(
                        createBounds(ratingChanges = ratingChanges, filterType = header.filterType, now = currentTime)
                    )
                    filterType = header.filterType
                }
            },
            shape = shape
        )

        RatingGraphCanvas(
            ratingChanges = ratingChanges,
            manager = manager,
            rectangles = rectangles,
            translator = translator,
            currentTime = currentTime,
            filterType = filterType,
            selectedRatingChange = selectedRatingChange,
            modifier = Modifier
                .height(graphHeight)
                .clip(shape)
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
    manager: RatedAccountManager<out RatedUserInfo>,
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

private fun makeValidFilters(ratingChanges: List<RatingChange>, currentTime: Instant) =
    buildList {
        if (ratingChanges.size > 10) add(RatingFilterType.LAST_10)
        val firstInMonth = ratingChanges.partitionIndex { it.date < currentTime - 30.days }
        val firstInYear = ratingChanges.partitionIndex { it.date < currentTime - 365.days }
        if (firstInMonth < ratingChanges.size && firstInMonth > 0) {
            add(RatingFilterType.LAST_MONTH)
        }
        if (firstInYear < ratingChanges.size && firstInYear > 0 && firstInYear != firstInMonth) {
            add(RatingFilterType.LAST_YEAR)
        }
        if (isNotEmpty()) add(index = 0, RatingFilterType.ALL)
    }

