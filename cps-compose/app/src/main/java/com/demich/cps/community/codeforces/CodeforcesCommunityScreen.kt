package com.demich.cps.community.codeforces

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.community.CommunityTab
import com.demich.cps.community.CommunityTabRow
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSSwipeRefreshBox
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.NewEntryTypeCounters
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.context
import com.demich.cps.utils.firstBlocking
import com.demich.cps.utils.rememberFirstValue
import kotlinx.coroutines.launch

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

@Composable
fun CodeforcesCommunityScreen(
    controller: CodeforcesCommunityController
) {
    Column {
        CodeforcesPagerHeader(
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
    val context = context
    val renderAllTabs = rememberFirstValue { context.settingsCommunity.renderAllTabs }

    val newEntriesState = rememberNewEntriesState()
    ProvideTimeEachMinute {
        HorizontalPager(
            beyondViewportPageCount = if (renderAllTabs) controller.tabs.size - 1 else 0,
            state = controller.pagerState,
            key = { index -> controller.tabs[index] },
            modifier = modifier
        ) { index ->
            when (controller.tabs[index]) {
                MAIN -> CodeforcesCommunityMainPage(controller, newEntriesState)
                TOP -> CodeforcesCommunityTopPage(controller, newEntriesState)
                RECENT -> CodeforcesCommunityRecentPage(controller = controller)
                LOST -> CodeforcesCommunityLostPage(controller, newEntriesState)
            }
        }
    }

}

@Composable
private fun CodeforcesPagerHeader(
    controller: CodeforcesCommunityController,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(CPSDefaults.tabsRowHeight)
    ) {
        Box(
            modifier = Modifier
                .background(cpsColors.background)
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
            modifier = Modifier.fillMaxSize(),
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
private fun CodeforcesCommunityController.newEntryCountersState(title: CodeforcesTitle): State<NewEntryTypeCounters?> {
    val context = context
    val flow = remember(key1 = title, key2 = this) {
        flowOfNewEntryCounters(tab = title, context = context)
    }

    return if (flow == null) {
        remember { mutableStateOf(null) }
    } else {
        flow.collectAsStateWithLifecycle(initialValue = remember { flow.firstBlocking() })
    }
}

@Composable
private fun CodeforcesCommunityTab(
    title: CodeforcesTitle,
    controller: CodeforcesCommunityController,
    modifier: Modifier = Modifier
) {
    val loadingStatus by controller.loadingStatusState(title)
    val counters by controller.newEntryCountersState(title)
    CodeforcesCommunityTab(
        title = title,
        index = controller.tabs.indexOf(title),
        loadingStatus = loadingStatus,
        badgeCount = { controller.badgeCounter(title, counters) },
        pagerState = controller.pagerState,
        modifier = modifier
    )
}

private fun CodeforcesCommunityController.badgeCounter(
    tab: CodeforcesTitle,
    counters: NewEntryTypeCounters?
): Int? =
    counters?.let {
        if (currentTab == tab) it.seenCount + it.unseenCount
        else it.unseenCount
    }?.takeIf { it != 0 }

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

