@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


@Composable
fun rememberCodeforcesNewsController(
    viewModel: CodeforcesNewsViewModel
): CodeforcesNewsController {
    val context = context

    val tabs by rememberCollect {
        context.settingsNews.flowOfCodeforcesTabs()
    }

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
                recentShowComments = false
            )
        )
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
    val recentShowComments: Boolean
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
        tabsState.value = newTabs
        if (newIndex != selectedTabIndex) {
            pagerState.scrollToPage(newIndex)
        }
    }

    val currentTab: CodeforcesTitle
        get() = tabs[selectedTabIndex]

    val selectedTabIndex: Int
        get() = pagerState.currentPage


    var topShowComments by mutableStateOf(data.topShowComments)

    var recentShowComments by mutableStateOf(data.recentShowComments)


    @Composable
    fun rememberLoadingStatusState(title: CodeforcesTitle) = remember {
        viewModel.pageLoadingStatusState(title)
    }

    @Composable
    fun rememberLoadingStatusState() = viewModel.rememberLoadingStatusState()

    fun reload(title: CodeforcesTitle, context: Context) = viewModel.reload(title, context)
    fun reloadAll(context: Context) = viewModel.reloadAll(context)

    fun flowOfMainBlogEntries(context: Context) = viewModel.flowOfMainBlogEntries(context)
    fun flowOfTopBlogEntries(context: Context) = viewModel.flowOfTopBlogEntries(context)
    fun flowOfTopComments(context: Context) = viewModel.flowOfTopComments(context)
    fun flowOfRecentActions(context: Context) = viewModel.flowOfRecentActions(context)

    companion object {
        fun saver(viewModel: CodeforcesNewsViewModel) = Saver<CodeforcesNewsController, String>(
            save = {
                jsonCPS.encodeToString(CodeforcesNewsControllerData(
                    selectedIndex = it.selectedTabIndex,
                    tabs = it.tabs,
                    topShowComments = it.topShowComments,
                    recentShowComments = it.recentShowComments
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