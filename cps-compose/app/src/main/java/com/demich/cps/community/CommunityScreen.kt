package com.demich.cps.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.demich.cps.community.codeforces.CodeforcesCommunityBottomBar
import com.demich.cps.community.codeforces.CodeforcesCommunityController
import com.demich.cps.community.codeforces.CodeforcesCommunityScreen
import com.demich.cps.community.codeforces.loadingStatusState
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context

@Composable
fun CommunityScreen(
    controller: CodeforcesCommunityController
) {
    CodeforcesCommunityScreen(
        controller = controller
    )
}


fun communityBottomBarBuilder(
    controller: CodeforcesCommunityController
): AdditionalBottomBarBuilder = {
    val context = context

    CodeforcesCommunityBottomBar(controller = controller)

    val loadingStatus by controller.loadingStatusState()
    CPSReloadingButton(loadingStatus = loadingStatus) {
        controller.reload(titles = controller.visitedTabs, context = context)
    }
}

fun communityMenuBuilder(
    controller: CodeforcesCommunityController,
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
): CPSMenuBuilder = {
    val context = context

    val loadingStatus by controller.loadingStatusState()
    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
        enabled = loadingStatus != LoadingStatus.LOADING,
        onClick = onOpenSettings
    )

    val followEnabled by collectItemAsState { context.settingsCommunity.codeforcesFollowEnabled }
    if (followEnabled) {
        CPSDropdownMenuItem(title = "Follow List", icon = CPSIcons.Profiles) {
            controller.updateFollowUsersInfo(context)
            onOpenFollowList()
        }
    }
}