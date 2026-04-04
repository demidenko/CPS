package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.NewEntryTypeCounters
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.combineToCounters
import com.demich.cps.utils.context
import com.demich.cps.utils.getValueBlocking
import com.demich.cps.utils.jsonCPS
import com.demich.cps.workers.CodeforcesCommunityLostRecentWorker
import com.demich.cps.workers.isRunning
import com.demich.kotlin_stdlib_boost.swap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable


@Composable
fun rememberCodeforcesCommunityController(): CodeforcesCommunityController {
    val context = context
    val viewModel = codeforcesCommunityViewModel()

    val tabsState = collectItemAsState { context.settingsCommunity.codeforcesTabs }

    val controller = rememberSaveable(saver = controllerSaver(viewModel, tabsState)) {
        val settings = context.settingsCommunity
        val defaultTab = settings.codeforcesDefaultTab.getValueBlocking()
        CodeforcesCommunityController(
            dataManager = viewModel,
            tabsState = tabsState,
            pagerData = CodeforcesCommunityPagerData(
                selectedTab = defaultTab,
                topPageType = BlogEntries,
                recentPageType = CodeforcesCommunityController.RecentPageType.RecentFeed
            )
        )
    }

    DisposableEffect(controller) {
        controller.touchFlows(context)
        onDispose { }
    }

    return controller
}

@Serializable
data class CodeforcesCommunityPagerData(
    val selectedTab: CodeforcesTitle,
    val topPageType: CodeforcesCommunityController.TopPageType,
    val recentPageType: CodeforcesCommunityController.RecentPageType
)

@Stable
class CodeforcesCommunityController(
    dataManager: CodeforcesCommunityDataManger,
    tabsState: State<List<CodeforcesTitle>>,
    pagerData: CodeforcesCommunityPagerData
): CodeforcesCommunityDataManger by dataManager {
    val tabs by tabsState

    //TODO: future support for dynamic tabs (selectedIndex can be out of bounds)
    val pagerState = object : PagerState(
        currentPage = tabs.indexOf(pagerData.selectedTab).let { if (it != -1) it else 0 }
    ) {
        override val pageCount: Int
            get() = tabs.size
    }

    val currentTab: CodeforcesTitle
        get() = tabs[pagerState.currentPage]

    // relies that tabs are always fixed!
    // not saved/restored!
    //TODO: incorrect!!!!
    val visitedTabs by tabs.toMutableList().let { list ->
        derivedStateOf {
            val pos = list.indexOf(currentTab)
            for (i in pos downTo 1) list.swap(i, i-1)
            list
        }
    }

    fun isTabVisible(tab: CodeforcesTitle) = tab == currentTab && !pagerState.isScrollInProgress

    suspend fun scrollTo(tab: CodeforcesTitle) =
        pagerState.animateScrollToPage(page = tabs.indexOf(tab))


    var topPageType by mutableStateOf(pagerData.topPageType)
    var recentPageType by mutableStateOf(pagerData.recentPageType)

    enum class TopPageType {
        BlogEntries, Comments
    }

    @Serializable
    sealed interface RecentPageType {
        @Serializable
        data object RecentFeed : RecentPageType
        @Serializable
        data object RecentComments : RecentPageType
        @Serializable
        data class BlogEntryRecentComments(val blogEntry: CodeforcesRecentFeedBlogEntry) : RecentPageType
    }
}

@Composable
fun CodeforcesCommunityDataManger.loadingStatusState(): State<LoadingStatus> =
    collectAsState { flowOfLoadingStatus() }

@Composable
fun CodeforcesCommunityDataManger.loadingStatusState(title: CodeforcesTitle): State<LoadingStatus> {
    if (title == LOST) {
        val context = context
        return remember {
            CodeforcesCommunityLostRecentWorker.getWork(context)
                .flowOfWorkInfo()
                .map { if (it.isRunning) LoadingStatus.LOADING else PENDING }
        }.collectAsStateWithLifecycle(initialValue = PENDING)
    }

    return remember(title) { flowOfLoadingStatus(title) }
        .collectAsState(initial = PENDING) //TODO: be sure this fake is ok
}


private fun controllerSaver(
    dataManager: CodeforcesCommunityDataManger,
    tabsState: State<List<CodeforcesTitle>>
) = Saver<CodeforcesCommunityController, String>(
    save = {
        jsonCPS.encodeToString(CodeforcesCommunityPagerData(
            selectedTab = it.currentTab,
            topPageType = it.topPageType,
            recentPageType = it.recentPageType
        ))
    },
    restore = {
        CodeforcesCommunityController(
            dataManager = dataManager,
            tabsState = tabsState,
            pagerData = jsonCPS.decodeFromString(it)
        )
    }
)

fun CodeforcesCommunityDataManger.flowOfNewEntryCounters(tab: CodeforcesTitle, context: Context): Flow<NewEntryTypeCounters>? {
    val flow = when (tab) {
        MAIN -> flowOfMainBlogEntries(context)
        LOST -> flowOfLostBlogEntries(context)
        else -> return null
    }
    return combineToCounters(
        flowOfIds = flow.map { it.map { it.id } },
        flowOfTypes = CodeforcesNewEntriesDataStore(context).commonNewEntries.asFlow()
    )
}

private fun CodeforcesCommunityController.touchFlows(context: Context) {
    visitedTabs.forEach { tab ->
        when (tab) {
            MAIN -> flowOfMainBlogEntries(context)
            TOP -> {
                when (topPageType) {
                    BlogEntries -> flowOfTopBlogEntries(context)
                    Comments -> flowOfTopComments(context)
                }
            }
            RECENT -> flowOfRecent(context)
            else -> { }
        }
    }
}