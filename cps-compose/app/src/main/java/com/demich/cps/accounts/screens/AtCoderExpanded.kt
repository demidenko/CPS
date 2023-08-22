package com.demich.cps.accounts.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.accountsViewModel
import com.demich.cps.accounts.managers.AtCoderAccountManager
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.userinfo.AtCoderUserInfo
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder

@Composable
fun AtCoderUserInfoExpandedContent(
    userInfo: AtCoderUserInfo,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
    modifier: Modifier
) {
    val manager = remember { AtCoderAccountManager() }
    val accountsViewModel = accountsViewModel()

    var showRatingGraph by rememberSaveable { mutableStateOf(false) }
    val ratingDataKey = currentCompositeKeyHash

    Box(modifier = modifier) {
        manager.SmallRatedAccountPanel(userInfo)
        if (showRatingGraph) {
            val ratingChangesResult by accountsViewModel
                .flowOfRatingResult(manager, userInfo.userId, key = ratingDataKey)
                .collectAsState()
            RatingGraph(
                ratingChangesResult = { ratingChangesResult },
                manager = manager,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
    setBottomBarContent {
        if (userInfo.hasRating()) {
            CPSIconButton(
                icon = CPSIcons.RatingGraph,
                enabled = !showRatingGraph,
                onClick = { showRatingGraph = true }
            )
        }
    }
}