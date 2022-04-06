package com.demich.cps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    CPSIconButton(
        icon = Icons.Default.Timeline,
        enabled = !ratingGraphUIStates.showRatingGraphState.value,
        onState = !ratingGraphUIStates.showRatingGraphState.value
    ) {
        ratingGraphUIStates.loadingStatusState.value = LoadingStatus.LOADING
        ratingGraphUIStates.showRatingGraphState.value = true
        scope.launch {
            val ratingChanges = getRatingHistory(getSavedInfo())
            if (ratingChanges == null) {
                ratingGraphUIStates.loadingStatusState.value = LoadingStatus.FAILED
            } else {
                ratingGraphUIStates.loadingStatusState.value = LoadingStatus.PENDING
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        if (loadingStatus == LoadingStatus.PENDING) {
            val titles = listOf("all", "last 10", "last month", "last year")
            var selected by rememberSaveable { mutableStateOf(0) }
            TextButtonsSelectRow(
                modifier = Modifier.background(cpsColors.background),
                values = titles.indices.toList(),
                selectedValue = selected,
                text = { value -> titles[value] },
                onSelect = { value -> selected = value }
            )
        }
        Box(modifier = Modifier
            .height(250.dp)
            .fillMaxWidth()
            .background(cpsColors.backgroundAdditional),
            contentAlignment = Alignment.Center
        ) {
            when (loadingStatus) {
                LoadingStatus.LOADING -> CircularProgressIndicator(color = cpsColors.textColor, strokeWidth = 3.dp)
                LoadingStatus.FAILED -> Text(
                    text = "Error on loading rating history",
                    color = cpsColors.errorColor,
                    fontWeight = FontWeight.Bold
                )
                LoadingStatus.PENDING -> {
                    DrawRatingGraph(manager = manager, ratingChanges = ratingChanges)
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
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        drawRect(color = Color.Red, size = Size(this.size.width/2, this.size.height/2))
    }
}