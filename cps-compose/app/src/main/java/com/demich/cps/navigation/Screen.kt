package com.demich.cps.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.demich.cps.accounts.managers.AccountManagerType
import kotlinx.serialization.Serializable


@Serializable
sealed interface Screen {
    val rootScreen: RootScreen

    @Serializable
    sealed interface RootScreen: Screen {
        override val rootScreen: RootScreen
            get() = this
    }

    interface NoBottomBarScreen

    @Serializable
    data object Profiles: RootScreen

    interface ProfileScreen {
        val managerType: AccountManagerType
    }

    @Serializable
    data class ProfileExpanded(override val managerType: AccountManagerType): Screen, ProfileScreen {
        override val rootScreen: RootScreen
            get() = Profiles
    }

    @Serializable
    data class ProfileSettings(override val managerType: AccountManagerType): Screen, ProfileScreen, NoBottomBarScreen {
        override val rootScreen: RootScreen
            get() = Profiles
    }

    @Serializable
    data object Community: RootScreen

    @Serializable
    data object CommunitySettings: Screen, NoBottomBarScreen {
        override val rootScreen: RootScreen
            get() = Community
    }

    @Serializable
    data object CommunityFollowList: Screen {
        override val rootScreen: RootScreen
            get() = Community
    }

    @Serializable
    data class CommunityCodeforcesBlog(val handle: String): Screen {
        override val rootScreen: RootScreen
            get() = Community
    }

    @Serializable
    data object Contests: RootScreen

    @Serializable
    data object ContestsSettings: Screen, NoBottomBarScreen {
        override val rootScreen: RootScreen
            get() = Contests
    }

    @Serializable
    data object Development: RootScreen

}

fun NavBackStackEntry.getScreen(): Screen {
    screenClasses.forEach {
        if (destination.hasRoute(it)) return toRoute(it)
    }

    error("unknown route ${destination.route}")
}

private val screenClasses = listOf(
    Screen.Contests::class,
    Screen.Community::class,
    Screen.Profiles::class,
    Screen.Development::class,

    Screen.ProfileExpanded::class,
    Screen.ProfileSettings::class,

    Screen.ContestsSettings::class,

    Screen.CommunityFollowList::class,
    Screen.CommunityCodeforcesBlog::class,
    Screen.CommunitySettings::class

)
