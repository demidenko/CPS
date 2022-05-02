package com.demich.cps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.*
import com.demich.cps.contests.*
import com.demich.cps.contests.settings.ContestsSettingsScreen
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.NewsSettingsScreen
import com.demich.cps.news.newsBottomBarBuilder
import com.demich.cps.news.newsMenuBuilder
import com.demich.cps.ui.CPSStatusBar
import com.demich.cps.ui.LocalUseOriginalColors
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cpsViewModels = CPSViewModels(
                accountsViewModel = viewModel(),
                contestsViewModel = viewModel(),
                progressBarsViewModel = viewModel()
            )
            val darkLightMode by rememberCollect { settingsUI.darkLightMode.flow }
            CPSTheme(darkTheme = darkLightMode.isDarkMode()) {
                val systemUiController = rememberSystemUiController()
                systemUiController.setNavigationBarColor(
                    color = cpsColors.backgroundNavigation,
                    darkIcons = MaterialTheme.colors.isLight
                )
                val useOriginalColors by rememberCollect { settingsUI.useOriginalColors.flow }
                CompositionLocalProvider(LocalUseOriginalColors provides useOriginalColors) {
                    CPSScaffold(
                        cpsViewModels,
                        systemUiController
                    )
                }
            }
        }
    }
}

@Composable
fun CPSScaffold(
    cpsViewModels: CPSViewModels,
    systemUiController: SystemUiController
) {
    val navController = rememberNavController()
    val currentScreen by remember(navController) {
        navController.currentBackStackEntryFlow.map { it.getScreen() }
    }.collectAsState(initial = null)

    CPSStatusBar(
        systemUiController = systemUiController,
        currentScreen = currentScreen
    )

    var menu: CPSMenuBuilder? by remember { mutableStateOf(null) }
    var bottomBar: AdditionalBottomBarBuilder? by remember { mutableStateOf(null) }

    fun NavGraphBuilder.cpsComposable(route: String, content: @Composable (NavBackStackEntry) -> Unit) {
        composable(route) {
            menu = null
            bottomBar = null
            content(it)
        }
    }

    val navBuilder: NavGraphBuilder.() -> Unit = remember(
        navController, cpsViewModels
    ) {
        {
            cpsComposable(Screen.Accounts.route) {
                AccountsScreen(
                    accountsViewModel = cpsViewModels.accountsViewModel,
                    onExpandAccount = { type -> navController.navigate(route = "account/$type") },
                    onSetAdditionalMenu = { menu = it }
                )
                bottomBar = accountsBottomBarBuilder(cpsViewModels)
            }
            cpsComposable(Screen.AccountExpanded.route) {
                val type = (it.getScreen() as Screen.AccountExpanded).type
                var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
                AccountExpandedScreen(
                    type = type,
                    showDeleteDialog = showDeleteDialog,
                    onDeleteRequest = { manager ->
                        navController.popBackStack()
                        cpsViewModels.accountsViewModel.delete(manager)
                    },
                    onDismissDeleteDialog = { showDeleteDialog = false },
                    setBottomBarContent = { content -> bottomBar = content }
                )
                menu = accountExpandedMenuBuilder(
                    type = type,
                    navController = navController,
                    onShowDeleteDialog = { showDeleteDialog = true }
                )
            }
            cpsComposable(Screen.AccountSettings.route) {
                val type = (it.getScreen() as Screen.AccountSettings).type
                AccountSettingsScreen(type)
            }

            cpsComposable(Screen.News.route) {
                NewsScreen(navController)
                menu = newsMenuBuilder(navController)
                bottomBar = newsBottomBarBuilder()
            }
            cpsComposable(Screen.NewsSettings.route) {
                NewsSettingsScreen()
            }

            cpsComposable(Screen.Contests.route) {
                val searchEnabled = rememberSaveable { mutableStateOf(false) }
                ContestsScreen(
                    contestsViewModel = cpsViewModels.contestsViewModel,
                    searchEnabledState = searchEnabled
                )
                bottomBar = contestsBottomBarBuilder(
                    contestsViewModel = cpsViewModels.contestsViewModel,
                    onEnableSearch = { searchEnabled.value = true }
                )
                menu = contestsMenuBuilder(
                    navController = navController,
                    contestsViewModel = cpsViewModels.contestsViewModel
                )
            }
            cpsComposable(Screen.ContestsSettings.route) {
                ContestsSettingsScreen(navController)
            }

            cpsComposable(Screen.Development.route) {
                DevelopScreen(navController)
                bottomBar = developAdditionalBottomBarBuilder(cpsViewModels.progressBarsViewModel)
            }
        }
    }

    Scaffold(
        topBar = { CPSTopBar(
            currentScreen = currentScreen,
            additionalMenu = menu
        ) },
        bottomBar = { CPSBottomBar(
            navController = navController,
            currentScreen = currentScreen,
            additionalBottomBar = bottomBar
        ) }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Accounts.route,
                builder = navBuilder
            )
            CPSBottomProgressBarsColumn(
                progressBarsViewModel = cpsViewModels.progressBarsViewModel,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

class CPSViewModels(
    val accountsViewModel: AccountsViewModel,
    val contestsViewModel: ContestsViewModel,
    val progressBarsViewModel: ProgressBarsViewModel
)


