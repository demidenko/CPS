package com.demich.cps

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.demich.cps.accounts.AccountExpandedScreen
import com.demich.cps.accounts.AccountSettingsScreen
import com.demich.cps.accounts.AccountsScreen
import com.demich.cps.accounts.accountExpandedMenuBuilder
import com.demich.cps.accounts.accountsBottomBarBuilder
import com.demich.cps.accounts.accountsViewModel
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.community.CommunityScreen
import com.demich.cps.community.codeforces.CodeforcesBlogScreen
import com.demich.cps.community.codeforces.codeforcesCommunityViewModel
import com.demich.cps.community.codeforces.rememberCodeforcesCommunityController
import com.demich.cps.community.communityBottomBarBuilder
import com.demich.cps.community.communityMenuBuilder
import com.demich.cps.community.follow.CommunityFollowScreen
import com.demich.cps.community.follow.communityFollowListBottomBarBuilder
import com.demich.cps.community.settings.CommunitySettingsScreen
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.contests.ContestsScreen
import com.demich.cps.contests.contestsBottomBarBuilder
import com.demich.cps.contests.contestsMenuBuilder
import com.demich.cps.contests.contestsViewModel
import com.demich.cps.contests.rememberCombinedLoadingStatusState
import com.demich.cps.contests.rememberContestsListController
import com.demich.cps.contests.settings.ContestsSettingsScreen
import com.demich.cps.develop.DevelopScreen
import com.demich.cps.develop.developAdditionalBottomBarBuilder
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.navigation.getScreen
import com.demich.cps.navigation.rememberCPSNavigator
import com.demich.cps.ui.CPSScaffold
import com.demich.cps.ui.filter.rememberFilterState
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.currentDataKey
import com.demich.cps.workers.enqueueEnabledWorkers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                appStartUp(this@MainActivity)
            }
        }

        setContent {
            CompositionLocalProvider(LocalCodeforcesAccountManager provides CodeforcesAccountManager()) {
                CPSTheme {
                    CPSContent()
                }
            }
        }
    }
}

