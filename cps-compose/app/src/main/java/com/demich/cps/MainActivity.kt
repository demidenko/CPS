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
import com.demich.cps.accounts.ProfileSettingsScreen
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.profilesViewModel
import com.demich.cps.community.CommunityScreen
import com.demich.cps.community.codeforces.NavContentCodeforcesBlog
import com.demich.cps.community.codeforces.rememberCodeforcesCommunityController
import com.demich.cps.community.communityBottomBarBuilder
import com.demich.cps.community.communityMenuBuilder
import com.demich.cps.community.follow.CommunityFollowScreen
import com.demich.cps.community.follow.communityFollowListBottomBarBuilder
import com.demich.cps.community.settings.CommunitySettingsScreen
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.contests.NavContentContestsScreen
import com.demich.cps.contests.settings.ContestsSettingsScreen
import com.demich.cps.develop.DevelopScreen
import com.demich.cps.develop.developAdditionalBottomBarBuilder
import com.demich.cps.navigation.Screen
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
        with(navigator) { //second receiver
        navEntry<Screen.Profiles> { holder ->
            NavContentProfilesScreen(
                holder = holder,
                onExpandProfile = { type -> navigator.navigateTo(Screen.ProfileExpanded(type)) }
            )
        }

        navEntry<Screen.ProfileExpanded> { holder ->
            val context = context
            val profilesViewModel = profilesViewModel()
            NavContentProfilesExpandedScreen(
                holder = holder,
                onOpenSettings = {
                    val type = holder.screen.managerType
                    navigator.navigateTo(Screen.ProfileSettings(type))
                },
                onDeleteRequest = { manager ->
                    profilesViewModel.delete(manager, context)
                    navigator.popBack()
                }
            )
        }

        navEntry<Screen.ProfileSettings> { holder ->
            val type = holder.screen.managerType
            ProfileSettingsScreen(type)
            holder.setSubtitle("profiles", type.name, "settings")
        }

        navEntry<Screen.Community> { holder ->
            val controller = rememberCodeforcesCommunityController()
            CommunityScreen(controller = controller)
            holder.menu = communityMenuBuilder(
                controller = controller,
                onOpenSettings = { navigator.navigateTo(Screen.CommunitySettings) },
                onOpenFollowList = { navigator.navigateTo(Screen.CommunityFollowList) }
            )
            holder.bottomBar = communityBottomBarBuilder(
                controller = controller
            )
            holder.setSubtitle("community", "codeforces", controller.currentTab.name)
        }

        navEntry<Screen.CommunitySettings> { holder ->
            CommunitySettingsScreen()
            holder.setSubtitle("community", "settings")
        }

        navEntry<Screen.CommunityFollowList> { holder ->
            CommunityFollowScreen { handle ->
                navigator.navigateTo(Screen.CommunityCodeforcesBlog(handle = handle))
            }
            holder.bottomBar = communityFollowListBottomBarBuilder()
            holder.setSubtitle("community", "codeforces", "follow", "list")
        }

        navEntry<Screen.CommunityCodeforcesBlog> { holder ->
            NavContentCodeforcesBlog(holder = holder)
        }

        navEntry<Screen.Contests> { holder ->
            NavContentContestsScreen(
                holder = holder,
                onOpenSettings = { navigator.navigateTo(Screen.ContestsSettings) }
            )
        }

        navEntry<Screen.ContestsSettings> { holder ->
            ContestsSettingsScreen()
            holder.setSubtitle("contests", "settings")
        }

        navEntry<Screen.Development> { holder ->
            DevelopScreen()
            holder.bottomBar = developAdditionalBottomBarBuilder()
            holder.setSubtitle("develop")
        }
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