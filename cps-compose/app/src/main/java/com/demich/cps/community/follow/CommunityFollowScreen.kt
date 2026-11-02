package com.demich.cps.community.follow

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.community.LocalCommunityUnitsFactory
import com.demich.cps.community.codeforces.codeforcesCommunityViewModel
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesUserBlogInfo
import com.demich.cps.profiles.DialogProfileSelector
import com.demich.cps.profiles.managers.CodeforcesProfileManager
import com.demich.cps.profiles.managers.makeHandleSpan
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.utils.ProvideSystemTimeEachMinute
import com.demich.cps.utils.backgroundCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CPSNavigator.ScreenScope<Screen.CommunityFollowList>.NavContentCommunityFollowListScreen(
    navigateToBlogScreen: (Long) -> Unit
) {
    screenTitle = ScreenStaticTitleState("community", "codeforces", "follow", "list")

    CommunityFollowScreen(
        onShowBlogScreen = navigateToBlogScreen
    )

    bottomBar = communityFollowListBottomBarBuilder()
}

@Composable
private fun CommunityFollowScreen(
    onShowBlogScreen: (Long) -> Unit
) {
    val unitsFactory = LocalCommunityUnitsFactory.current
    val viewModel = codeforcesCommunityViewModel()
    val scope = backgroundCoroutineScope

    val loadingStatusState = viewModel.flowOfFollowUpdateLoadingStatus.collectAsState()

    val userBlogs by remember { unitsFactory.followRepository().flowOfUserBlogs() }
        .collectAsStateWithLifecycle(initialValue = null)

    ProvideSystemTimeEachMinute {
        CodeforcesFollowList(
            userBlogs = userBlogs,
            isRefreshing = { loadingStatusState.value == LOADING },
            onOpenBlog = onShowBlogScreen,
            onDeleteUser = { blogId ->
                scope.launch { unitsFactory.followRepository().remove(blogId = blogId) }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    //TODO: block if worker in progress

}

@Composable
private fun CodeforcesFollowList(
    userBlogs: List<CodeforcesUserBlogInfo>?,
    isRefreshing: () -> Boolean,
    onOpenBlog: (Long) -> Unit,
    onDeleteUser: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    var showDeleteDialogForBlog: CodeforcesUserBlogInfo? by remember { mutableStateOf(null) }

    LazyColumnOfData(
        state = listState,
        modifier = modifier,
        autoScrollPredicate = { _, _ -> true },
        items = userBlogs,
        key = { it.id }
    ) { userBlog ->
        ContentWithCPSDropdownMenu(
            modifier = Modifier.animateItem(),
            content = {
                CodeforcesUserBlogPreview(
                    userBlogInfo = userBlog,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            menuBuilder = {
                val enabled = !isRefreshing()
                CPSDropdownMenuItem(
                    title = "Show blog",
                    icon = CPSIcons.BlogEntry,
                    enabled = enabled,
                    onClick = { onOpenBlog(userBlog.id) }
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
                val result = userBlog.userProfile
                append("Delete ")
                append(CodeforcesProfileManager().makeHandleSpan(profileResult = result))
                append(" from follow list?")
            },
            onDismissRequest = { showDeleteDialogForBlog = null },
            onConfirmRequest = { onDeleteUser(userBlog.id) }
        )
    }
}

private fun communityFollowListBottomBarBuilder(): AdditionalBottomBarBuilder = {
    var showChooseDialog by remember { mutableStateOf(false) }

    CPSIconButton(icon = CPSIcons.Add) {
        showChooseDialog = true
    }

    if (showChooseDialog) {
        val scope = backgroundCoroutineScope
        val unitsFactory = LocalCommunityUnitsFactory.current

        DialogProfileSelector(
            manager = CodeforcesProfileManager(),
            initial = null,
            onDismissRequest = { showChooseDialog = false },
            onResult = {
                scope.launch {
                    unitsFactory.followRepository().addNewUser(result = it)
                }
            }
        )
    }
}