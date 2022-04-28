package com.demich.cps.news

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.CPSMenuBuilder
import com.demich.cps.Screen
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
    CPSDropdownMenuItem(title = "Settings", icon = Icons.Default.Settings) {
        navController.navigate(Screen.NewsSettings.route)
    }
    CPSDropdownMenuItem(title = "Follow List", icon = Icons.Rounded.PeopleAlt) {
        //TODO Open FollowList
    }
}