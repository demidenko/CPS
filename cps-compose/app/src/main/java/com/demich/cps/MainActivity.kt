package com.demich.cps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.AccountExpandedScreen
import com.demich.cps.accounts.AccountsScreen
import com.demich.cps.accounts.AccountsViewModel
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.contests.ContestsScreen
import com.demich.cps.contests.ContestsSettingsScreen
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.NewsSettingsScreen
import com.demich.cps.ui.CPSStatusBar
import com.demich.cps.ui.LocalUseOriginalColors
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
                accountsViewModel = viewModel()
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

    Scaffold(
        topBar = { CPSTopBar(
            navController = navController,
            currentScreen = currentScreen,
            cpsViewModels = cpsViewModels
        ) },
        bottomBar = { CPSBottomBar(
            navController = navController,
            currentScreen = currentScreen,
            cpsViewModels = cpsViewModels
        ) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Accounts.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Accounts.route) {
                AccountsScreen(navController, cpsViewModels.accountsViewModel)
            }
            composable(Screen.AccountExpanded.route) {
                val type = (it.getScreen() as Screen.AccountExpanded).type
                AccountExpandedScreen(type, navController)
            }
            composable(Screen.News.route) {
                NewsScreen(navController)
            }
            composable(Screen.NewsSettings.route) {
                NewsSettingsScreen()
            }
            composable(Screen.Contests.route) {
                ContestsScreen(navController)
            }
            composable(Screen.ContestsSettings.route) {
                ContestsSettingsScreen(navController)
            }
            composable(Screen.Development.route) {
                DevelopScreen(navController)
            }
        }
    }
}

class CPSViewModels(
    val accountsViewModel: AccountsViewModel
)



sealed class Screen(
    val route: String,
    val enableBottomBar: Boolean = true,
    private val root: Screen? = null
) {
    val rootScreen: Screen get() = root ?: this
    open val subtitle: String get() = "::$route"

    object Accounts: Screen("accounts")
    class AccountExpanded(val type: AccountManagers): Screen(route = route, root = Accounts) {
        companion object {
            const val route = "account/{manager}"
        }
        override val subtitle get() = "::accounts.$type"
    }

    object News: Screen("news")
    object NewsSettings: Screen("news.settings", root = News, enableBottomBar = false)

    object Contests: Screen("contests")
    object ContestsSettings: Screen("contests.settings", root = Contests, enableBottomBar = false)

    object Development: Screen("develop")

    companion object {
        fun all() = listOf(
            Accounts,
            News,
            NewsSettings,
            Contests,
            ContestsSettings,
            Development
        )
    }
}

fun NavBackStackEntry.getScreen(): Screen {
    val route = destination.route
    if (route == Screen.AccountExpanded.route) {
        val type = AccountManagers.valueOf(arguments?.getString("manager")!!)
        return Screen.AccountExpanded(type)
    }
    return Screen.all().first { it.route == route }
}


/*
accounts
    expanded account
        settings
news: tabs
    settings
    follow list
        user blog
contests
    settings
        ...?

 */

