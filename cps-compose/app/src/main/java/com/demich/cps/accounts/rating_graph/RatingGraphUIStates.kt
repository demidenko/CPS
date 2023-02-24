package com.demich.cps.accounts.rating_graph

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.saver

@Stable
class RatingGraphUIStates (
    showRatingGraphState: MutableState<Boolean>,
    loadingStatusState: MutableState<LoadingStatus>,
    ratingChangesState: MutableState<List<RatingChange>>
) {
    var showRatingGraph by showRatingGraphState
    var loadingStatus by loadingStatusState
    var ratingChanges by ratingChangesState
}

@Composable
fun rememberRatingGraphUIStates(): RatingGraphUIStates {
    val showRatingGraph = rememberSaveable { mutableStateOf(false) }
    val ratingLoadingStatus = rememberSaveable { mutableStateOf(LoadingStatus.PENDING) }
    val ratingChanges = rememberSaveable(stateSaver = jsonCPS.saver()) {
        mutableStateOf(emptyList<RatingChange>())
    }
    return remember { RatingGraphUIStates(showRatingGraph, ratingLoadingStatus, ratingChanges) }
}