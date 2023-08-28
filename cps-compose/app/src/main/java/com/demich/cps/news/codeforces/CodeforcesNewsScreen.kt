@file:OptIn(ExperimentalFoundationApi::class)

package com.demich.cps.news.codeforces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.contests.database.Contest
import com.demich.cps.news.NewsTab
import com.demich.cps.news.NewsTabRow
import com.demich.cps.ui.CPSSwipeRefreshBox
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

@Composable
fun CodeforcesNewsScreen(
    controller: CodeforcesNewsController
) {
    Column {
        TabsHeader(
            controller = controller,
            modifier = Modifier.fillMaxWidth()
        )
        CodeforcesPager(
            controller = controller,
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
fun CodeforcesReloadablePage(
    controller: CodeforcesNewsController,
    title: CodeforcesTitle,
    content: @Composable () -> Unit
) {
    val context = context
    val loadingStatus by controller.rememberLoadingStatusState(title = title)
    CPSSwipeRefreshBox(
        isRefreshing = { loadingStatus == LoadingStatus.LOADING },
        onRefresh = { controller.reload(title = title, context = context) },
        content = content
    )
}

@Composable
private fun CodeforcesPager(
    controller: CodeforcesNewsController,
    modifier: Modifier = Modifier
) {
    ProvideTimeEachMinute {
        HorizontalPager(
            beyondBoundsPageCount = controller.tabs.size - 1,
            state = controller.pagerState,
            key = { index -> controller.tabs[index] },
            modifier = modifier
        ) { index ->
            when (controller.tabs[index]) {
                CodeforcesTitle.MAIN -> CodeforcesNewsMainPage(controller = controller)
                CodeforcesTitle.TOP -> CodeforcesNewsTopPage(controller = controller)
                CodeforcesTitle.RECENT -> CodeforcesNewsRecentPage(controller = controller)
                CodeforcesTitle.LOST -> CodeforcesNewsLostPage(controller = controller)
            }
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
            modifier = Modifier
                .padding(start = 8.dp, end = 6.dp)
                .size(24.dp)
        )
        NewsTabRow(pagerState = controller.pagerState) {
            controller.tabs.forEach { title ->
                CodeforcesNewsTab(
                    title = title,
                    controller = controller,
                    modifier = Modifier.clickableNoRipple {
                        scope.launch { controller.scrollTo(title) }
                    }
                )
            }
        }
    }
}

@Composable
private fun CodeforcesNewsTab(
    title: CodeforcesTitle,
    controller: CodeforcesNewsController,
    modifier: Modifier = Modifier
) {
    val context = context
    val loadingStatus by controller.rememberLoadingStatusState(title)
    val badgeCount by rememberCollect { controller.flowOfBadgeCount(tab = title, context) }
    CodeforcesNewsTab(
        title = title,
        index = controller.tabs.indexOf(title),
        loadingStatus = loadingStatus,
        badgeCount = badgeCount,
        pagerState = controller.pagerState,
        modifier = modifier
    )
}

@Composable
private fun CodeforcesNewsTab(
    title: CodeforcesTitle,
    index: Int,
    loadingStatus: LoadingStatus,
    badgeCount: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    NewsTab(
        title = if (loadingStatus != LoadingStatus.LOADING) title.name else "...",
        index = index,
        badgeCount = badgeCount,
        pagerState = pagerState,
        selectedTextColor = if (loadingStatus != LoadingStatus.FAILED) cpsColors.content else cpsColors.error,
        unselectedTextColor = if (loadingStatus != LoadingStatus.FAILED) cpsColors.contentAdditional else cpsColors.error,
        modifier = modifier
    )
}

