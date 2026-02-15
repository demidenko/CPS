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
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.NewEntriesDataStoreItem
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

    fun flowOfNewEntryCounters(tab: CodeforcesTitle, context: Context): Flow<NewEntryTypeCounters>? =
        when (tab) {
            MAIN -> flowOfNewEntryCounters(
                blogEntriesFlow = flowOfMainBlogEntries(context),
                newEntriesItem = CodeforcesNewEntriesDataStore(context).commonNewEntries
            )
            LOST -> flowOfNewEntryCounters(
                blogEntriesFlow = flowOfLostBlogEntries(context),
                newEntriesItem = CodeforcesNewEntriesDataStore(context).commonNewEntries
            )
            else -> null
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
        data class BlogEntryRecentComments(val blogEntry: CodeforcesRecentFeedBlogEntry) : RecentPageType
    }
}

@Composable
fun CodeforcesCommunityController.loadingStatusState(): State<LoadingStatus> =
    collectAsState { flowOfLoadingStatus() }

@Composable
fun CodeforcesCommunityController.loadingStatusState(title: CodeforcesTitle): State<LoadingStatus> {
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

private fun flowOfNewEntryCounters(
    blogEntriesFlow: Flow<List<CodeforcesBlogEntry>>,
    newEntriesItem: NewEntriesDataStoreItem
): Flow<NewEntryTypeCounters> =
    combineToCounters(
        flowOfIds = blogEntriesFlow.map { it.map { it.id } },
        flowOfTypes = newEntriesItem.asFlow()
    )

private fun CodeforcesCommunityController.touchFlows(context: Context) {
    visitedTabs.forEach { tab ->
        when (tab) {
            MAIN -> flowOfMainBlogEntries(context)
            TOP -> {
                when (topPageType) {
                    CodeforcesCommunityController.TopPageType.BlogEntries -> flowOfTopBlogEntries(context)
                    CodeforcesCommunityController.TopPageType.Comments -> flowOfTopComments(context)
                }
            }
            RECENT -> flowOfRecent(context)
            else -> { }
        }
    }
}