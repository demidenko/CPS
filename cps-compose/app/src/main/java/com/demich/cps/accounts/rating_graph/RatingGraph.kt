package com.demich.cps.accounts.rating_graph

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
import androidx.compose.ui.graphics.Shape
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
                getRatingHistory(userInfo.handle)
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
            resultRatingChanges = ratingGraphUIStates.ratingChanges,
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

    val title: String get() = name.lowercase().replace('_', ' ')
}

@Composable
private fun RatingGraph(
    loadingStatus: LoadingStatus,
    resultRatingChanges: List<RatingChange>,
    manager: RatedAccountManager<out RatedUserInfo>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(5.dp)
) {
    RatingGraph(
        ratingChangesResult = {
            if (loadingStatus == LoadingStatus.LOADING) null
            else runCatching {
                require(loadingStatus == LoadingStatus.PENDING)
                resultRatingChanges
            }
        },
        manager, modifier, shape
    )
}

@Composable
fun RatingGraph(
    ratingChangesResult: () -> Result<List<RatingChange>>?,
    manager: RatedAccountManager<out RatedUserInfo>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(5.dp)
) {
    //TODO: find good solution instead of this
    var header: @Composable () -> Unit by remember { mutableStateOf({}) }

    Column(modifier = modifier) {
        header()

        LoadingContentBox(
            dataResult = ratingChangesResult,
            failedText = { "Failed to get rating history" },
            modifier = Modifier
                .height(240.dp)
                .fillMaxWidth()
                .background(cpsColors.backgroundAdditional, shape)
                .clip(shape)
        ) { ratingChanges ->
            if (ratingChanges.isEmpty()) {
                Text(text = "Rating history is empty")
            } else {
                RatingGraph(
                    ratingChanges = ratingChanges,
                    manager = manager,
                    setHeader = { header = it },
                    shape = shape
                )
            }
        }
    }
}

@Composable
private fun RatingGraph(
    ratingChanges: List<RatingChange>,
    manager: RatedAccountManager<out RatedUserInfo>,
    setHeader: (@Composable () -> Unit) -> Unit,
    shape: Shape
) {
    require(ratingChanges.isNotEmpty())

    //TODO: saveables not reset after ratings changes
    val translator = rememberCoordinateTranslator()

    val currentTime = remember { getCurrentTime() }
    var filterType by rememberSaveable {
        translator.setWindow(
            createBounds(ratingChanges = ratingChanges, filterType = RatingFilterType.ALL, now = currentTime)
        )
        mutableStateOf(RatingFilterType.ALL)
    }

    var selectedRatingChange: RatingChange?
            by rememberSaveable(stateSaver = jsonCPS.saver()) { mutableStateOf(null) }

    val rectangles = remember(manager.type) { RatingGraphRectangles(manager) }

    setHeader {
        RatingGraphHeader(
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
    }

    RatingGraphCanvas(
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

@Composable
private fun RatingGraphHeader(
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
    } else {
        require(ratingChanges.isNotEmpty())
        TextButtonsSelectRow(
            values = remember(ratingChanges, currentTime) {
                makeValidFilters(ratingChanges, currentTime)
            },
            selectedValue = selectedFilterType,
            text = RatingFilterType::title,
            onSelect = onSelectFilterType,
            modifier = Modifier.background(cpsColors.background)
        )
    }
}

private fun makeValidFilters(ratingChanges: List<RatingChange>, currentTime: Instant) =
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

