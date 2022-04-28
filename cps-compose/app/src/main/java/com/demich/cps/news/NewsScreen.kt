package com.demich.cps.news

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.CPSMenuBuilder
import com.demich.cps.Screen
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.utils.LoadingStatus

@Composable
fun NewsScreen(navController: NavController) {

}

@Composable
fun NewsSettingsScreen() {

}

fun newsBottomBarBuilder()
: AdditionalBottomBarBuilder = {
    CPSReloadingButton(loadingStatus = LoadingStatus.PENDING) {

    }
}

fun newsMenuBuilder(navController: NavController)
: CPSMenuBuilder = {
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings) {
        navController.navigate(Screen.NewsSettings.route)
    }
    CPSDropdownMenuItem(title = "Follow List", icon = CPSIcons.Accounts) {
        //TODO Open FollowList
    }
}