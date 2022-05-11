package com.demich.cps.news

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.news.codeforces.CodeforcesNewsScreen
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset

@Composable
fun NewsScreen(navigator: CPSNavigator) {
    CodeforcesNewsScreen(navigator = navigator)
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun NewsTabRow(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabs: @Composable () -> Unit
) {
    TabRow(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp),
        selectedTabIndex = pagerState.currentPage,
        backgroundColor = cpsColors.background,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                color = cpsColors.colorAccent
            )
        },
        divider = { },
        tabs = tabs
    )
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