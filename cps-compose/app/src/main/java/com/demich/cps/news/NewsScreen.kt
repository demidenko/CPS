package com.demich.cps.news

import androidx.compose.runtime.Composable
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.news.codeforces.CodeforcesNewsScreen
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.utils.LoadingStatus

@Composable
fun NewsScreen(navigator: CPSNavigator) {
    CodeforcesNewsScreen(navigator = navigator)
}


@Composable
fun NewsSettingsScreen() {

}

fun newsBottomBarBuilder(

): AdditionalBottomBarBuilder = {
    CPSReloadingButton(loadingStatus = LoadingStatus.PENDING) {

    }
}

fun newsMenuBuilder(
    navigator: CPSNavigator
): CPSMenuBuilder = {
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings) {
        navigator.navigateTo(Screen.NewsSettings)
    }
    //CPSDropdownMenuItem(title = "Follow List", icon = CPSIcons.Accounts) { }
}