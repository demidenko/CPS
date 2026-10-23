package com.demich.cps.community.codeforces

import android.content.Context
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
import androidx.compose.runtime.collectAsState
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
import com.demich.cps.utils.LoadingStatus.PENDING
import com.demich.cps.utils.NewEntryTypeCounters
import com.demich.cps.utils.ProvideSystemTimeEachMinute
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.context
import com.demich.cps.utils.newEntriesCountersBy
import com.demich.cps.utils.stateOfNull
import com.demich.cps.workers.CodeforcesLostRecentWorker
import com.demich.cps.workers.isRunning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class CodeforcesTab {
    MAIN, TOP, RECENT, LOST
}

@Composable
fun CodeforcesCommunityScreen(
    controller: CodeforcesCommunityController,
    renderAllPages: Boolean
) {
    val newEntriesState = rememberNewEntriesState()
    Column {
        CodeforcesPagerHeader(
            backgroundLoader = controller,
            pagerState = controller.pagerState,
            newEntriesState = newEntriesState,
            modifier = Modifier.fillMaxWidth()
        )
        CodeforcesPager(
            controller = controller,
            renderAllPages = renderAllPages,
            newEntriesState = newEntriesState,
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
fun CodeforcesReloadablePage(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    tab: CodeforcesTab,
    content: @Composable () -> Unit
) {
    val context = context
    val loadingStatus by backgroundLoader.loadingStatusState(tab = tab)
    CPSSwipeRefreshBox(
        isRefreshing = { loadingStatus == LOADING },
        onRefresh = { backgroundLoader.reload(tab = tab, context = context) },
        content = content
    )
}

@Composable
private fun CodeforcesCommunityBackgroundLoader.loadingStatusState(tab: CodeforcesTab): State<LoadingStatus> {
    if (tab == LOST) {
        val context = context
        return remember {
            CodeforcesLostRecentWorker.getWork(context)
                .flowOfWorkInfo()
                .map { if (it.isRunning) LoadingStatus.LOADING else PENDING }
        }.collectAsStateWithLifecycle(initialValue = PENDING)
    }

    return remember(tab) { flowOfLoadingStatus(tab) }
        .collectAsState(initial = PENDING) //TODO: be sure this fake is ok
}

@Composable
private fun CodeforcesPager(
    controller: CodeforcesCommunityController,
    renderAllPages: Boolean,
    newEntriesState: NewEntriesState,
    modifier: Modifier = Modifier
) {
    val pagerState = controller.pagerState
    ProvideSystemTimeEachMinute {
        HorizontalPager(
            beyondViewportPageCount = if (renderAllPages) pagerState.tabs.size - 1 else 0,
            state = pagerState,
            key = { index -> pagerState.tabs[index] },
            modifier = modifier
        ) { index ->
            when (pagerState.tabs[index]) {
                MAIN -> CodeforcesCommunityMainPage(controller, pagerState, newEntriesState)
                TOP -> CodeforcesCommunityTopPage(controller, pagerState, newEntriesState)
                RECENT -> CodeforcesCommunityRecentPage(controller, pagerState)
                LOST -> CodeforcesCommunityLostPage(controller, pagerState, newEntriesState)
            }
        }
    }

}

@Composable
private fun CodeforcesPagerHeader(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    pagerState: CodeforcesCommunityPagerState,
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
            pagerState = pagerState
        ) {
            pagerState.tabs.forEach { title ->
                CodeforcesCommunityTab(
                    tab = title,
                    backgroundLoader = backgroundLoader,
                    pagerState = pagerState,
                    newEntriesState = newEntriesState,
                    modifier = Modifier.clickableNoRipple {
                        animationScope.launch { pagerState.scrollTo(title) }
                    }
                )
            }
        }
    }
}

@Composable
private fun CodeforcesCommunityTab(
    tab: CodeforcesTab,
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    pagerState: CodeforcesCommunityPagerState,
    newEntriesState: NewEntriesState,
    modifier: Modifier = Modifier
) {
    val loadingStatus by backgroundLoader.loadingStatusState(tab)
    val counters by backgroundLoader.newEntryCountersState(tab, newEntriesState)

    CodeforcesCommunityTab(
        tab = tab,
        index = pagerState.tabs.indexOf(tab),
        loadingStatus = loadingStatus,
        badgeCount = counters?.toBadgeCounter(pagerState, tab),
        pagerState = pagerState,
        modifier = modifier
    )
}

@Composable
private fun CodeforcesCommunityBackgroundLoader.newEntryCountersState(
    tab: CodeforcesTab,
    newEntriesState: NewEntriesState
): State<NewEntryTypeCounters?> {
    val context = context
    val flowIds = remember(key1 = tab, key2 = this) {
        flowOfEntryIds(tab = tab, context = context)
    }

    if (flowIds == null) return stateOfNull()

    val entriesIdsState = flowIds.collectAsStateWithLifecycle(initialValue = null)
    return remember(newEntriesState) {
        derivedStateOf {
            when (val ids = entriesIdsState.value) {
                null -> null
                else -> ids.newEntriesCountersBy(newEntriesState.types)
            }
        }
    }
}

private fun CodeforcesCommunityBackgroundLoader.flowOfEntryIds(tab: CodeforcesTab, context: Context): Flow<List<Int>>? {
    val flow = when (tab) {
        MAIN -> flowOfMainBlogEntries(context)
        LOST -> flowOfLostBlogEntries(context)
        else -> return null
    }
    return flow.map { it?.map { it.id } ?: [] }
}

private fun NewEntryTypeCounters.toBadgeCounter(
    pagerState: CodeforcesCommunityPagerState,
    tab: CodeforcesTab
): Int =
    if (pagerState.currentTab == tab) seenCount + unseenCount
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

