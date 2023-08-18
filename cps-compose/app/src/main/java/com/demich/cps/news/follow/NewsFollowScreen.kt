package com.demich.cps.news.follow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.LocalCodeforcesAccountManager
import com.demich.cps.accounts.DialogAccountChooser
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import com.demich.cps.navigation.Screen
import com.demich.cps.news.codeforces.codeforcesNewsViewModel
import com.demich.cps.room.followListDao
import com.demich.cps.ui.*
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Composable
fun NewsFollowScreen(navigator: CPSNavigator) {
    val context = context
    val scope = rememberCoroutineScope()

    val newsViewModel = codeforcesNewsViewModel()

    val followLoadingStatus by rememberCollect { newsViewModel.flowOfFollowUpdateLoadingStatus() }

    val userBlogs by rememberCollect {
        context.followListDao.flowOfAllBlogs().map {
            it.sortedByDescending { it.id }
        }
    }

    ProvideTimeEachMinute {
        CodeforcesFollowList(
            userBlogs = { userBlogs },
            isRefreshing = { followLoadingStatus == LoadingStatus.LOADING },
            onOpenBlog = { handle ->
                navigator.navigateTo(Screen.NewsCodeforcesBlog(handle = handle))
            },
            onDeleteUser = { handle ->
                scope.launch {
                    context.followListDao.remove(handle)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    //TODO: block if worker in progress

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeforcesFollowList(
    userBlogs: () -> List<CodeforcesUserBlog>,
    isRefreshing: () -> Boolean,
    onOpenBlog: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(key1 = listState) {
        /* Cases:
            1) delete not first -> removing animation
            2) delete first + scroll on top -> removing animation
            3) delete first + scroll not top -> ?? (whatever)
            4) add new + scroll on top -> adding animation + scroll to top
            5) add new + scroll not top -> adding animation + scroll to top
         */
        snapshotFlow { userBlogs().let { it.size to it.firstOrNull()?.id } }
            .distinctUntilChangedBy { it.second } //wait for first id changed
            .drop(1) //ignore first because of first composition
            .collect { (listSize, _) ->
                snapshotFlow { listState.layoutInfo.totalItemsCount }
                    .takeWhile { it != listSize }.collect() //wait for listState have same size
                with(listState) {
                    if (firstVisibleItemIndex > 0) animateScrollToItem(index = 0)
                }
            }
    }

    var showDeleteDialogForBlog: CodeforcesUserBlog? by remember { mutableStateOf(null) }

    LazyColumnWithScrollBar(
        state = listState,
        modifier = modifier
    ) {
        itemsNotEmpty(
            items = userBlogs(),
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
            onDismissRequest = { showDeleteDialogForBlog = null },
            onConfirmRequest = { onDeleteUser(userBlog.handle) }
        )
    }
}

fun newsFollowListBottomBarBuilder(): AdditionalBottomBarBuilder = {
    val context = context
    val newsViewModel = codeforcesNewsViewModel()

    var showChooseDialog by remember { mutableStateOf(false) }

    CPSIconButton(icon = CPSIcons.Add) {
        showChooseDialog = true
    }

    if (showChooseDialog) {
        DialogAccountChooser(
            manager = LocalCodeforcesAccountManager.current,
            initialUserInfo = null,
            onDismissRequest = { showChooseDialog = false },
            onResult = { newsViewModel.addToFollowList(userInfo = it, context = context) }
        )
    }
}