@Composable
private fun CPSContent() {
    val navigator = rememberCPSNavigator()

    fun NavGraphBuilder.cpsComposable(
        screenType: ScreenTypes,
        content: @Composable (CPSNavigator.DuringCompositionHolder) -> Unit
    ) {
        composable(screenType.route) {
            val holder = remember {
                navigator.DuringCompositionHolder(it.getScreen()).apply {
                    menu = null
                    bottomBar = null
                }
            }
            content(holder)
        }
    }

    val navBuilder: NavGraphBuilder.() -> Unit = {
        cpsComposable(ScreenTypes.accounts) { holder ->
            var reorderEnabled by rememberSaveable { mutableStateOf(false) }
            AccountsScreen(
                onExpandAccount = { type -> navigator.navigateTo(Screen.AccountExpanded(type)) },
                onSetAdditionalMenu = holder.menuSetter,
                reorderEnabled = { reorderEnabled },
                enableReorder = { reorderEnabled = true }
            )
            holder.bottomBar = accountsBottomBarBuilder(
                reorderEnabled = { reorderEnabled },
                onReorderDone = { reorderEnabled = false }
            )
            holder.setSubtitle("accounts")
        }
        cpsComposable(ScreenTypes.accountExpanded) { holder ->
            val context = context
            val accountsViewModel = accountsViewModel()
            val type = (holder.screen as Screen.AccountExpanded).type
            var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
            AccountExpandedScreen(
                type = type,
                showDeleteDialog = showDeleteDialog,
                onDeleteRequest = { manager ->
                    navigator.popBack()
                    accountsViewModel.delete(manager, context)
                },
                onDismissDeleteDialog = { showDeleteDialog = false },
                setBottomBarContent = holder.bottomBarSetter
            )
            holder.menu = accountExpandedMenuBuilder(
                type = type,
                onShowDeleteDialog = { showDeleteDialog = true },
                onOpenSettings = { navigator.navigateTo(Screen.AccountSettings(type)) }
            )
            holder.setSubtitle("accounts", type.name)
        }
        cpsComposable(ScreenTypes.accountSettings) { holder ->
            val type = (holder.screen as Screen.AccountSettings).type
            AccountSettingsScreen(type)
            holder.setSubtitle("accounts", type.name, "settings")
        }

        cpsComposable(ScreenTypes.community) { holder ->
            val controller = rememberCodeforcesCommunityController()
            CommunityScreen(controller = controller)
            holder.menu = communityMenuBuilder(
                controller = controller,
                onOpenSettings = { navigator.navigateTo(Screen.CommunitySettings) },
                onOpenFollowList = { navigator.navigateTo(Screen.CommunityFollowList) }
            )
            holder.bottomBar = communityBottomBarBuilder(
                controller = controller
            )
            holder.setSubtitle("community", "codeforces", controller.currentTab.name)
        }
        cpsComposable(ScreenTypes.communitySettings) { holder ->
            CommunitySettingsScreen()
            holder.setSubtitle("community", "settings")
        }
        cpsComposable(ScreenTypes.communityFollowList) { holder ->
            CommunityFollowScreen { handle ->
                navigator.navigateTo(Screen.CommunityCodeforcesBlog(handle = handle))
            }
            holder.bottomBar = communityFollowListBottomBarBuilder()
            holder.setSubtitle("community", "codeforces", "follow", "list")
        }
        cpsComposable(ScreenTypes.communityCodeforcesBlog) { holder ->
            val handle = (holder.screen as Screen.CommunityCodeforcesBlog).handle
            val newsViewModel = codeforcesCommunityViewModel()
            val blogEntriesResult by newsViewModel
                .flowOfBlogEntriesResult(handle, context, key = currentDataKey)
                .collectAsState()
            CodeforcesBlogScreen(blogEntriesResult = { blogEntriesResult })
            holder.setSubtitle("community", "codeforces", "blog")
        }

        cpsComposable(ScreenTypes.contests) { holder ->
            val context = context
            val contestsViewModel = contestsViewModel()
            val contestsListController = rememberContestsListController()
            val filterState = rememberFilterState()
            val loadingStatus by rememberCombinedLoadingStatusState()
            val isReloading = { loadingStatus == LoadingStatus.LOADING }
            val onReload = { contestsViewModel.reloadEnabledPlatforms(context) }
            ContestsScreen(
                contestsListController = contestsListController,
                filterState = filterState,
                isReloading = isReloading,
                onReload = onReload
            )
            holder.bottomBar = contestsBottomBarBuilder(
                contestsListController = contestsListController,
                filterState = filterState,
                loadingStatus = { loadingStatus },
                onReloadClick = onReload
            )
            holder.menu = contestsMenuBuilder(
                onOpenSettings = { navigator.navigateTo(Screen.ContestsSettings) },
                isReloading = isReloading
            )

            when (contestsListController.showFinished) {
                true -> holder.setSubtitle("contests", "finished")
                false -> holder.setSubtitle("contests")
            }

        }
        cpsComposable(ScreenTypes.contestsSettings) { holder ->
            ContestsSettingsScreen()
            holder.setSubtitle("contests", "settings")
        }

        cpsComposable(ScreenTypes.develop) { holder ->
            DevelopScreen()
            holder.bottomBar = developAdditionalBottomBarBuilder()
            holder.setSubtitle("develop")
        }
    }

    CPSScaffold(
        navigator = navigator
    ) {
        navigator.NavHost(builder = navBuilder)
    }

}

val LocalCodeforcesAccountManager = staticCompositionLocalOf<CodeforcesAccountManager> {
    throw IllegalAccessException()
}

private suspend fun appStartUp(context: Context) {
    //init items with dynamic defaults
    //TODO: not perfect solution, default still can run multiple times
    withContext(Dispatchers.IO) {
        context.settingsCommunity.codeforcesLocale.update { it }
    }

    //workers
    context.enqueueEnabledWorkers()
}