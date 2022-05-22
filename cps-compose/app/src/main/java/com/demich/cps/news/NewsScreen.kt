package com.demich.cps.news

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.news.codeforces.CodeforcesNewsBottomBar
import com.demich.cps.news.codeforces.CodeforcesNewsController
import com.demich.cps.news.codeforces.CodeforcesNewsScreen
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context

@Composable
fun NewsScreen(
    navigator: CPSNavigator,
    controller: CodeforcesNewsController
) {
    CodeforcesNewsScreen(
        navigator = navigator,
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
    navigator: CPSNavigator,
    controller: CodeforcesNewsController
): CPSMenuBuilder = {
    val loadingStatus by controller.rememberLoadingStatusState()
    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
        enabled = loadingStatus != LoadingStatus.LOADING,
        onClick = { navigator.navigateTo(Screen.NewsSettings) }
    )
    //CPSDropdownMenuItem(title = "Follow List", icon = CPSIcons.Accounts) { }
}