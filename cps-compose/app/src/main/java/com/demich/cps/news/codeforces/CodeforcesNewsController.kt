@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.lostBlogEntriesDao
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


@Composable
fun rememberCodeforcesNewsController(
    viewModel: CodeforcesNewsViewModel
): CodeforcesNewsController {
    val context = context

    val controller = rememberSaveable(
        viewModel,
        saver = remember(viewModel) { CodeforcesNewsController.saver(viewModel) }
    ) {
        val settings = context.settingsNews
        val initTabs = runBlocking { settings.flowOfCodeforcesTabs().first() }
        val defaultTab = runBlocking { settings.codeforcesDefaultTab() }
        CodeforcesNewsController(
            viewModel = viewModel,
            data = CodeforcesNewsControllerData(
                selectedIndex = initTabs.indexOf(defaultTab),
                tabs = initTabs,
                topShowComments = false,
                recentShowComments = false,
                recentFilterByBlogEntryId = null
            )
        )
    }

    val tabs by rememberCollect {
        context.settingsNews.flowOfCodeforcesTabs()
    }

    LaunchedEffect(key1 = tabs) {
        controller.updateTabs(newTabs = tabs)
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
data class CodeforcesNewsControllerData(
    val selectedIndex: Int,
    val tabs: List<CodeforcesTitle>,
    val topShowComments: Boolean,
    val recentShowComments: Boolean,
    val recentFilterByBlogEntryId: Int?
)

@Stable
class CodeforcesNewsController(
    private val viewModel: CodeforcesNewsViewModel,
    data: CodeforcesNewsControllerData
) {
    val pagerState = PagerState(currentPage = data.selectedIndex)

    private val tabsState = mutableStateOf(data.tabs)
    val tabs by tabsState

    suspend fun updateTabs(newTabs: List<CodeforcesTitle>) {
        val oldSelectedTab = currentTab
        val newIndex = newTabs.indexOf(oldSelectedTab).takeIf { it != -1 }
            ?: selectedTabIndex.coerceAtMost(newTabs.size - 1)
        if (newIndex != selectedTabIndex) {
            pagerState.scrollToPage(newIndex)
        }
        tabsState.value = newTabs
    }

    val currentTab: CodeforcesTitle
        get() = tabs[selectedTabIndex]

    val selectedTabIndex: Int
        get() = pagerState.currentPage

    fun isTabVisible(tab: CodeforcesTitle) = tab == currentTab && !pagerState.isScrollInProgress

    suspend fun scrollTo(tab: CodeforcesTitle) =
        //TODO: replace to animateScroll when lagging gone
        pagerState.scrollToPage(page = tabs.indexOf(tab))


    var topShowComments by mutableStateOf(data.topShowComments)

    var recentShowComments by mutableStateOf(data.recentShowComments)
    var recentFilterByBlogEntryId: Int? by mutableStateOf(data.recentFilterByBlogEntryId)


    private val badges = mutableMapOf<CodeforcesTitle, MutableState<Int>>()
    fun getBadgeCountState(tab: CodeforcesTitle): State<Int> = badges.getOrPut(tab) { mutableStateOf(0) }
    fun setBadgeCount(tab: CodeforcesTitle, count: Int) {
        badges.getOrPut(tab) { mutableStateOf(count) }.value = count
    }

    @Composable
    fun rememberLoadingStatusState(title: CodeforcesTitle) = rememberCollect { viewModel.flowOfLoadingStatus(title) }

    @Composable
    fun rememberLoadingStatusState() = rememberCollect { viewModel.flowOfLoadingStatus() }

    fun reload(title: CodeforcesTitle, context: Context) = viewModel.reload(title, context)
    fun reloadAll(context: Context) = viewModel.reloadAll(context)

    fun flowOfMainBlogEntries(context: Context) = viewModel.flowOfMainBlogEntries(context)
    fun flowOfTopBlogEntries(context: Context) = viewModel.flowOfTopBlogEntries(context)
    fun flowOfTopComments(context: Context) = viewModel.flowOfTopComments(context)
    fun flowOfRecentActions(context: Context) = viewModel.flowOfRecentActions(context)

    fun flowOfLostBlogEntries(context: Context) =
        context.lostBlogEntriesDao.flowOfLost().map { blogEntries ->
            blogEntries.sortedByDescending { it.timeStamp }
                .map {
                    CodeforcesBlogEntry(
                        id = it.id,
                        title = it.title,
                        authorHandle = it.authorHandle,
                        authorColorTag = it.authorColorTag,
                        creationTime = it.creationTime,
                        commentsCount = 0,
                        rating = 0
                    )
                }
        }


    fun addToFollow(handle: String, context: Context) = viewModel.addToFollowList(handle, context)
    fun updateFollowUsersInfo(context: Context) = viewModel.updateFollowUsersInfo(context)

    companion object {
        fun saver(viewModel: CodeforcesNewsViewModel) = Saver<CodeforcesNewsController, String>(
            save = {
                jsonCPS.encodeToString(CodeforcesNewsControllerData(
                    selectedIndex = it.selectedTabIndex,
                    tabs = it.tabs,
                    topShowComments = it.topShowComments,
                    recentShowComments = it.recentShowComments,
                    recentFilterByBlogEntryId = it.recentFilterByBlogEntryId
                ))
            },
            restore = {
                CodeforcesNewsController(
                    viewModel = viewModel,
                    data = jsonCPS.decodeFromString(it)
                )
            }
        )
    }
}