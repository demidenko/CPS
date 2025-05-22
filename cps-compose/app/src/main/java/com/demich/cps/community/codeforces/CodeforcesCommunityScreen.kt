@file:OptIn(ExperimentalFoundationApi::class)

package com.demich.cps.community.codeforces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.community.CommunityTab
import com.demich.cps.community.CommunityTabRow
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSSwipeRefreshBox
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import kotlinx.coroutines.launch

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

@Composable
fun CodeforcesCommunityScreen(
    controller: CodeforcesCommunityController
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
    controller: CodeforcesCommunityController,
    title: CodeforcesTitle,
    content: @Composable () -> Unit
) {
    val context = context
    val loadingStatus by controller.loadingStatusState(title = title)
    CPSSwipeRefreshBox(
        isRefreshing = { loadingStatus == LoadingStatus.LOADING },
        onRefresh = { controller.reload(title = title, context = context) },
        content = content
    )
}

@Composable
private fun CodeforcesPager(
    controller: CodeforcesCommunityController,
    modifier: Modifier = Modifier
) {
    val newEntriesState = rememberNewEntriesState()
    ProvideTimeEachMinute {
        HorizontalPager(
            beyondBoundsPageCount = controller.tabs.size - 1,
            state = controller.pagerState,
            key = { index -> controller.tabs[index] },
            modifier = modifier
        ) { index ->
            when (controller.tabs[index]) {
                CodeforcesTitle.MAIN -> CodeforcesCommunityMainPage(controller, newEntriesState)
                CodeforcesTitle.TOP -> CodeforcesCommunityTopPage(controller, newEntriesState)
                CodeforcesTitle.RECENT -> CodeforcesCommunityRecentPage(controller = controller)
                CodeforcesTitle.LOST -> CodeforcesCommunityLostPage(controller, newEntriesState)
            }
        }
    }

}

@Composable
private fun TabsHeader(
    controller: CodeforcesCommunityController,
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
        CommunityTabRow(pagerState = controller.pagerState) {
            controller.tabs.forEach { title ->
                CodeforcesCommunityTab(
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
private fun CodeforcesCommunityTab(
    title: CodeforcesTitle,
    controller: CodeforcesCommunityController,
    modifier: Modifier = Modifier
) {
    val context = context
    val loadingStatus by controller.loadingStatusState(title)
    val badgeCount by collectAsState { controller.flowOfBadgeCount(tab = title, context) }
    CodeforcesCommunityTab(
        title = title,
        index = controller.tabs.indexOf(title),
        loadingStatus = loadingStatus,
        badgeCount = { badgeCount.takeIf { it != 0 } },
        pagerState = controller.pagerState,
        modifier = modifier
    )
}

@Composable
private fun CodeforcesCommunityTab(
    title: CodeforcesTitle,
    index: Int,
    loadingStatus: LoadingStatus,
    badgeCount: () -> Int?,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    CommunityTab(
        title = if (loadingStatus != LoadingStatus.LOADING) title.name else "...",
        index = index,
        badgeCount = badgeCount,
        pagerState = pagerState,
        selectedTextColor = if (loadingStatus != LoadingStatus.FAILED) cpsColors.content else cpsColors.error,
        unselectedTextColor = if (loadingStatus != LoadingStatus.FAILED) cpsColors.contentAdditional else cpsColors.error,
        modifier = modifier
    )
}

