package com.demich.cps.news.follow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.LocalCodeforcesAccountManager
import com.demich.cps.Screen
import com.demich.cps.accounts.DialogAccountChooser
import com.demich.cps.news.codeforces.CodeforcesNewsViewModel
import com.demich.cps.room.CodeforcesUserBlog
import com.demich.cps.room.followListDao
import com.demich.cps.ui.*
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun NewsFollowScreen(
    navigator: CPSNavigator,
    newsViewModel: CodeforcesNewsViewModel
) {
    val context = context
    val followLoadingStatus by rememberCollect { newsViewModel.flowOfFollowUpdateLoadingStatus() }
    ProvideTimeEachMinute {
        CodeforcesFollowList(
            isRefreshing = { followLoadingStatus == LoadingStatus.LOADING }
        ) { handle ->
            newsViewModel.loadBlog(handle = handle, context = context)
            navigator.navigateTo(Screen.NewsCodeforcesBlog(handle = handle))
        }
    }

    //TODO: block if worker in progress

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeforcesFollowList(
    isRefreshing: () -> Boolean,
    onOpenBlog: (String) -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    val userBlogs by rememberCollect {
        context.followListDao.flowOfAllBlogs().map {
            it.sortedByDescending { it.id }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { userBlogs.firstOrNull()?.id }
            .drop(1)
            .collect {
                listState.animateScrollToItem(index = 0)
            }
    }

    var showDeleteDialogForBlog: CodeforcesUserBlog? by remember { mutableStateOf(null) }

    LazyColumnWithScrollBar(
        state = listState
    ) {
        itemsNotEmpty(
            items = userBlogs,
            key = { it.id }
        ) { userBlog ->
            ContentWithCPSDropdownMenu(
                modifier = Modifier.animateItemPlacement(),
                content = {
                    NewsFollowListItem(
                        userInfo = userBlog.userInfo,
                        blogEntriesCount = userBlog.blogEntries?.size,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .fillMaxWidth()
                    )
                },
                menuBuilder = {
                    val enabled = !isRefreshing()
                    CPSDropdownMenuItem(
                        title = "Show blog",
                        icon = CPSIcons.BlogEntry,
                        enabled = enabled,
                        onClick = { onOpenBlog(userBlog.handle) }
                    )
                    CPSDropdownMenuItem(
                        title = "Delete",
                        icon = CPSIcons.Delete,
                        enabled = enabled,
                        onClick = { showDeleteDialogForBlog = userBlog }
                    )
                }
            )
            Divider(modifier = Modifier.animateItemPlacement())
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

    CPSIconButton(icon = CPSIcons.Add) {
        showChooseDialog = true
    }

    if (showChooseDialog) {
        DialogAccountChooser(
            manager = LocalCodeforcesAccountManager.current,
            onDismissRequest = { showChooseDialog = false },
            onResult = { newsViewModel.addToFollowList(userInfo = it, context = context) }
        )
    }
}