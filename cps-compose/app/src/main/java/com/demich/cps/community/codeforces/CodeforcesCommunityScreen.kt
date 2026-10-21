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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.community.CommunityTab
import com.demich.cps.community.CommunityTabRow
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSSwipeRefreshBox
import com.demich.cps.ui.platformLogoPainter
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.NewEntryTypeCounters
import com.demich.cps.utils.ProvideSystemTimeEachMinute
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.combineToCounters
import com.demich.cps.utils.context
import com.demich.cps.utils.stateOfNull
import kotlinx.coroutines.launch

enum class CodeforcesTab {
    MAIN, TOP, RECENT, LOST
}

@Composable
fun CodeforcesCommunityScreen(
    controller: CodeforcesCommunityController,
    renderAllTabs: Boolean
) {
    val newEntriesState = rememberNewEntriesState()
    Column {
        CodeforcesPagerHeader(
            controller = controller,
            newEntriesState = newEntriesState,
            modifier = Modifier.fillMaxWidth()
        )
        CodeforcesPager(
            controller = controller,
            renderAllTabs = renderAllTabs,
            newEntriesState = newEntriesState,
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
fun CodeforcesReloadablePage(
    controller: CodeforcesCommunityController,
    tab: CodeforcesTab,
    content: @Composable () -> Unit
) {
    val context = context
    val loadingStatus by controller.loadingStatusState(tab = tab)
    CPSSwipeRefreshBox(
        isRefreshing = { loadingStatus == LOADING },
        onRefresh = { controller.reload(tab = tab, context = context) },
        content = content
    )
}

@Composable
private fun CodeforcesPager(
    controller: CodeforcesCommunityController,
    renderAllTabs: Boolean,
    newEntriesState: NewEntriesState,
    modifier: Modifier = Modifier
) {
    ProvideSystemTimeEachMinute {
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
    newEntriesState: NewEntriesState,
    modifier: Modifier = Modifier
) {
    val animationScope = rememberCoroutineScope()
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
                painter = platformLogoPainter(platform = codeforces),
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
                    tab = title,
                    controller = controller,
                    newEntriesState = newEntriesState,
                    modifier = Modifier.clickableNoRipple {
                        animationScope.launch { controller.scrollTo(title) }
                    }
                )
            }
        }
    }
}

@Composable
private fun CodeforcesCommunityTab(
    tab: CodeforcesTab,
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState,
    modifier: Modifier = Modifier
) {
    val loadingStatus by controller.loadingStatusState(tab)
    val badgeCount by controller.tabBadgeCountState(tab, newEntriesState)

    CodeforcesCommunityTab(
        tab = tab,
        index = controller.tabs.indexOf(tab),
        loadingStatus = loadingStatus,
        badgeCount = badgeCount,
        pagerState = controller.pagerState,
        modifier = modifier
    )
}

@Composable
private fun CodeforcesCommunityController.tabBadgeCountState(
    tab: CodeforcesTab,
    newEntriesState: NewEntriesState
): State<Int?> {
    val context = context
    val flowIds = remember(key1 = tab, key2 = this) {
        flowOfEntryIds(tab = tab, context = context)
    }

    if (flowIds == null) return stateOfNull()

    val entriesIdsState = flowIds.collectAsStateWithLifecycle(initialValue = null)
    return remember(this, tab, newEntriesState) {
        derivedStateOf {
            when (val ids = entriesIdsState.value) {
                null -> null
                else -> combineToCounters(ids, newEntriesState.types).toBadgeCounter(this, tab)
            }
        }
    }
}

private fun NewEntryTypeCounters.toBadgeCounter(
    controller: CodeforcesCommunityController,
    tab: CodeforcesTab
): Int =
    if (controller.currentTab == tab) seenCount + unseenCount
    else unseenCount

@Composable
private fun CodeforcesCommunityTab(
    tab: CodeforcesTab,
    index: Int,
    loadingStatus: LoadingStatus,
    badgeCount: Int?,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    CommunityTab(
        title = if (loadingStatus != LOADING) tab.name else "...",
        index = index,
        badgeCount = badgeCount,
        pagerState = pagerState,
        selectedTextColor = if (loadingStatus != FAILED) cpsColors.content else cpsColors.error,
        unselectedTextColor = if (loadingStatus != FAILED) cpsColors.contentAdditional else cpsColors.error,
        modifier = modifier
    )
}

