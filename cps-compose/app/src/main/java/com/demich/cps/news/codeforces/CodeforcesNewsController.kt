package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString


@Composable
fun rememberCodeforcesNewsController(): CodeforcesNewsController {
    val context = context
    val viewModel = codeforcesNewsViewModel()

    val tabsState = rememberCollect {
        context.settingsNews.flowOfCodeforcesTabs()
    }

    val controller = rememberSaveable(
        viewModel,
        saver = remember(viewModel, tabsState) {
            CodeforcesNewsController.saver(viewModel, tabsState)
        }
    ) {
        val settings = context.settingsNews
        val defaultTab = runBlocking { settings.codeforcesDefaultTab() }
        CodeforcesNewsController(
            viewModel = viewModel,
            tabsState = tabsState,
            data = CodeforcesNewsControllerData(
                selectedTab = defaultTab,
                topShowComments = false,
                recentShowComments = false,
                recentFilterByBlogEntry = null
            )
        )
    }

    LaunchedEffect(controller) {
        with(controller) {
            flowOfMainBlogEntries(context)
            if (topShowComments) flowOfTopComments(context) else flowOfTopBlogEntries(context)
            flowOfRecentActions(context)
        }
    }

    return controller
}

@Serializable
internal data class CodeforcesNewsControllerData(
    val selectedTab: CodeforcesTitle,
    val topShowComments: Boolean,
    val recentShowComments: Boolean,
    val recentFilterByBlogEntry: CodeforcesBlogEntry?
)

@OptIn(ExperimentalFoundationApi::class)
@Stable
class CodeforcesNewsController internal constructor(
    viewModel: CodeforcesNewsViewModel,
    tabsState: State<List<CodeforcesTitle>>,
    data: CodeforcesNewsControllerData
): CodeforcesNewsDataManger by viewModel {
    val tabs by tabsState

    //TODO: future support for dynamic tabs (selectedIndex can be out of bounds)
    val pagerState = object : PagerState(
        initialPage = tabs.indexOf(data.selectedTab)
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


    private val badges = mutableMapOf<CodeforcesTitle, MutableIntState>()
    private fun badgeState(tab: CodeforcesTitle) = badges.getOrPut(tab) { mutableIntStateOf(0) }
    fun getBadgeCountState(tab: CodeforcesTitle): IntState = badgeState(tab)
    fun setBadgeCount(tab: CodeforcesTitle, count: Int) { badgeState(tab).intValue = count }

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
            viewModel: CodeforcesNewsViewModel,
            tabsState: State<List<CodeforcesTitle>>
        ) = Saver<CodeforcesNewsController, String>(
            save = {
                jsonCPS.encodeToString(CodeforcesNewsControllerData(
                    selectedTab = it.currentTab,
                    topShowComments = it.topShowComments,
                    recentShowComments = it.recentShowComments,
                    recentFilterByBlogEntry = it.recentFilterByBlogEntry
                ))
            },
            restore = {
                CodeforcesNewsController(
                    viewModel = viewModel,
                    tabsState = tabsState,
                    data = jsonCPS.decodeFromString(it)
                )
            }
        )
    }
}