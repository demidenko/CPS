package com.demich.cps.accounts.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.accountsViewModel
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.rating_graph.RatingGraph
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.theme.cpsColors

@Composable
fun CodeforcesUserInfoExpandedContent(
    userInfo: CodeforcesUserInfo,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
    modifier: Modifier
) {
    val manager = remember { CodeforcesAccountManager() }
    val accountsViewModel = accountsViewModel()

    var showRatingGraph by rememberSaveable { mutableStateOf(false) }
    val ratingDataKey = currentCompositeKeyHash

    Box(modifier = modifier) {
        Column {
            manager.SmallRatedAccountPanel(userInfo)
            if (userInfo.contribution != 0) {
                Contribution(contribution = userInfo.contribution)
            }
        }
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
        //TODO: upsolving list button (icon = Icons.Default.FitnessCenter)
        if (userInfo.hasRating()) {
            CPSIconButton(
                icon = CPSIcons.RatingGraph,
                enabled = !showRatingGraph
            ) {
                showRatingGraph = true
            }
        }
    }
}

@Composable
private fun Contribution(contribution: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "contribution:",
            color = cpsColors.contentAdditional,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 5.dp)
        )
        VotedRating(
            rating = contribution,
            fontSize = 20.sp
        )
    }
}