package com.demich.cps.accounts.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.demich.cps.accounts.accountsViewModel
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState

@Composable
internal fun<U: RatedUserInfo> RatingGraphItem(
    manager: RatedAccountManager<U>,
    userInfo: U,
    modifier: Modifier = Modifier
) {
    val accountsViewModel = accountsViewModel()
    var dataKey by rememberUUIDState

    val ratingChangesResult by accountsViewModel
        .flowOfRatingResult(manager, userInfo.userId, key = dataKey)
        .collectAsState()

    RatingGraph(
        modifier = modifier,
        manager = manager,
        ratingChangesResult = { ratingChangesResult },
        onRetry = { dataKey = randomUuid() }
    )
}