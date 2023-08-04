package com.demich.cps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.AccountExpandedScreen
import com.demich.cps.accounts.AccountSettingsScreen
import com.demich.cps.accounts.AccountsScreen
import com.demich.cps.accounts.accountExpandedMenuBuilder
import com.demich.cps.accounts.accountsBottomBarBuilder
import com.demich.cps.accounts.accountsViewModel
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.ContestsScreen
import com.demich.cps.contests.contestsBottomBarBuilder
import com.demich.cps.contests.contestsMenuBuilder
import com.demich.cps.contests.contestsViewModel
import com.demich.cps.contests.rememberCombinedLoadingStatusState
import com.demich.cps.contests.rememberContestsFilterController
import com.demich.cps.contests.settings.ContestsSettingsScreen
import com.demich.cps.develop.DevelopScreen
import com.demich.cps.develop.developAdditionalBottomBarBuilder
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.navigation.getScreen
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.codeforces.CodeforcesBlogScreen
import com.demich.cps.news.codeforces.codeforcesNewsViewModel
import com.demich.cps.news.codeforces.rememberCodeforcesNewsController
import com.demich.cps.news.follow.NewsFollowScreen
import com.demich.cps.news.follow.newsFollowListBottomBarBuilder
import com.demich.cps.news.newsBottomBarBuilder
import com.demich.cps.news.newsMenuBuilder
import com.demich.cps.news.settings.NewsSettingsScreen
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.rememberCPSNavigator
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.toLoadingStatus
import com.demich.cps.workers.enqueueEnabledWorkers
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                this@MainActivity.enqueueEnabledWorkers()
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CompositionLocalProvider(LocalCodeforcesAccountManager provides CodeforcesAccountManager(context)) {
                CPSTheme {
                    CPSContent()
                }
            }
        }
    }
}

@Composable
private fun CPSContent() {
    val navigator = rememberCPSNavigator(navController = rememberNavController())

    navigator.ColorizeNavAndStatusBars()

    CPSScaffold(navigator = navigator)
}


@Composable
private fun CPSScaffold(
    navigator: CPSNavigator
) {
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

    val navBuilder: NavGraphBuilder.() -> Unit = remember(key1 = navigator) {
        {
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
                val accountsViewModel = accountsViewModel()
                val type = (holder.screen as Screen.AccountExpanded).type
                var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
                AccountExpandedScreen(
                    type = type,
                    showDeleteDialog = showDeleteDialog,
                    onDeleteRequest = { manager ->
                        navigator.popBack()
                        accountsViewModel.delete(manager)
                    },
                    onDismissDeleteDialog = { showDeleteDialog = false },
                    setBottomBarContent = holder.bottomBarSetter
                )
                holder.menu = accountExpandedMenuBuilder(
                    type = type,
                    navigator = navigator,
                    onShowDeleteDialog = { showDeleteDialog = true }
                )
                holder.setSubtitle("accounts", type.name)
            }
            cpsComposable(ScreenTypes.accountSettings) { holder ->
                val type = (holder.screen as Screen.AccountSettings).type
                AccountSettingsScreen(type)
                holder.setSubtitle("accounts", type.name, "settings")
            }

            cpsComposable(ScreenTypes.news) { holder ->
                val controller = rememberCodeforcesNewsController()
                NewsScreen(controller = controller)
                holder.menu = newsMenuBuilder(
                    navigator = navigator,
                    controller = controller
                )
                holder.bottomBar = newsBottomBarBuilder(
                    controller = controller
                )
                holder.setSubtitle("news", "codeforces", controller.currentTab.name)
            }
            cpsComposable(ScreenTypes.newsSettings) { holder ->
                NewsSettingsScreen()
                holder.setSubtitle("news", "settings")
            }
            cpsComposable(ScreenTypes.newsFollowList) { holder ->
                NewsFollowScreen(navigator = navigator)
                holder.bottomBar = newsFollowListBottomBarBuilder()
                holder.setSubtitle("news", "codeforces", "follow", "list")
            }
            cpsComposable(ScreenTypes.newsCodeforcesBlog) { holder ->
                val handle = (holder.screen as Screen.NewsCodeforcesBlog).handle
                val context = context
                val newsViewModel = codeforcesNewsViewModel()
                val blogEntriesResult by newsViewModel.blogEntriesResult.collectAsState()

                val loadingDataId = rememberSaveable(key = handle) {
                    Random.nextLong().also {
                        newsViewModel.loadBlog(handle = handle, context = context, id = it)
                    }
                }

                CodeforcesBlogScreen(
                    blogEntries = { blogEntriesResult?.getOrNull() ?: emptyList() },
                    loadingStatus = { blogEntriesResult.toLoadingStatus() }
                )
                holder.setSubtitle("news", "codeforces", "blog")
            }

            cpsComposable(ScreenTypes.contests) { holder ->
                val context = context
                val contestsViewModel = contestsViewModel()
                val filterController = rememberContestsFilterController()
                val loadingStatus by rememberCombinedLoadingStatusState()
                val isReloading = { loadingStatus == LoadingStatus.LOADING }
                val onReload = { contestsViewModel.reloadEnabledPlatforms(context) }
                ContestsScreen(
                    filterController = filterController,
                    isReloading = isReloading,
                    onReload = onReload
                )
                holder.bottomBar = contestsBottomBarBuilder(
                    filterController = filterController,
                    loadingStatus = { loadingStatus },
                    onReloadClick = onReload
                )
                holder.menu = contestsMenuBuilder(
                    onOpenSettings = { navigator.navigateTo(Screen.ContestsSettings) },
                    isReloading = isReloading
                )
                holder.setSubtitle("contests")
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
    }

    Scaffold(
        topBar = { navigator.TopBar() },
        bottomBar = { navigator.BottomBar() },
        modifier = Modifier.systemBarsPadding()
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            navigator.NavHost(builder = navBuilder)
            CPSBottomProgressBarsColumn(
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

val LocalCodeforcesAccountManager = staticCompositionLocalOf<CodeforcesAccountManager> {
    throw IllegalAccessException()
}