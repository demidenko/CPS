@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.lostBlogEntriesDao
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.rememberCollect
import com.demich.cps.data.api.CodeforcesBlogEntry
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
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
                recentFilterByBlogEntryId = null
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
data class CodeforcesNewsControllerData(
    val selectedTab: CodeforcesTitle,
    val topShowComments: Boolean,
    val recentShowComments: Boolean,
    val recentFilterByBlogEntryId: Int?
)

@Stable
class CodeforcesNewsController(
    private val viewModel: CodeforcesNewsViewModel,
    private val tabsState: State<List<CodeforcesTitle>>,
    data: CodeforcesNewsControllerData
) {
    val tabs by tabsState

    //TODO: future support for dynamic tabs (selectedIndex can be out of bounds)
    val pagerState = PagerState(
        currentPage = tabs.indexOf(data.selectedTab)
            .takeIf { it != -1 } ?: 0
    )

    val currentTab: CodeforcesTitle
        get() = tabs[pagerState.currentPage]

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
        fun saver(
            viewModel: CodeforcesNewsViewModel,
            tabsState: State<List<CodeforcesTitle>>
        ) = Saver<CodeforcesNewsController, String>(
            save = {
                jsonCPS.encodeToString(CodeforcesNewsControllerData(
                    selectedTab = it.currentTab,
                    topShowComments = it.topShowComments,
                    recentShowComments = it.recentShowComments,
                    recentFilterByBlogEntryId = it.recentFilterByBlogEntryId
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