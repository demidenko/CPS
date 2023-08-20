package com.demich.cps.news

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.demich.cps.news.codeforces.CodeforcesNewsBottomBar
import com.demich.cps.news.codeforces.CodeforcesNewsController
import com.demich.cps.news.codeforces.CodeforcesNewsScreen
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

@Composable
fun NewsScreen(
    controller: CodeforcesNewsController
) {
    CodeforcesNewsScreen(
        controller = controller
    )
}


fun newsBottomBarBuilder(
    controller: CodeforcesNewsController
): AdditionalBottomBarBuilder = {
    val context = context

    CodeforcesNewsBottomBar(controller = controller)

    val loadingStatus by controller.rememberLoadingStatusState()
    CPSReloadingButton(loadingStatus = loadingStatus) {
        controller.reloadAll(context)
    }
}

fun newsMenuBuilder(
    controller: CodeforcesNewsController,
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
): CPSMenuBuilder = {
    val context = context

    val loadingStatus by controller.rememberLoadingStatusState()
    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
        enabled = loadingStatus != LoadingStatus.LOADING,
        onClick = onOpenSettings
    )

    val followEnabled by rememberCollect { context.settingsNews.codeforcesFollowEnabled.flow }
    if (followEnabled) {
        CPSDropdownMenuItem(title = "Follow List", icon = CPSIcons.Accounts) {
            controller.updateFollowUsersInfo(context)
            onOpenFollowList()
        }
    }
}