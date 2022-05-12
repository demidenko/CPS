@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.Contest
import com.demich.cps.news.NewsTab
import com.demich.cps.news.NewsTabRow
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

val LocalCodeforcesAccountManager = compositionLocalOf<CodeforcesAccountManager> {
    throw IllegalAccessError()
}

@Composable
fun CodeforcesNewsScreen(
    navigator: CPSNavigator,
    viewModel: CodeforcesNewsViewModel
) {

    val context = context
    val manager = remember { CodeforcesAccountManager(context) }
    val settings = remember { context.settingsNews }

    val tabs = remember {
        mutableStateOf(
            listOf(
                CodeforcesTitle.MAIN,
                CodeforcesTitle.TOP,
                CodeforcesTitle.RECENT,
                CodeforcesTitle.LOST
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = remember {
            val defaultTab = runBlocking { settings.codeforcesDefaultTab() }
            tabs.value.indexOf(defaultTab)
        }
    )

    LaunchedEffect(key1 = pagerState.currentPage) {
        val selectedTab = tabs.value[pagerState.currentPage]
        navigator.setSubtitle("news", "codeforces", selectedTab.name)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabsHeader(
            tabs = tabs,
            pagerState = pagerState,
            modifier = Modifier.fillMaxWidth()
        )
        val currentTime by collectCurrentTimeEachMinute()
        CompositionLocalProvider(
            LocalCodeforcesAccountManager provides manager,
            LocalCurrentTime provides currentTime
        ) {
            HorizontalPager(
                count = tabs.value.size,
                state = pagerState,
                key = { tabs.value[it] },
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val modifier = Modifier.fillMaxSize()
                when (tabs.value[index]) {
                    CodeforcesTitle.MAIN -> CodeforcesNewsMainPage(
                        modifier = modifier,
                        viewModel = viewModel
                    )
                    CodeforcesTitle.TOP -> CodeforcesNewsTopPage(
                        modifier = modifier,
                        viewModel = viewModel
                    )
                    CodeforcesTitle.RECENT -> CodeforcesNewsRecentPage()
                    CodeforcesTitle.LOST -> CodeforcesNewsLostPage()
                }
            }
        }
    }

}


@Composable
private fun CodeforcesNewsMainPage(
    modifier: Modifier = Modifier,
    viewModel: CodeforcesNewsViewModel
) {
    val context = context
    val loadingStatus by viewModel.pageLoadingStatusState(CodeforcesTitle.MAIN)
    val blogEntriesState = rememberCollect { viewModel.flowOfMainBlogEntries(context) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { viewModel.reload(title = CodeforcesTitle.MAIN, context = context) },
        modifier = modifier
    ) {
        CodeforcesBlogEntries(
            blogEntriesState = blogEntriesState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CodeforcesNewsTopPage(
    modifier: Modifier = Modifier,
    viewModel: CodeforcesNewsViewModel
) {
    val context = context
    val loadingStatus by viewModel.pageLoadingStatusState(CodeforcesTitle.TOP)
    val blogEntriesState = rememberCollect { viewModel.flowOfTopBlogEntries(context) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { viewModel.reload(title = CodeforcesTitle.TOP, context = context) },
        modifier = modifier
    ) {
        CodeforcesBlogEntries(
            blogEntriesState = blogEntriesState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CodeforcesNewsRecentPage() {

}

@Composable
private fun CodeforcesNewsLostPage() {

}


@Composable
private fun TabsHeader(
    tabs: State<List<CodeforcesTitle>>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedTextColor: Color = cpsColors.content,
    unselectedTextColor: Color = cpsColors.contentAdditional,
) {
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = platformIconPainter(platform = Contest.Platform.codeforces),
            contentDescription = null,
            tint = cpsColors.content,
            modifier = Modifier.padding(start = 8.dp, end = 6.dp)
        )
        NewsTabRow(pagerState = pagerState) {
            tabs.value.forEachIndexed { index, title ->
                NewsTab(
                    index = index,
                    title = title.name,
                    pagerState = pagerState,
                    selectedTextColor = selectedTextColor,
                    unselectedTextColor = unselectedTextColor,
                    modifier = Modifier.clickableNoRipple {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    }
}
