package com.demich.cps.accounts.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.accounts.managers.DmojAccountManager
import com.demich.cps.accounts.userinfo.DmojUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder

@Composable
context(manager: DmojAccountManager)
fun DmojUserInfoExpandedContent(
    profileResult: ProfileResult<DmojUserInfo>,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        manager.PanelContent(profileResult)

        if (profileResult is ProfileResult.Success) {
            val userInfo = profileResult.userInfo
            var showRatingGraph by rememberSaveable { mutableStateOf(false) }

            if (showRatingGraph) {
                RatingGraphItem(
                    manager = manager,
                    handle = userInfo.handle,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }

            setBottomBarContent {
                if (userInfo.rating != null) {
                    CPSIconButton(
                        icon = CPSIcons.RatingGraph,
                        enabled = !showRatingGraph,
                        onClick = { showRatingGraph = true }
                    )
                }
            }
        }
    }
}