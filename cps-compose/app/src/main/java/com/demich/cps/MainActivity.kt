package com.demich.cps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.*
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.contests.ContestsScreen
import com.demich.cps.contests.ContestsSettingsScreen
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.NewsSettingsScreen
import com.demich.cps.ui.CPSDropdownMenuScope
import com.demich.cps.ui.CPSStatusBar
import com.demich.cps.ui.LocalUseOriginalColors
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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

    var menu: (@Composable CPSDropdownMenuScope.() -> Unit)? by remember { mutableStateOf(null) }

    fun NavGraphBuilder.cpsComposable(route: String, content: @Composable (NavBackStackEntry) -> Unit) {
        //TODO: NavHost(): graph rebuilds after every navigation :(
        composable(route) {
            menu = null
            content(it)
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
            cpsViewModels = cpsViewModels
        ) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Accounts.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            cpsComposable(Screen.Accounts.route) {
                AccountsScreen(navController, cpsViewModels.accountsViewModel)
            }
            cpsComposable(Screen.AccountExpanded.route) {
                val type = (it.getScreen() as Screen.AccountExpanded).type
                var showDeleteDialog by remember { mutableStateOf(false) }
                AccountExpandedScreen(
                    type = type,
                    navController = navController,
                    accountsViewModel = cpsViewModels.accountsViewModel,
                    showDeleteDialog = showDeleteDialog,
                    onDismissDeleteDialog = { showDeleteDialog = false }
                )
                menu = {
                    val context = context
                    CPSDropdownMenuItem(title = "Delete", icon = Icons.Default.DeleteForever) {
                        showDeleteDialog = true
                    }
                    CPSDropdownMenuItem(title = "Settings", icon = Icons.Default.Settings) {
                        navController.navigate(Screen.AccountSettings.route(type))
                    }
                    CPSDropdownMenuItem(title = "Origin", icon = Icons.Default.Photo) {
                        val url = runBlocking {
                            context.allAccountManagers.first { it.type == type }.getSavedInfo().link()
                        }
                        context.startActivity(makeIntentOpenUrl(url))
                    }
                }
            }
            cpsComposable(Screen.AccountSettings.route) {
                val type = (it.getScreen() as Screen.AccountSettings).type
                AccountSettingsScreen(type)
            }

            cpsComposable(Screen.News.route) {
                NewsScreen(navController)
                menu = {
                    CPSDropdownMenuItem(title = "Settings", icon = Icons.Default.Settings) {
                        navController.navigate(Screen.NewsSettings.route)
                    }
                    CPSDropdownMenuItem(title = "Follow List", icon = Icons.Rounded.PeopleAlt) {
                        //TODO Open FollowList
                    }
                }
            }
            cpsComposable(Screen.NewsSettings.route) {
                NewsSettingsScreen()
            }

            cpsComposable(Screen.Contests.route) {
                ContestsScreen(navController)
            }
            cpsComposable(Screen.ContestsSettings.route) {
                ContestsSettingsScreen(navController)
            }

            cpsComposable(Screen.Development.route) {
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
    class AccountExpanded(val type: AccountManagers)
        : Screen(route = route, root = Accounts) {
        override val subtitle get() = "::accounts.$type"
        companion object {
            const val route = "account/{manager}"
        }
    }
    class AccountSettings(val type: AccountManagers)
        : Screen(route = route, root = Accounts, enableBottomBar = false) {
        override val subtitle get() = "::accounts.$type.settings"
        companion object {
            const val route = "account_settings/{manager}"
            fun route(type: AccountManagers) = "account_settings/$type"
        }
    }

    object News: Screen("news")
    object NewsSettings: Screen("news.settings", root = News, enableBottomBar = false)

    object Contests: Screen("contests")
    object ContestsSettings: Screen("contests.settings", root = Contests, enableBottomBar = false)

    object Development: Screen("develop")

}

fun NavBackStackEntry.getScreen(): Screen {
    val route = destination.route
    if (route == Screen.AccountExpanded.route) {
        val type = AccountManagers.valueOf(arguments?.getString("manager")!!)
        return Screen.AccountExpanded(type)
    }
    if (route == Screen.AccountSettings.route) {
        val type = AccountManagers.valueOf(arguments?.getString("manager")!!)
        return Screen.AccountSettings(type)
    }
    return listOf(
        Screen.Accounts,
        Screen.News,
        Screen.NewsSettings,
        Screen.Contests,
        Screen.ContestsSettings,
        Screen.Development
    ).first { it.route == route }
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

