package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.utils.NewEntriesDataStoreItem
import com.demich.cps.utils.combineToCounters
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString


@Composable
fun rememberCodeforcesCommunityController(): CodeforcesCommunityController {
    val context = context
    val viewModel = codeforcesCommunityViewModel()

    val tabsState = rememberCollect {
        context.settingsCommunity.flowOfCodeforcesTabs()
    }

    val controller = rememberSaveable(
        saver = CodeforcesCommunityController.saver(viewModel, tabsState)
    ) {
        val settings = context.settingsCommunity
        val defaultTab = runBlocking { settings.codeforcesDefaultTab() }
        CodeforcesCommunityController(
            viewModel = viewModel,
            tabsState = tabsState,
            data = CodeforcesCommunityControllerData(
                selectedTab = defaultTab,
                topShowComments = false,
                recentShowComments = false,
                recentFilterByBlogEntry = null
            )
        )
    }

    DisposableEffect(controller) {
        with(controller) {
            flowOfMainBlogEntries(context)
            if (topShowComments) flowOfTopComments(context) else flowOfTopBlogEntries(context)
            flowOfRecent(context)
        }
        onDispose { }
    }

    return controller
}

@Serializable
internal data class CodeforcesCommunityControllerData(
    val selectedTab: CodeforcesTitle,
    val topShowComments: Boolean,
    val recentShowComments: Boolean,
    val recentFilterByBlogEntry: CodeforcesBlogEntry?
)

@OptIn(ExperimentalFoundationApi::class)
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

    fun isTabVisible(tab: CodeforcesTitle) = tab == currentTab && !pagerState.isScrollInProgress

    suspend fun scrollTo(tab: CodeforcesTitle) =
        pagerState.animateScrollToPage(page = tabs.indexOf(tab))


    var topShowComments by mutableStateOf(data.topShowComments)

    var recentShowComments by mutableStateOf(data.recentShowComments)
    var recentFilterByBlogEntry: CodeforcesBlogEntry? by mutableStateOf(data.recentFilterByBlogEntry)


    fun flowOfBadgeCount(tab: CodeforcesTitle, context: Context): Flow<Int> =
        when (tab) {
            CodeforcesTitle.MAIN -> flowOfBadgeCount(
                isTabVisibleFlow = snapshotFlow { tab == currentTab },
                blogEntriesFlow = flowOfMainBlogEntries(context),
                newEntriesItem = CodeforcesNewEntriesDataStore(context).mainNewEntries
            )
            CodeforcesTitle.LOST -> flowOfBadgeCount(
                isTabVisibleFlow = snapshotFlow { tab == currentTab },
                blogEntriesFlow = flowOfLostBlogEntries(context),
                newEntriesItem = CodeforcesNewEntriesDataStore(context).lostNewEntries
            )
            else -> flowOf(0)
        }

    @Composable
    fun rememberLoadingStatusState() = rememberCollect { flowOfLoadingStatus() }

    @Composable
    fun rememberLoadingStatusState(title: CodeforcesTitle) = rememberCollect { flowOfLoadingStatus(title) }

    fun flowOfLostBlogEntries(context: Context) =
        context.lostBlogEntriesDao.flowOfLost().map { blogEntries ->
            blogEntries.sortedByDescending { it.timeStamp }
                .map { it.blogEntry }
        }

    companion object {
        fun saver(
            viewModel: CodeforcesCommunityViewModel,
            tabsState: State<List<CodeforcesTitle>>
        ) = Saver<CodeforcesCommunityController, String>(
            save = {
                jsonCPS.encodeToString(CodeforcesCommunityControllerData(
                    selectedTab = it.currentTab,
                    topShowComments = it.topShowComments,
                    recentShowComments = it.recentShowComments,
                    recentFilterByBlogEntry = it.recentFilterByBlogEntry
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
    }
}

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