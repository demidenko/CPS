package com.demich.cps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllOut
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.*
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.demich.cps.accounts.AccountsScreen
import com.demich.cps.contests.ContestsScreen
import com.demich.cps.contests.ContestsSettingsScreen
import com.demich.cps.news.NewsScreen
import com.demich.cps.news.NewsSettingsScreen
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.CPSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkLightMode by settingsUI.darkLightMode.collectAsState()
            val navController = rememberNavController()
            CPSTheme(
                darkTheme = darkLightMode.isDarkMode()
            ) {
                CPSScaffold(
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun CPSScaffold(
    navController: NavHostController
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    Scaffold(
        topBar = { CPSTopBar(navController, currentBackStackEntry) },
        bottomBar = { CPSBottomBar(navController, currentBackStackEntry) }
    ) {
        NavHost(navController = navController, startDestination = Screen.Accounts.route) {
            composable(Screen.Accounts.route) {
                AccountsScreen(navController)
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



sealed class Screen(
    val route: String,
    val enableBottomBar: Boolean = true,
    private val root: Screen? = null
) {
    val rootScreen: Screen get() = root ?: this
    val subtitle = "::$route"

    object Accounts: Screen("accounts")
    object News: Screen("news")
    object NewsSettings: Screen("news.settings", root = News, enableBottomBar = false)
    object Contests: Screen("contests")
    object ContestsSettings: Screen("contests.settings", root = Contests, enableBottomBar = false)
    object Development: Screen("develop")

    companion object {
        val majorScreens by lazy {
            listOf(
                Accounts to Icons.Rounded.Person,
                News to Icons.Default.Subtitles,
                Contests to Icons.Filled.EmojiEvents,
                Development to Icons.Default.AllOut
            )
        }
        val all by lazy {
            listOf(
                Accounts,
                News,
                NewsSettings,
                Contests,
                ContestsSettings,
                Development
            )
        }
    }
}

fun NavDestination.getScreen(): Screen {
    return Screen.all.first { it.route == route }
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

