package com.demich.cps.community.follow

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.demich.cps.LocalCodeforcesAccountManager
import com.demich.cps.accounts.DialogAccountChooser
import com.demich.cps.accounts.managers.makeHandleSpan
import com.demich.cps.accounts.userinfo.asResult
import com.demich.cps.community.codeforces.codeforcesCommunityViewModel
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.collectAsStateWithLifecycle
import com.demich.cps.utils.context
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Composable
fun CommunityFollowScreen(
    onShowBlogScreen: (String) -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    val communityViewModel = codeforcesCommunityViewModel()

    val followLoadingStatus by collectAsState { communityViewModel.flowOfFollowUpdateLoadingStatus() }

    val userBlogs by collectAsStateWithLifecycle {
        context.followListDao.flowOfAllBlogs().map {
            it.sortedByDescending { it.id }
        }
    }

    ProvideTimeEachMinute {
        CodeforcesFollowList(
            userBlogs = { userBlogs },
            isRefreshing = { followLoadingStatus == LoadingStatus.LOADING },
            onOpenBlog = onShowBlogScreen,
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

@Composable
private fun CodeforcesFollowList(
    userBlogs: () -> List<CodeforcesUserBlog>,
    isRefreshing: () -> Boolean,
    onOpenBlog: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, userBlogs) {
        /* Cases:
            1) delete not first -> removing animation [ok by default]
            2) delete first + no scroll -> removing animation [ok by default]
            3) delete first + scroll on top -> removing animation [ok by default]
            4) delete first + scroll not top -> ?? (whatever) [ok by default]
            5) add first -> adding animation + scroll to top [NO by default]
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

    LazyColumnOfData(
        state = listState,
        modifier = modifier,
        items = userBlogs,
        key = CodeforcesUserBlog::id
    ) { userBlog ->
        ContentWithCPSDropdownMenu(
            modifier = Modifier.animateItem(),
            content = {
                CommunityFollowListItem(
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
        Divider(modifier = Modifier.animateItem())
    }

    showDeleteDialogForBlog?.let { userBlog ->
        CPSDeleteDialog(
            title = buildAnnotatedString {
                append("Delete ")
                append(LocalCodeforcesAccountManager.current.makeHandleSpan(profileResult = userBlog.userInfo.asResult()))
                append(" from follow list?")
            },
            onDismissRequest = { showDeleteDialogForBlog = null },
            onConfirmRequest = { onDeleteUser(userBlog.handle) }
        )
    }
}

fun communityFollowListBottomBarBuilder(): AdditionalBottomBarBuilder = {
    val context = context
    val communityViewModel = codeforcesCommunityViewModel()

    var showChooseDialog by remember { mutableStateOf(false) }

    CPSIconButton(icon = CPSIcons.Add) {
        showChooseDialog = true
    }

    if (showChooseDialog) {
        DialogAccountChooser(
            manager = LocalCodeforcesAccountManager.current,
            initialUserInfo = null,
            onDismissRequest = { showChooseDialog = false },
            onResult = { communityViewModel.addToFollowList(result = it.asResult(), context = context) }
        )
    }
}