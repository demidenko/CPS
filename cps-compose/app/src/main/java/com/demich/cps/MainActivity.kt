package com.demich.cps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.AccountsScreen
import com.demich.cps.accounts.AccountsViewModel
import com.demich.cps.contests.ContestsScreen
import com.demich.cps.contests.ContestsSettingsScreen
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.NewsSettingsScreen
import com.demich.cps.ui.CPSStatusBar
import com.demich.cps.ui.LocalUseOriginalColors
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.CPSTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            val useOriginalColors by settingsUI.useOriginalColors.collectAsState()
            val darkLightMode by settingsUI.darkLightMode.collectAsState()

            CompositionLocalProvider(
                LocalUseOriginalColors provides useOriginalColors
            ) {
                CPSTheme(darkTheme = darkLightMode.isDarkMode()) {
                    CPSScaffold(
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun CPSScaffold(
    navController: NavHostController
) {
    //val context = context

    val systemUiController = rememberSystemUiController()

    CPSStatusBar(systemUiController)

    val cpsViewModels = CPSViewModels(
        accountsViewModel = viewModel()
    )

    Scaffold(
        topBar = { CPSTopBar(
            navController = navController
        ) },
        bottomBar = { CPSBottomBar(
            navController = navController,
            cpsViewModels = cpsViewModels,
            systemUiController = systemUiController
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
    val subtitle: String get() = "::$route"

    object Accounts: Screen("accounts")
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

fun NavDestination.getScreen(): Screen {
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

