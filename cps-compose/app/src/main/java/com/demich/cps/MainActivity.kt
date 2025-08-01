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
import com.demich.cps.accounts.NavContentProfilesExpandedScreen
import com.demich.cps.accounts.NavContentProfilesScreen
import com.demich.cps.accounts.NavContentProfilesSettingsScreen
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.profilesViewModel
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
import com.demich.cps.ui.CPSScaffold
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.utils.context
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
        navigator.navEntry<Screen.Profiles>(false) {
            NavContentProfilesScreen(
                onExpandProfile = { type -> navigator.navigateTo(Screen.ProfileExpanded(type)) }
            )
        }

        navigator.navEntry<Screen.ProfileExpanded>(false) {
            val context = context
            val profilesViewModel = profilesViewModel()
            NavContentProfilesExpandedScreen(
                onOpenSettings = {
                    val type = screen.managerType
                    navigator.navigateTo(Screen.ProfileSettings(type))
                },
                onDeleteRequest = { manager ->
                    profilesViewModel.delete(manager, context)
                    navigator.popBack()
                }
            )
        }

        navigator.navEntry<Screen.ProfileSettings>(false) {
            NavContentProfilesSettingsScreen()
        }

        navigator.navEntry<Screen.Community> {
            NavContentCommunityScreen(
                onOpenSettings = { navigator.navigateTo(Screen.CommunitySettings) },
                onOpenFollowList = { navigator.navigateTo(Screen.CommunityFollowList) }
            )
        }

        navigator.navEntry<Screen.CommunitySettings>(false) {
            NavContentCommunitySettingsScreen()
        }

        navigator.navEntry<Screen.CommunityFollowList>(false) {
            NavContentCommunityFollowListScreen(
                onShowBlogScreen = { handle ->
                    navigator.navigateTo(Screen.CommunityCodeforcesBlog(handle = handle))
                }
            )
        }

        navigator.navEntry<Screen.CommunityCodeforcesBlog>(false) {
            NavContentCodeforcesBlog()
        }

        navigator.navEntry<Screen.Contests> {
            NavContentContestsScreen(
                onOpenSettings = { navigator.navigateTo(Screen.ContestsSettings) }
            )
        }

        navigator.navEntry<Screen.ContestsSettings>(false) {
            NavContentContestsSettingsScreen()
        }

        navigator.navEntry<Screen.Development>(false) {
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