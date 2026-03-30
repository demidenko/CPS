package com.demich.cps.profiles.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.demich.cps.profiles.RatingChange
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.rating_graph.RatingGraph
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState
import com.sebaslogen.resaca.viewModelScoped

@Composable
internal fun RatingGraphItem(
    manager: RatedProfileManager<*>,
    handle: String,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModelScoped { RatingLoadingViewModel() }
    var dataKey by rememberUUIDState()

    val ratingChangesResult by viewModel
        .flowOfRatingResult(manager = manager, userId = handle, key = dataKey)
        .collectAsState()

    RatingGraph(
        modifier = modifier,
        manager = manager,
        ratingChangesResult = { ratingChangesResult },
        onRetry = { dataKey = randomUuid() }
    )
}

private class RatingLoadingViewModel: ViewModel() {

    private val loader = backgroundDataLoader<List<RatingChange>>()

    fun flowOfRatingResult(manager: RatedProfileManager<*>, userId: String, key: Any) =
        loader.execute(key = Triple(manager.platform, userId, key)) {
            manager.getRatingChangeHistory(userId)
        }
}