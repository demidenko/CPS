@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news.codeforces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.utils.context
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.runBlocking


@Composable
fun rememberCodeforcesNewsController(

): CodeforcesNewsController {
    val context = context

    val (tabs, defaultTab) = remember {
        with(context.settingsNews) {
            runBlocking {
                val tabs = listOf(
                    CodeforcesTitle.MAIN,
                    CodeforcesTitle.TOP,
                    CodeforcesTitle.RECENT,
                    //TODO CodeforcesTitle.LOST
                )
                val defaultTab = codeforcesDefaultTab()
                tabs to defaultTab
            }
        }
    }

    //TODO: save selected Tab instead of index
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(defaultTab)
    )

    return remember(pagerState, tabs) {
        CodeforcesNewsController(
            tabs = tabs,
            pagerState = pagerState
        )
    }
}

@Stable
data class CodeforcesNewsController(
    val tabs: List<CodeforcesTitle>,
    val pagerState: PagerState
) {
    val currentTab: CodeforcesTitle
        get() = tabs[pagerState.currentPage]

    val selectedTabIndex: Int
        get() = pagerState.currentPage
}