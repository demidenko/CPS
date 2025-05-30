package com.demich.cps.community.codeforces

import androidx.compose.runtime.Composable
import com.demich.cps.community.codeforces.CodeforcesCommunityController.RecentPageType
import com.demich.cps.community.codeforces.CodeforcesCommunityController.TopPageType
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons

@Composable
fun CodeforcesCommunityBottomBar(
    controller: CodeforcesCommunityController,
) {
    when (controller.currentTab) {
        CodeforcesTitle.TOP -> {
            TopPageButton(
                pageType = controller.topPageType,
                onPageChange = { controller.topPageType = it }
            )
        }
        CodeforcesTitle.RECENT -> {
            RecentPageButton(
                pageType = controller.recentPageType,
                onPageChange = { controller.recentPageType = it }
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
    CommentsModeButton(isOn = pageType == TopPageType.Comments) { isOn ->
        onPageChange(if (isOn) TopPageType.Comments else TopPageType.BlogEntries)
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