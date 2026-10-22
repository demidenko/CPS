package com.demich.cps.community.codeforces

import androidx.compose.runtime.Composable
import com.demich.cps.community.codeforces.CodeforcesCommunityPagerState.RecentPageType
import com.demich.cps.community.codeforces.CodeforcesCommunityPagerState.TopPageType
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons

@Composable
fun CodeforcesCommunityBottomBar(
    pagerState: CodeforcesCommunityPagerState,
) {
    when (pagerState.currentTab) {
        TOP -> {
            TopPageButton(
                pageType = pagerState.topPageType,
                onPageChange = { pagerState.topPageType = it }
            )
        }
        RECENT -> {
            RecentPageButton(
                pageType = pagerState.recentPageType,
                onPageChange = { pagerState.recentPageType = it }
            )
        }
        else -> Unit
    }
}

@Composable
private fun TopPageButton(
    pageType: TopPageType,
    onPageChange: (TopPageType) -> Unit
) {
    CommentsModeButton(isOn = pageType == Comments) { isOn ->
        onPageChange(if (isOn) Comments else BlogEntries)
    }
}

@Composable
private fun RecentPageButton(
    pageType: RecentPageType,
    onPageChange: (RecentPageType) -> Unit
) {
    when (pageType) {
        RecentPageType.RecentFeed, RecentPageType.RecentComments -> {
            CommentsModeButton(isOn = pageType == RecentPageType.RecentComments) { isOn ->
                onPageChange(if (isOn) RecentPageType.RecentComments else RecentPageType.RecentFeed)
            }
        }
        is RecentPageType.BlogEntryRecentComments -> {
            CPSIconButton(icon = CPSIcons.ArrowBack) {
                onPageChange(RecentPageType.RecentFeed)
            }
        }
    }
}

@Composable
private fun CommentsModeButton(
    isOn: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    CPSIconButton(
        icon = CPSIcons.Comments,
        onState = isOn
    ) {
        onModeChange(!isOn)
    }
}