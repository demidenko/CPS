package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.community.CommunityTab
import com.demich.cps.community.CommunityTabRow
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSDefaults
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
            beyondViewportPageCount = controller.tabs.size - 1,
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
        Box(
            modifier = Modifier
                .height(CPSDefaults.tabsRowHeight)
                .padding(vertical = 10.dp)
                .padding(start = 6.dp, end = 10.dp)
        ) {
            Icon(
                painter = platformIconPainter(platform = Contest.Platform.codeforces),
                contentDescription = null,
                tint = cpsColors.content,
                modifier = Modifier
                    .align(Alignment.Center)
                    .aspectRatio(1f)
                    .fillMaxSize()
            )
        }

        CommunityTabRow(
            modifier = Modifier.fillMaxWidth(),
            pagerState = controller.pagerState
        ) {
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
private fun CodeforcesCommunityController.badgeCountState(title: CodeforcesTitle): State<Int> {
    val context = context
    val flow = remember(title, this) { flowOfBadgeCount(tab = title, context = context) }
    return if (flow == null) {
        remember { mutableIntStateOf(0) }
    } else {
        collectAsState { flow }
    }
}

@Composable
private fun CodeforcesCommunityTab(
    title: CodeforcesTitle,
    controller: CodeforcesCommunityController,
    modifier: Modifier = Modifier
) {
    val loadingStatus by controller.loadingStatusState(title)
    val badgeCount by controller.badgeCountState(title)
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

