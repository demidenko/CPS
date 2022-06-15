package com.demich.cps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.*
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.*
import com.demich.cps.contests.settings.ContestsSettingsScreen
import com.demich.cps.develop.DevelopScreen
import com.demich.cps.develop.developAdditionalBottomBarBuilder
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.codeforces.CodeforcesBlogScreen
import com.demich.cps.news.codeforces.CodeforcesNewsViewModel
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.news.codeforces.rememberCodeforcesNewsController
import com.demich.cps.news.follow.NewsFollowScreen
import com.demich.cps.news.follow.newsFollowListBottomBarBuilder
import com.demich.cps.news.newsBottomBarBuilder
import com.demich.cps.news.newsMenuBuilder
import com.demich.cps.news.settings.NewsSettingsScreen
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.LocalUseOriginalColors
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.ui.rememberCPSNavigator
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.cps.workers.enqueueEnabledWorkers

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            this@MainActivity.enqueueEnabledWorkers()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = context
            val cpsViewModels = CPSViewModels(
                accountsViewModel = viewModel(),
                newsViewModel = viewModel(),
                contestsViewModel = viewModel(),
                progressBarsViewModel = viewModel()
            )
            CPSTheme {
                val useOriginalColors by rememberCollect { settingsUI.useOriginalColors.flow }
                CompositionLocalProvider(
                    LocalUseOriginalColors provides useOriginalColors,
                    LocalContentAlpha provides 1f,
                    LocalCodeforcesAccountManager provides remember { CodeforcesAccountManager(context) }
                ) {
                    CPSContent(cpsViewModels = cpsViewModels)
                }
            }
        }
    }
}

@Composable
private fun CPSContent(
    cpsViewModels: CPSViewModels
) {
    val navigator = rememberCPSNavigator(navController = rememberNavController())

    navigator.ColorizeNavAndStatusBars()

    CPSScaffold(
        cpsViewModels = cpsViewModels,
        navigator = navigator
    )
}


@Composable
private fun CPSScaffold(
    cpsViewModels: CPSViewModels,
    navigator: CPSNavigator
) {

    fun NavGraphBuilder.cpsComposable(screenType: ScreenTypes, content: @Composable (CPSNavigator.DuringCompositionHolder) -> Unit) {
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

    val navBuilder: NavGraphBuilder.() -> Unit = remember(
        navigator, cpsViewModels
    ) {
        {
            cpsComposable(ScreenTypes.accounts) { holder ->
                val reorderEnabledState = rememberSaveable { mutableStateOf(false) }
                AccountsScreen(
                    accountsViewModel = cpsViewModels.accountsViewModel,
                    onExpandAccount = { type -> navigator.navigateTo(Screen.AccountExpanded(type)) },
                    onSetAdditionalMenu = holder.menuSetter,
                    reorderEnabledState = reorderEnabledState
                )
                holder.bottomBar = accountsBottomBarBuilder(
                    cpsViewModels = cpsViewModels,
                    reorderEnabledState = reorderEnabledState
                )
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("accounts")
                }
            }
            cpsComposable(ScreenTypes.accountExpanded) { holder ->
                val type = (holder.screen as Screen.AccountExpanded).type
                var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
                AccountExpandedScreen(
                    type = type,
                    showDeleteDialog = showDeleteDialog,
                    onDeleteRequest = { manager ->
                        navigator.popBack()
                        cpsViewModels.accountsViewModel.delete(manager)
                    },
                    onDismissDeleteDialog = { showDeleteDialog = false },
                    setBottomBarContent = holder.bottomBarSetter
                )
                holder.menu = accountExpandedMenuBuilder(
                    type = type,
                    navigator = navigator,
                    onShowDeleteDialog = { showDeleteDialog = true }
                )
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("accounts", type.name)
                }
            }
            cpsComposable(ScreenTypes.accountSettings) { holder ->
                val type = (holder.screen as Screen.AccountSettings).type
                AccountSettingsScreen(type)
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("accounts", type.name, "settings")
                }
            }

            cpsComposable(ScreenTypes.news) { holder ->
                val controller = rememberCodeforcesNewsController(cpsViewModels.newsViewModel)
                NewsScreen(
                    navigator = navigator,
                    controller = controller
                )
                holder.menu = newsMenuBuilder(
                    navigator = navigator,
                    controller = controller
                )
                holder.bottomBar = newsBottomBarBuilder(
                    controller = controller
                )
            }
            cpsComposable(ScreenTypes.newsSettings) {
                NewsSettingsScreen()
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("news", "settings")
                }
            }
            cpsComposable(ScreenTypes.newsFollowList) { holder ->
                NewsFollowScreen(
                    navigator = navigator,
                    newsViewModel = cpsViewModels.newsViewModel
                )
                holder.bottomBar = newsFollowListBottomBarBuilder(
                    newsViewModel = cpsViewModels.newsViewModel
                )
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("news", "codeforces", "follow", "list")
                }
            }
            cpsComposable(ScreenTypes.newsCodeforcesBlog) { holder ->
                val handle = (holder.screen as Screen.NewsCodeforcesBlog).handle
                CodeforcesBlogScreen(
                    blogEntriesState = cpsViewModels.newsViewModel.blogEntriesState,
                    loadingStatus = cpsViewModels.newsViewModel.blogLoadingStatus
                )
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("news", "codeforces", "blog")
                }
            }

            cpsComposable(ScreenTypes.contests) { holder ->
                val filterController = rememberContestsFilterController()
                ContestsScreen(
                    contestsViewModel = cpsViewModels.contestsViewModel,
                    filterController = filterController
                )
                holder.bottomBar = contestsBottomBarBuilder(
                    contestsViewModel = cpsViewModels.contestsViewModel,
                    filterController = filterController
                )
                holder.menu = contestsMenuBuilder(
                    navigator = navigator,
                    contestsViewModel = cpsViewModels.contestsViewModel
                )
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("contests")
                }
            }
            cpsComposable(ScreenTypes.contestsSettings) {
                ContestsSettingsScreen()
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("contests", "settings")
                }
            }

            cpsComposable(ScreenTypes.develop) { holder ->
                DevelopScreen()
                holder.bottomBar = developAdditionalBottomBarBuilder(cpsViewModels.progressBarsViewModel)
                LaunchedEffect(Unit) {
                    navigator.setSubtitle("develop")
                }
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
                progressBarsViewModel = cpsViewModels.progressBarsViewModel,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

class CPSViewModels(
    val accountsViewModel: AccountsViewModel,
    val newsViewModel: CodeforcesNewsViewModel,
    val contestsViewModel: ContestsViewModel,
    val progressBarsViewModel: ProgressBarsViewModel
)


