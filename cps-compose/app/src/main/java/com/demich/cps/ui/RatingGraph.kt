package com.demich.cps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.UserInfo
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.jsonSaver
import kotlinx.coroutines.launch


@Composable
fun createRatingStuffStates(): Triple<MutableState<Boolean>, MutableState<LoadingStatus>, MutableState<List<RatingChange>>> {
    val showRatingGraph = rememberSaveable { mutableStateOf(false) }
    val ratingLoadingStatus = rememberSaveable { mutableStateOf(LoadingStatus.PENDING) }
    val ratingChanges = rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(emptyList<RatingChange>()) }
    return Triple(showRatingGraph, ratingLoadingStatus, ratingChanges)
}

@Composable
fun<U: UserInfo> RatedAccountManager<U>.RatingLoadButton(
    showRatingGraphState: MutableState<Boolean>,
    loadingStatusState: MutableState<LoadingStatus>,
    ratingChangesState: MutableState<List<RatingChange>>
) {
    val scope = rememberCoroutineScope()
    CPSIconButton(
        icon = Icons.Default.Timeline,
        enabled = !showRatingGraphState.value,
        onState = !showRatingGraphState.value
    ) {
        loadingStatusState.value = LoadingStatus.LOADING
        showRatingGraphState.value = true
        scope.launch {
            val ratingChanges = getRatingHistory(getSavedInfo())
            if (ratingChanges == null) {
                loadingStatusState.value = LoadingStatus.FAILED
            } else {
                loadingStatusState.value = LoadingStatus.PENDING
                ratingChangesState.value = ratingChanges
            }
        }
    }
}

@Composable
fun RatingGraph(
    loadingStatus: LoadingStatus,
    ratingChanges: List<RatingChange>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row {
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = "all")
            }
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = "last 10")
            }
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = "last month")
            }
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = "last year")
            }
        }
        Box(modifier = Modifier
            .height(256.dp)
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
                    Text(text = "size = ${ratingChanges.size}")
                }
            }
        }
    }
}