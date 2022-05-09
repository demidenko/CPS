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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.*
import com.demich.cps.contests.ContestsScreen
import com.demich.cps.contests.ContestsViewModel
import com.demich.cps.contests.contestsBottomBarBuilder
import com.demich.cps.contests.contestsMenuBuilder
import com.demich.cps.contests.settings.ContestsSettingsScreen
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.NewsSettingsScreen
import com.demich.cps.news.newsBottomBarBuilder
import com.demich.cps.news.newsMenuBuilder
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSStatusBar
import com.demich.cps.ui.LocalUseOriginalColors
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cpsViewModels = CPSViewModels(
                accountsViewModel = viewModel(),
                contestsViewModel = viewModel(),
                progressBarsViewModel = viewModel()
            )
            CPSTheme {
                val useOriginalColors by rememberCollect { settingsUI.useOriginalColors.flow }
                CompositionLocalProvider(LocalUseOriginalColors provides useOriginalColors) {
                    CPSContent(cpsViewModels = cpsViewModels)
                }
            }
        }
    }
}

@Composable
private fun CPSContent(
    cpsViewModels: CPSViewModels,
    navController: NavHostController = rememberNavController()
) {
    val currentScreenState = remember(navController) {
        navController.currentBackStackEntryFlow.map { it.getScreen() }
    }.collectAsState(initial = null)

    NavigationAndStatusBars(
        currentScreen = currentScreenState.value
    )

    CPSScaffold(
        cpsViewModels = cpsViewModels,
        navController = navController,
        currentScreenState = currentScreenState
    )
}

private class BuildersHolder(
    private val currentScreenState: State<Screen?>,
    val screen: Screen,

    private val menuBuilderState: MutableState<CPSMenuBuilder?>,
    private val bottomBarBuilderState: MutableState<AdditionalBottomBarBuilder?>
) {
    var menu: CPSMenuBuilder?
        get() = menuBuilderState.value
        set(value) { if (screen == currentScreenState.value) menuBuilderState.value = value }

    var bottomBar: AdditionalBottomBarBuilder?
        get() = bottomBarBuilderState.value
        set(value) { if (screen == currentScreenState.value) bottomBarBuilderState.value = value }

    val menuSetter get() = menuBuilderState.component2()
    val bottomBarSetter get() = bottomBarBuilderState.component2()
}

@Composable
private fun CPSScaffold(
    cpsViewModels: CPSViewModels,
    navController: NavHostController,
    currentScreenState: State<Screen?>
) {

    val menuBuilderState = remember { mutableStateOf<CPSMenuBuilder?>(null) }
    val bottomBarBuilderState = remember { mutableStateOf<AdditionalBottomBarBuilder?>(null) }

    fun NavGraphBuilder.cpsComposable(route: String, content: @Composable (BuildersHolder) -> Unit) {
        composable(route) {
            val holder: BuildersHolder = remember {
                BuildersHolder(
                    currentScreenState = currentScreenState,
                    screen = it.getScreen(),
                    menuBuilderState = menuBuilderState,
                    bottomBarBuilderState = bottomBarBuilderState
                )
            }.apply {
                menu = null
                bottomBar = null
            }
            content(holder)
        }
    }

    val navBuilder: NavGraphBuilder.() -> Unit = remember(
        navController, cpsViewModels
    ) {
        {
            cpsComposable(Screen.Accounts.route) { holder ->
                val reorderEnabledState = rememberSaveable { mutableStateOf(false) }
                AccountsScreen(
                    accountsViewModel = cpsViewModels.accountsViewModel,
                    onExpandAccount = { type -> navController.navigate(route = "account/$type") },
                    onSetAdditionalMenu = holder.menuSetter,
                    reorderEnabledState = reorderEnabledState
                )
                holder.bottomBar = accountsBottomBarBuilder(
                    cpsViewModels = cpsViewModels,
                    reorderEnabledState = reorderEnabledState
                )
            }
            cpsComposable(Screen.AccountExpanded.route) { holder ->
                val type = (holder.screen as Screen.AccountExpanded).type
                var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
                AccountExpandedScreen(
                    type = type,
                    showDeleteDialog = showDeleteDialog,
                    onDeleteRequest = { manager ->
                        navController.popBackStack()
                        cpsViewModels.accountsViewModel.delete(manager)
                    },
                    onDismissDeleteDialog = { showDeleteDialog = false },
                    setBottomBarContent = holder.bottomBarSetter
                )
                holder.menu = accountExpandedMenuBuilder(
                    type = type,
                    navController = navController,
                    onShowDeleteDialog = { showDeleteDialog = true }
                )
            }
            cpsComposable(Screen.AccountSettings.route) { holder ->
                val type = (holder.screen as Screen.AccountSettings).type
                AccountSettingsScreen(type)
            }

            cpsComposable(Screen.News.route) { holder ->
                NewsScreen(navController)
                holder.menu = newsMenuBuilder(navController)
                holder.bottomBar = newsBottomBarBuilder()
            }
            cpsComposable(Screen.NewsSettings.route) {
                NewsSettingsScreen()
            }

            cpsComposable(Screen.Contests.route) { holder ->
                val searchEnabled = rememberSaveable { mutableStateOf(false) }
                ContestsScreen(
                    contestsViewModel = cpsViewModels.contestsViewModel,
                    searchEnabledState = searchEnabled
                )
                holder.bottomBar = contestsBottomBarBuilder(
                    contestsViewModel = cpsViewModels.contestsViewModel,
                    onEnableSearch = { searchEnabled.value = true }
                )
                holder.menu = contestsMenuBuilder(
                    navController = navController,
                    contestsViewModel = cpsViewModels.contestsViewModel
                )
            }
            cpsComposable(Screen.ContestsSettings.route) {
                ContestsSettingsScreen()
            }

            cpsComposable(Screen.Development.route) { holder ->
                DevelopScreen()
                holder.bottomBar = developAdditionalBottomBarBuilder(cpsViewModels.progressBarsViewModel)
            }
        }
    }

    Scaffold(
        topBar = { CPSTopBar(
            currentScreen = currentScreenState.value,
            additionalMenu = menuBuilderState.value
        ) },
        bottomBar = { CPSBottomBar(
            navController = navController,
            currentScreen = currentScreenState.value,
            additionalBottomBar = bottomBarBuilderState.value
        ) }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            val startRoute = with(context) {
                remember { runBlocking { settingsUI.startScreenRoute() } }
            }
            NavHost(
                navController = navController,
                startDestination = startRoute,
                builder = navBuilder
            )
            CPSBottomProgressBarsColumn(
                progressBarsViewModel = cpsViewModels.progressBarsViewModel,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun NavigationAndStatusBars(
    currentScreen: Screen?,
    systemUiController: SystemUiController = rememberSystemUiController()
) {
    systemUiController.setNavigationBarColor(
        color = cpsColors.backgroundNavigation,
        darkIcons = MaterialTheme.colors.isLight
    )

    CPSStatusBar(
        systemUiController = systemUiController,
        currentScreen = currentScreen
    )
}

class CPSViewModels(
    val accountsViewModel: AccountsViewModel,
    val contestsViewModel: ContestsViewModel,
    val progressBarsViewModel: ProgressBarsViewModel
)


