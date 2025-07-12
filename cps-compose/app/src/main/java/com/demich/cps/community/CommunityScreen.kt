package com.demich.cps.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.demich.cps.community.codeforces.CodeforcesCommunityBottomBar
import com.demich.cps.community.codeforces.CodeforcesCommunityController
import com.demich.cps.community.codeforces.CodeforcesCommunityScreen
import com.demich.cps.community.codeforces.loadingStatusState
import com.demich.cps.community.codeforces.rememberCodeforcesCommunityController
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTitleState
import com.demich.cps.navigation.cpsScreenTitle
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context

@Composable
private fun CommunityScreen(
    controller: CodeforcesCommunityController
) {
    CodeforcesCommunityScreen(
        controller = controller
    )
}


private fun communityBottomBarBuilder(
    controller: CodeforcesCommunityController
): AdditionalBottomBarBuilder = {
    val context = context

    CodeforcesCommunityBottomBar(controller = controller)

    val loadingStatus by controller.loadingStatusState()
    CPSReloadingButton(loadingStatus = loadingStatus) {
        controller.reload(titles = controller.visitedTabs, context = context)
    }
}

private fun communityMenuBuilder(
    controller: CodeforcesCommunityController,
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
): CPSMenuBuilder = {
    val context = context

    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
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

@Composable
fun CPSNavigator.ScreenScope<Screen.Community>.NavContentCommunityScreen(
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
) {
    val controller = rememberCodeforcesCommunityController()
    CommunityScreen(controller = controller)

    menu = communityMenuBuilder(
        controller = controller,
        onOpenSettings = onOpenSettings,
        onOpenFollowList = onOpenFollowList
    )

    bottomBar = communityBottomBarBuilder(
        controller = controller
    )

    screenTitle = remember(controller) {
        ScreenTitleState {
            cpsScreenTitle("community", "codeforces", controller.currentTab.name)
        }
    }
}