package com.demich.cps.accounts.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.managers.DmojAccountManager
import com.demich.cps.accounts.userinfo.DmojUserInfo
import com.demich.cps.accounts.userinfo.hasRating
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder

@Composable
fun DmojUserInfoExpandedContent(
    userInfo: DmojUserInfo,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
    modifier: Modifier
) {
    val manager = remember { DmojAccountManager() }

    var showRatingGraph by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        manager.SmallRatedAccountPanel(userInfo)
        if (showRatingGraph) {
            RatingGraphItem(
                manager = manager,
                userInfo = userInfo,
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