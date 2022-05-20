@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news.codeforces

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Composable
fun rememberCodeforcesNewsController(

): CodeforcesNewsController {
    val context = context

    val tabs by rememberCollect {
        context.settingsNews.flowOfCodeforcesTabs()
    }

    val controller =  rememberSaveable(saver = CodeforcesNewsController.saver) {
        val settings = context.settingsNews
        val initTabs = runBlocking { settings.flowOfCodeforcesTabs().first() }
        val defaultTab = runBlocking { settings.codeforcesDefaultTab() }
        CodeforcesNewsController(
            pagerState = PagerState(currentPage = initTabs.indexOf(defaultTab)),
            tabs = initTabs
        )
    }

    LaunchedEffect(key1 = tabs) {
        controller.updateTabs(newTabs = tabs)
    }

    return controller
}

@Stable
class CodeforcesNewsController(
    val pagerState: PagerState,
    tabs: List<CodeforcesTitle>
) {

    private val tabsState = mutableStateOf(tabs)

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

    companion object {
        val saver = listSaver<CodeforcesNewsController, String>(
            save = {
                buildList {
                    add(it.selectedTabIndex.toString())
                    addAll(it.tabs.map { tab -> tab.name })
                }
            },
            restore = { list ->
                val index = list[0].toInt()
                CodeforcesNewsController(
                    pagerState = PagerState(currentPage = index),
                    tabs = list.drop(1).map { CodeforcesTitle.valueOf(it) }
                )
            }
        )
    }
}