package com.demich.cps.navigation

import androidx.navigation.NavBackStackEntry
import com.demich.cps.accounts.managers.AccountManagerType

enum class ScreenTypes(
    val route: String
) {
    profiles("profiles"),
    profileExpanded("profile/{manager}"),
    profileSettings("profile.settings/{manager}"),
    contests("contests"),
    contestsSettings("contests.settings"),
    community("community"),
    communitySettings("community.settings"),
    communityFollowList("community.follow"),
    communityCodeforcesBlog("codeforces.blog/{handle}"),
    develop("develop")
}

sealed class Screen(
    val screenType: ScreenTypes,
    val rootScreenType: ScreenTypes,
    val enableBottomBar: Boolean = true
) {

    protected open fun createPath(pattern: String): String = pattern

    val routePath get() = createPath(screenType.route)

    data object Profiles: RootScreen(ScreenTypes.profiles)

    data class ProfileExpanded(override val type: AccountManagerType)
        : ProfileScreen(ScreenTypes.profileExpanded, type, true)

    data class ProfileSettings(override val type: AccountManagerType)
        : ProfileScreen(ScreenTypes.profileSettings, type, false)

    data object Community: RootScreen(ScreenTypes.community)
    data object CommunitySettings: Screen(ScreenTypes.communitySettings, rootScreenType = ScreenTypes.community, enableBottomBar = false)
    data object CommunityFollowList: Screen(ScreenTypes.communityFollowList, rootScreenType = ScreenTypes.community)
    data class CommunityCodeforcesBlog(val handle: String)
        : Screen(ScreenTypes.communityCodeforcesBlog, rootScreenType = ScreenTypes.community) {
            override fun createPath(pattern: String) = pattern.replace("{handle}", handle)
        }

    data object Contests: RootScreen(ScreenTypes.contests)
    data object ContestsSettings: Screen(ScreenTypes.contestsSettings, rootScreenType = ScreenTypes.contests, enableBottomBar = false)

    data object Development: RootScreen(ScreenTypes.develop)

}

sealed class RootScreen(
    screenType: ScreenTypes
): Screen(
    screenType = screenType,
    rootScreenType = screenType
)

sealed class ProfileScreen(
    screenType: ScreenTypes,
    open val type: AccountManagerType,
    enableBottomBar: Boolean
): Screen(
    screenType = screenType,
    rootScreenType = ScreenTypes.profiles,
    enableBottomBar = enableBottomBar
) {
    final override fun createPath(pattern: String): String =
        pattern.replace("{manager}", type.name)
}

private val simpleScreens = arrayOf(
    Screen.Contests,
    Screen.Community,
    Screen.Profiles,
    Screen.ContestsSettings,
    Screen.CommunitySettings,
    Screen.CommunityFollowList,
    Screen.Development
)

fun NavBackStackEntry.getScreen(): Screen {
    return when(val route = destination.route) {
        ScreenTypes.profileExpanded.route -> {
            Screen.ProfileExpanded(type = AccountManagerType.valueOf(requireString("manager")))
        }
        ScreenTypes.profileSettings.route -> {
            Screen.ProfileSettings(type = AccountManagerType.valueOf(requireString("manager")))
        }
        ScreenTypes.communityCodeforcesBlog.route -> {
            Screen.CommunityCodeforcesBlog(handle = requireString("handle"))
        }
        else -> simpleScreens.first { it.screenType.route == route }
    }
}


private fun NavBackStackEntry.requireString(key: String): String =
    arguments?.getString(key)!!

