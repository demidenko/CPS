package com.demich.cps.news

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.news.codeforces.CodeforcesNewsScreen
import com.demich.cps.news.codeforces.CodeforcesNewsViewModel
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.combine

@Composable
fun NewsScreen(
    navigator: CPSNavigator,
    codeforcesNewsViewModel: CodeforcesNewsViewModel
) {
    CodeforcesNewsScreen(
        navigator = navigator,
        viewModel = codeforcesNewsViewModel
    )
}


@Composable
fun NewsSettingsScreen() {

}

fun newsBottomBarBuilder(
    newsViewModel: CodeforcesNewsViewModel
): AdditionalBottomBarBuilder = {
    val loadingStatus by remember {
        derivedStateOf {
            listOf(CodeforcesTitle.MAIN).map {
                newsViewModel.pageLoadingStatusState(it).value
            }.combine()
        }
    }
    CPSReloadingButton(loadingStatus = loadingStatus) {
        newsViewModel.reload(CodeforcesTitle.MAIN, CodeforcesLocale.RU)
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