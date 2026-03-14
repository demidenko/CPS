package com.demich.cps

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import com.demich.cps.community.NavContentCommunityScreen
import com.demich.cps.community.codeforces.NavContentCodeforcesBlog
import com.demich.cps.community.follow.NavContentCommunityFollowListScreen
import com.demich.cps.community.settings.NavContentCommunitySettingsScreen
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.contests.NavContentContestsScreen
import com.demich.cps.contests.settings.NavContentContestsSettingsScreen
import com.demich.cps.develop.NavContentDevelopmentScreen
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.navEntry
import com.demich.cps.navigation.rememberCPSNavigator
import com.demich.cps.profiles.NavContentProfilesExpandedScreen
import com.demich.cps.profiles.NavContentProfilesScreen
import com.demich.cps.profiles.NavContentProfilesSettingsScreen
import com.demich.cps.profiles.managers.CodeforcesAccountManager
import com.demich.cps.ui.CPSScaffold
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.workers.enqueueEnabledWorkers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.Default) {
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

    val navBuilder: NavGraphBuilder.() -> Unit = {
        navigator.navEntry<Screen.Profiles> {
            NavContentProfilesScreen(
                onExpandProfile = { platform ->
                    navigator.navigateTo(Screen.ProfileExpanded(platform))
                }
            )
        }

        navigator.navEntry<Screen.ProfileExpanded> {
            NavContentProfilesExpandedScreen(
                onOpenSettings = {
                    val platform = screen.platform
                    navigator.navigateTo(Screen.ProfileSettings(platform))
                },
                navigateBack = {
                    navigator.popBack()
                }
            )
        }

        navigator.navEntry<Screen.ProfileSettings> {
            NavContentProfilesSettingsScreen()
        }

        navigator.navEntry<Screen.Community>(includeFontPadding = true) {
            NavContentCommunityScreen(
                onOpenSettings = { navigator.navigateTo(Screen.CommunitySettings) },
                onOpenFollowList = { navigator.navigateTo(Screen.CommunityFollowList) }
            )
        }

        navigator.navEntry<Screen.CommunitySettings> {
            NavContentCommunitySettingsScreen()
        }

        navigator.navEntry<Screen.CommunityFollowList> {
            NavContentCommunityFollowListScreen(
                onShowBlogScreen = { handle ->
                    navigator.navigateTo(Screen.CommunityCodeforcesBlog(handle = handle))
                }
            )
        }

        navigator.navEntry<Screen.CommunityCodeforcesBlog> {
            NavContentCodeforcesBlog()
        }

        navigator.navEntry<Screen.Contests>(includeFontPadding = true) {
            NavContentContestsScreen(
                onOpenSettings = { navigator.navigateTo(Screen.ContestsSettings) }
            )
        }

        navigator.navEntry<Screen.ContestsSettings> {
            NavContentContestsSettingsScreen()
        }

        navigator.navEntry<Screen.Development> {
            NavContentDevelopmentScreen()
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
    context.settingsCommunity.codeforcesLocale.update { it }

    //workers
    context.enqueueEnabledWorkers()
}