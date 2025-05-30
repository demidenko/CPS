package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.NewEntriesDataStoreItem
import com.demich.cps.utils.combineToCounters
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.collectAsState
import com.demich.kotlin_stdlib_boost.swap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


@Composable
fun rememberCodeforcesCommunityController(): CodeforcesCommunityController {
    val context = context
    val viewModel = codeforcesCommunityViewModel()

    val tabsState = collectAsState {
        context.settingsCommunity.flowOfCodeforcesTabs()
    }

    val controller = rememberSaveable(saver = controllerSaver(viewModel, tabsState)) {
        val settings = context.settingsCommunity
        val defaultTab = runBlocking { settings.codeforcesDefaultTab() }
        CodeforcesCommunityController(
            viewModel = viewModel,
            tabsState = tabsState,
            data = CodeforcesCommunityControllerData(
                selectedTab = defaultTab,
                topPageType = CodeforcesCommunityController.TopPageType.BlogEntries,
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
internal data class CodeforcesCommunityControllerData(
    val selectedTab: CodeforcesTitle,
    val topPageType: CodeforcesCommunityController.TopPageType,
    val recentPageType: CodeforcesCommunityController.RecentPageType
)

@Stable
class CodeforcesCommunityController internal constructor(
    viewModel: CodeforcesCommunityViewModel,
    tabsState: State<List<CodeforcesTitle>>,
    data: CodeforcesCommunityControllerData
): CodeforcesCommunityDataManger by viewModel {
    val tabs by tabsState

    //TODO: future support for dynamic tabs (selectedIndex can be out of bounds)
    val pagerState = object : PagerState(
        currentPage = tabs.indexOf(data.selectedTab)
            .takeIf { it != -1 } ?: 0
    ) {
        override val pageCount: Int
            get() = tabs.size
    }

    val currentTab: CodeforcesTitle
        get() = tabs[pagerState.currentPage]

    // relies that tabs are always fixed!
    // not saved/restored!
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


    var topPageType by mutableStateOf(data.topPageType)
    var recentPageType by mutableStateOf(data.recentPageType)

    fun flowOfBadgeCount(tab: CodeforcesTitle, context: Context): Flow<Int> =
        when (tab) {
            CodeforcesTitle.MAIN -> flowOfBadgeCount(
                isTabVisibleFlow = snapshotFlow { currentTab == CodeforcesTitle.MAIN },
                blogEntriesFlow = flowOfMainBlogEntries(context),
                newEntriesItem = CodeforcesNewEntriesDataStore(context).commonNewEntries
            )
            CodeforcesTitle.LOST -> flowOfBadgeCount(
                isTabVisibleFlow = snapshotFlow { currentTab == CodeforcesTitle.LOST },
                blogEntriesFlow = flowOfLostBlogEntries(context),
                newEntriesItem = CodeforcesNewEntriesDataStore(context).commonNewEntries
            )
            else -> flowOf(0)
        }

    fun flowOfLostBlogEntries(context: Context) =
        context.lostBlogEntriesDao.flowOfLost().map { blogEntries ->
            blogEntries.sortedByDescending { it.timeStamp }
                .map { it.blogEntry }
        }

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
        data class BlogEntryRecentComments(val blogEntry: CodeforcesBlogEntry) : RecentPageType
    }
}

@Composable
fun CodeforcesCommunityController.loadingStatusState(): State<LoadingStatus> =
    collectAsState { flowOfLoadingStatus() }

//TODO: remember(key = title)???
@Composable
fun CodeforcesCommunityController.loadingStatusState(title: CodeforcesTitle): State<LoadingStatus> =
    collectAsState { flowOfLoadingStatus(title) }

private fun controllerSaver(
    viewModel: CodeforcesCommunityViewModel,
    tabsState: State<List<CodeforcesTitle>>
) = Saver<CodeforcesCommunityController, String>(
    save = {
        jsonCPS.encodeToString(CodeforcesCommunityControllerData(
            selectedTab = it.currentTab,
            topPageType = it.topPageType,
            recentPageType = it.recentPageType
        ))
    },
    restore = {
        CodeforcesCommunityController(
            viewModel = viewModel,
            tabsState = tabsState,
            data = jsonCPS.decodeFromString(it)
        )
    }
)

private fun flowOfBadgeCount(
    isTabVisibleFlow: Flow<Boolean>,
    blogEntriesFlow: Flow<List<CodeforcesBlogEntry>>,
    newEntriesItem: NewEntriesDataStoreItem
): Flow<Int> =
    combineToCounters(
        flowOfIds = blogEntriesFlow.map { it.map { it.id } },
        flowOfTypes = newEntriesItem.flow
    ).combine(isTabVisibleFlow) { counters, isTabVisible ->
        if (isTabVisible) counters.seenCount + counters.unseenCount
        else counters.unseenCount
    }

private fun CodeforcesCommunityController.touchFlows(context: Context) {
    visitedTabs.forEach { tab ->
        when (tab) {
            CodeforcesTitle.MAIN -> flowOfMainBlogEntries(context)
            CodeforcesTitle.TOP -> {
                when (topPageType) {
                    CodeforcesCommunityController.TopPageType.BlogEntries -> flowOfTopBlogEntries(context)
                    CodeforcesCommunityController.TopPageType.Comments -> flowOfTopComments(context)
                }
            }
            CodeforcesTitle.RECENT -> flowOfRecent(context)
            else -> { }
        }
    }
}