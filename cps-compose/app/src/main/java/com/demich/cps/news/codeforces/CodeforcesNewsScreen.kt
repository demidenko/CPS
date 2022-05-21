@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.Contest
import com.demich.cps.news.NewsTab
import com.demich.cps.news.NewsTabRow
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerScope
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

val LocalCodeforcesAccountManager = compositionLocalOf<CodeforcesAccountManager> {
    throw IllegalAccessException()
}

@Composable
fun CodeforcesNewsScreen(
    navigator: CPSNavigator,
    controller: CodeforcesNewsController
) {

    LaunchedEffect(key1 = controller.currentTab) {
        navigator.setSubtitle("news", "codeforces", controller.currentTab.name)
    }

    Column {
        TabsHeader(
            controller = controller,
            modifier = Modifier.fillMaxWidth()
        )
        CodeforcesPager(
            controller = controller,
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                CodeforcesTitle.MAIN -> CodeforcesNewsMainPage(controller = controller)
                CodeforcesTitle.TOP -> CodeforcesNewsTopPage(controller = controller)
                CodeforcesTitle.RECENT -> CodeforcesNewsRecentPage(controller = controller)
                CodeforcesTitle.LOST -> CodeforcesNewsLostPage()
            }
        }
    }

}


@Composable
private fun CodeforcesNewsMainPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val loadingStatus by controller.pageLoadingStatusState(CodeforcesTitle.MAIN)
    val blogEntriesState = rememberCollect { controller.flowOfMainBlogEntries(context) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { controller.reload(title = CodeforcesTitle.MAIN, context = context) },
    ) {
        CodeforcesBlogEntries(
            blogEntriesState = blogEntriesState,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CodeforcesNewsTopPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val loadingStatus by controller.pageLoadingStatusState(CodeforcesTitle.TOP)
    val blogEntriesState = rememberCollect { controller.flowOfTopBlogEntries(context) }
    //val commentsState = rememberCollect { viewModel.flowOfTopComments(context) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { controller.reload(title = CodeforcesTitle.TOP, context = context) },
    ) {
        CodeforcesBlogEntries(
            blogEntriesState = blogEntriesState,
            modifier = Modifier.fillMaxSize()
        )
        /*CodeforcesComments(
            commentsState = commentsState,
            modifier = Modifier.fillMaxSize()
        )*/
    }
}

@Composable
private fun CodeforcesNewsRecentPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val loadingStatus by controller.pageLoadingStatusState(CodeforcesTitle.RECENT)
    val recentActionsState = rememberCollect { controller.flowOfRecentActions(context) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { controller.reload(title = CodeforcesTitle.RECENT, context = context) },
    ) {
        CodeforcesRecentBlogEntries(
            recentActionsState = recentActionsState,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CodeforcesNewsLostPage() {

}


@Composable
private fun CodeforcesPager(
    controller: CodeforcesNewsController,
    modifier: Modifier = Modifier,
    content: @Composable (PagerScope.(CodeforcesTitle) -> Unit)
) {
    val context = context
    val manager = remember { CodeforcesAccountManager(context) }

    val currentTime by collectCurrentTimeEachMinute()
    CompositionLocalProvider(
        LocalCodeforcesAccountManager provides manager,
        LocalCurrentTime provides currentTime
    ) {
        HorizontalPager(
            count = controller.tabs.size,
            state = controller.pagerState,
            key = { index -> controller.tabs[index] },
            modifier = modifier
        ) { index ->
            content(controller.tabs[index])
        }
    }

}

@Composable
private fun TabsHeader(
    controller: CodeforcesNewsController,
    modifier: Modifier = Modifier
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
        NewsTabRow(pagerState = controller.pagerState) {
            controller.tabs.forEachIndexed { index, title ->
                NewsTab(
                    index = index,
                    title = title.name,
                    pagerState = controller.pagerState,
                    selectedTextColor = cpsColors.content,
                    unselectedTextColor = cpsColors.contentAdditional,
                    modifier = Modifier.clickableNoRipple {
                        scope.launch {
                            controller.pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    }
}
