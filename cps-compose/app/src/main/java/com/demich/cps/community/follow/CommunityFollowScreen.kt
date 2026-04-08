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
import com.demich.cps.LocalCodeforcesProfileManager
import com.demich.cps.community.codeforces.codeforcesCommunityViewModel
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlogEntity
import com.demich.cps.features.codeforces.follow.database.blogSize
import com.demich.cps.features.codeforces.follow.database.profileResult
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.profiles.DialogProfileSelector
import com.demich.cps.profiles.managers.makeHandleSpan
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.utils.ProvideSystemTimeEachMinute
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.collectAsStateWithLifecycle
import com.demich.cps.utils.context
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Composable
private fun CommunityFollowScreen(
    onShowBlogScreen: (String) -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    val communityViewModel = codeforcesCommunityViewModel()

    val followLoadingStatus by collectAsState { communityViewModel.flowOfFollowUpdateLoadingStatus() }

    val userBlogs by collectAsStateWithLifecycle { context.followRepository.flowOfUserBlogs() }

    ProvideSystemTimeEachMinute {
        CodeforcesFollowList(
            userBlogs = { userBlogs },
            isRefreshing = { followLoadingStatus == LOADING },
            onOpenBlog = onShowBlogScreen,
            onDeleteUser = { handle ->
                scope.launch {
                    context.followRepository.remove(handle)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    //TODO: block if worker in progress

}

@Composable
fun CPSNavigator.ScreenScope<Screen.CommunityFollowList>.NavContentCommunityFollowListScreen(
    onShowBlogScreen: (String) -> Unit
) {
    screenTitle = ScreenStaticTitleState("community", "codeforces", "follow", "list")

    CommunityFollowScreen(
        onShowBlogScreen = onShowBlogScreen
    )

    bottomBar = communityFollowListBottomBarBuilder()
}

@Composable
private fun CodeforcesFollowList(
    userBlogs: () -> List<CodeforcesUserBlogEntity>,
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

    var showDeleteDialogForBlog: CodeforcesUserBlogEntity? by remember { mutableStateOf(null) }

    LazyColumnOfData(
        state = listState,
        modifier = modifier,
        items = userBlogs,
        key = CodeforcesUserBlogEntity::id
    ) { userBlog ->
        ContentWithCPSDropdownMenu(
            modifier = Modifier.animateItem(),
            content = {
                CommunityFollowListItem(
                    profile = userBlog.profileResult,
                    blogEntriesCount = userBlog.blogSize,
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
                val result = userBlog.profileResult
                append("Delete ")
                append(LocalCodeforcesProfileManager.current.makeHandleSpan(profileResult = result))
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
        DialogProfileSelector(
            manager = LocalCodeforcesProfileManager.current,
            initial = null,
            onDismissRequest = { showChooseDialog = false },
            onResult = { communityViewModel.addToFollowList(result = it, context = context) }
        )
    }
}