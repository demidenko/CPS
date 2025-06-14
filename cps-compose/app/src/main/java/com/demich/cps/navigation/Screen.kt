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
    data class ProfileSettings(override val managerType: AccountManagerType): Screen, ProfileScreen {
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

// navigation lib.....
fun NavBackStackEntry.getScreen(): Screen {
    toRouteOrNull<Screen.Profiles>()?.let { return it }
    toRouteOrNull<Screen.ProfileExpanded>()?.let { return it }
    toRouteOrNull<Screen.ProfileSettings>()?.let { return it }

    toRouteOrNull<Screen.Community>()?.let { return it }
    toRouteOrNull<Screen.CommunitySettings>()?.let { return it }
    toRouteOrNull<Screen.CommunityFollowList>()?.let { return it }
    toRouteOrNull<Screen.CommunityCodeforcesBlog>()?.let { return it }

    toRouteOrNull<Screen.Contests>()?.let { return it }
    toRouteOrNull<Screen.ContestsSettings>()?.let { return it }

    toRouteOrNull<Screen.Development>()?.let { return it }

    error("unknown route ${destination.route}")
}

private inline fun <reified S: Screen> NavBackStackEntry.toRouteOrNull(): S? {
    if (destination.hasRoute<S>()) return toRoute<S>()
    return null
}