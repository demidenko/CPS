package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.demich.cps.contests.Contest
import com.demich.cps.news.NewsTab
import com.demich.cps.news.NewsTabRow
import com.demich.cps.news.settingsNews
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun CodeforcesNewsScreen(
    navigator: CPSNavigator,
    viewModel: CodeforcesNewsViewModel
) {

    val context = context
    val settings = remember { context.settingsNews }

    val tabs = remember {
        mutableStateOf(
            listOf(
                CodeforcesTitle.MAIN,
                CodeforcesTitle.TOP,
                CodeforcesTitle.RECENT,
                CodeforcesTitle.LOST
            )
        )
    }

    val locale by rememberCollect { settings.codeforcesLocale.flow }

    val pagerState = rememberPagerState()

    LaunchedEffect(key1 = pagerState.currentPage) {
        val selectedTab = tabs.value[pagerState.currentPage]
        navigator.setSubtitle("news", "codeforces", selectedTab.name)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabsHeader(
            tabs = tabs,
            pagerState = pagerState,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalPager(
            count = tabs.value.size,
            state = pagerState,
            key = { tabs.value[it] },
            modifier = Modifier.fillMaxSize()
        ) { index ->
            val modifier = Modifier.fillMaxSize()
            when (tabs.value[index]) {
                CodeforcesTitle.MAIN -> CodeforcesNewsMainContent(
                    modifier = modifier,
                    viewModel = viewModel,
                    locale = locale
                )
                CodeforcesTitle.TOP -> CodeforcesNewsTopContent()
                CodeforcesTitle.RECENT -> CodeforcesNewsRecentContent()
                CodeforcesTitle.LOST -> CodeforcesNewsLostContent()
            }
        }
    }


}


@Composable
fun CodeforcesNewsMainContent(
    modifier: Modifier = Modifier,
    viewModel: CodeforcesNewsViewModel,
    locale: CodeforcesLocale
) {
    val context = context
    val scope = rememberCoroutineScope()

    val loadingStatus by viewModel.pageLoadingStatusState(CodeforcesTitle.MAIN)

    val blogEntries by rememberCollect { viewModel.flowOfMainBlogEntries(context) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { viewModel.reload(title = CodeforcesTitle.MAIN, locale = locale) },
        modifier = modifier
    ) {
        CodeforcesBlogEntries(
            blogEntries = blogEntries,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CodeforcesNewsTopContent() {

}

@Composable
fun CodeforcesNewsRecentContent() {

}

@Composable
fun CodeforcesNewsLostContent() {

}


@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TabsHeader(
    tabs: State<List<CodeforcesTitle>>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedTextColor: Color = cpsColors.textColor,
    unselectedTextColor: Color = cpsColors.textColorAdditional,
) {
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = platformIconPainter(platform = Contest.Platform.codeforces),
            contentDescription = null,
            tint = cpsColors.textColor,
            modifier = Modifier.padding(start = 8.dp, end = 6.dp)
        )
        NewsTabRow(pagerState = pagerState) {
            tabs.value.forEachIndexed { index, title ->
                NewsTab(
                    index = index,
                    title = title.name,
                    pagerState = pagerState,
                    selectedTextColor = selectedTextColor,
                    unselectedTextColor = unselectedTextColor,
                    modifier = Modifier.clickableNoRipple {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    }
}
