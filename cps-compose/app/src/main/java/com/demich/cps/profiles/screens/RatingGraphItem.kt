package com.demich.cps.profiles.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.demich.cps.profiles.RatingChange
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.managers.getRatingHistory
import com.demich.cps.profiles.rating_graph.RatingGraph
import com.demich.cps.utils.backgroundFetchStateHost
import com.demich.cps.utils.rememberUUIDState
import com.sebaslogen.resaca.viewModelScoped

@Composable
internal fun RatingGraphItem(
    manager: RatedProfileManager<*>,
    handle: String,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModelScoped { RatingLoadingViewModel() }
    val uuidState = rememberUUIDState()

    val ratingChanges by viewModel
        .flowOfFetchRatingHistory(manager = manager, userId = handle, key = uuidState.value)
        .collectAsState()

    RatingGraph(
        modifier = modifier,
        manager = manager,
        ratingChanges = ratingChanges,
        onRetry = uuidState::reset
    )
}

private class RatingLoadingViewModel: ViewModel() {

    private val host = backgroundFetchStateHost<List<RatingChange>>()

    fun flowOfFetchRatingHistory(manager: RatedProfileManager<*>, userId: String, key: Any) =
        host.execute(key = Triple(manager.platform, userId, key)) {
            manager.getRatingHistory(userId)
        }
}