package com.demich.cps.news.follow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.accounts.DialogAccountChooser
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.news.codeforces.CodeforcesNewsViewModel
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.room.CodeforcesUserBlog
import com.demich.cps.room.followListDao
import com.demich.cps.ui.*
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun NewsFollowScreen(
    navigator: CPSNavigator,
    newsViewModel: CodeforcesNewsViewModel
) {
    val context = context

    val manager = remember { CodeforcesAccountManager(context) }
    val currentTime by collectCurrentTimeEachMinute()
    CompositionLocalProvider(
        LocalCodeforcesAccountManager provides manager,
        LocalCurrentTime provides currentTime,
    ) {
        NewsFollowList { handle ->
            newsViewModel.loadBlog(handle = handle, context = context)
            navigator.navigateTo(Screen.NewsCodeforcesBlog(handle = handle))
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewsFollowList(
    onOpenBlog: (String) -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

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

    var showMenuForId: Int? by remember { mutableStateOf(null) }
    var showDeleteDialogForBlog: CodeforcesUserBlog? by remember { mutableStateOf(null) }

    LazyColumnWithScrollBar(
        state = listState
    ) {
        itemsNotEmpty(
            items = userBlogsState.value,
            key = { it.id }
        ) { userBlog ->
            ContentWithCPSDropdownMenu(
                expanded = userBlog.id == showMenuForId,
                onDismissRequest = { showMenuForId = null },
                modifier = Modifier
                    .clickable { showMenuForId = userBlog.id }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .animateItemPlacement(),
                content = {
                    NewsFollowListItem(
                        userInfo = userBlog.userInfo,
                        blogEntriesCount = userBlog.blogEntries?.size,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                },
                menuAlignment = Alignment.Center,
                menuBuilder = {
                    CPSDropdownMenuItem(title = "Show blog", icon = CPSIcons.BlogEntry) {
                        onOpenBlog(userBlog.handle)
                    }
                    CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete) {
                        showDeleteDialogForBlog = userBlog
                    }
                }
            )
            Divider()
        }
    }

    showDeleteDialogForBlog?.let { userBlog ->
        CPSDeleteDialog(
            title = buildAnnotatedString {
                append("Delete ")
                append(LocalCodeforcesAccountManager.current.makeHandleSpan(userInfo = userBlog.userInfo))
                append(" from follow list?")
            },
            onDismissRequest = { showDeleteDialogForBlog = null }
        ) {
            scope.launch {
                context.followListDao.remove(userBlog.handle)
            }
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