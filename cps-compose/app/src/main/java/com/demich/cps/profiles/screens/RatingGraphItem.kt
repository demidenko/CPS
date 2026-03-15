package com.demich.cps.profiles.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.demich.cps.profiles.managers.RatedProfileManager
import com.demich.cps.profiles.profilesViewModel
import com.demich.cps.profiles.rating_graph.RatingGraph
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState

@Composable
internal fun RatingGraphItem(
    manager: RatedProfileManager<*>,
    handle: String,
    modifier: Modifier = Modifier
) {
    val viewModel = profilesViewModel()
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