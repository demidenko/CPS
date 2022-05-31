package com.demich.cps.news.follow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.DialogAccountChooser
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.news.codeforces.CodeforcesNewsViewModel
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.room.followListDao
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.itemsNotEmpty
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.map

@Composable
fun NewsFollowList() {
    val context = context

    val manager = remember { CodeforcesAccountManager(context) }
    val currentTime by collectCurrentTimeEachMinute()
    CompositionLocalProvider(
        LocalCodeforcesAccountManager provides manager,
        LocalCurrentTime provides currentTime,
    ) {
        NewsFollowListItems()
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewsFollowListItems() {
    val context = context

    val userBlogsState = rememberCollect {
        context.followListDao.flowOfAll().map {
            it.sortedByDescending { it.id }
        }
    }

    val listState = rememberLazyListState()
    val firstId by remember {
        derivedStateOf { userBlogsState.value.firstOrNull()?.id }
    }
    LaunchedEffect(firstId) {
        listState.animateScrollToItem(index = 0)
    }

    LazyColumnWithScrollBar(
        state = listState
    ) {
        itemsNotEmpty(
            items = userBlogsState.value,
            key = { it.id }
        ) {
            NewsFollowListItem(
                userInfo = it.userInfo,
                blogEntriesCount = it.blogEntries?.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .animateItemPlacement()
            )
            Divider()
        }
    }
}

fun newsFollowListBottomBarBuilder(
    newsViewModel: CodeforcesNewsViewModel
): AdditionalBottomBarBuilder = {
    val context = context

    var showChooseDialog by remember { mutableStateOf(false) }

    CPSIconButton(
        icon = CPSIcons.Add,
        enabled = newsViewModel.followLoadingStatus != LoadingStatus.LOADING
    ) {
        showChooseDialog = true
    }

    if (showChooseDialog) {
        DialogAccountChooser(
            manager = CodeforcesAccountManager(context),
            onDismissRequest = { showChooseDialog = false },
            onResult = { newsViewModel.addToFollowList(userInfo = it, context = context) }
        )
    }
}