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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.HandleColor
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.UserInfo
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.jsonSaver
import kotlinx.coroutines.launch


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
            .height(250.dp)
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
                        DrawRatingGraph(manager = manager, ratingChanges = ratingChanges)
                    }
                }
            }
        }
    }
}

@Composable
private fun<U: UserInfo> DrawRatingGraph(
    manager: RatedAccountManager<U>,
    ratingChanges: List<RatingChange>
) {
    require(ratingChanges.isNotEmpty())
    val minRating = ratingChanges.minOf { it.rating } - 100f
    val maxRating = ratingChanges.maxOf { it.rating } + 100f
    val ratingBounds = remember { manager.ratingsUpperBounds + Pair(HandleColor.RED, Int.MAX_VALUE) }
    val colorMap = ratingBounds.associate {
        it.first to manager.colorFor(handleColor = it.first)
    }
    Canvas(
        modifier = Modifier.fillMaxSize().clipToBounds()
    ) {
        translate(top = -minRating) {
            scale(scaleX = 1f, scaleY = size.height / (maxRating - minRating), pivot = Offset(0f, minRating)) {
                scale(scaleX = 1f, scaleY = -1f, pivot = Offset(x = 0f, y = (maxRating + minRating) / 2f)) {
                    ratingBounds.sortedByDescending { it.second }
                        .forEach { (handleColor, upperRating) ->
                            drawRect(
                                color = colorMap[handleColor]!!,
                                topLeft = Offset(x = 0f, y = 0f),
                                size = Size(width = size.width, height = upperRating.toFloat())
                            )
                        }
                }
            }
        }
    }
}