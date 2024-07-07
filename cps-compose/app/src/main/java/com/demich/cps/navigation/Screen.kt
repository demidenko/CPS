package com.demich.cps.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.ui.CPSIcons

enum class ScreenTypes(
    val route: String
) {
    accounts("accounts"),
    accountExpanded("account/{manager}"),
    accountSettings("account.settings/{manager}"),
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

    data object Accounts: RootScreen(ScreenTypes.accounts, icon = CPSIcons.Account)

    data class AccountExpanded(override val type: AccountManagerType)
        : AccountScreen(ScreenTypes.accountExpanded, type, true)

    data class AccountSettings(override val type: AccountManagerType)
        : AccountScreen(ScreenTypes.accountSettings, type, false)

    data object Community: RootScreen(ScreenTypes.community, icon = CPSIcons.Community)
    data object CommunitySettings: Screen(ScreenTypes.communitySettings, rootScreenType = ScreenTypes.community, enableBottomBar = false)
    data object CommunityFollowList: Screen(ScreenTypes.communityFollowList, rootScreenType = ScreenTypes.community)
    data class CommunityCodeforcesBlog(val handle: String)
        : Screen(ScreenTypes.communityCodeforcesBlog, rootScreenType = ScreenTypes.community) {
            override fun createPath(pattern: String) = pattern.replace("{handle}", handle)
        }

    data object Contests: RootScreen(ScreenTypes.contests, icon = CPSIcons.Contest)
    data object ContestsSettings: Screen(ScreenTypes.contestsSettings, rootScreenType = ScreenTypes.contests, enableBottomBar = false)

    data object Development: RootScreen(ScreenTypes.develop, icon = CPSIcons.Development)

}

sealed class RootScreen(
    screenType: ScreenTypes,
    val icon: ImageVector
): Screen(
    screenType = screenType,
    rootScreenType = screenType
)

sealed class AccountScreen(
    screenType: ScreenTypes,
    open val type: AccountManagerType,
    enableBottomBar: Boolean
): Screen(
    screenType = screenType,
    rootScreenType = ScreenTypes.accounts,
    enableBottomBar = enableBottomBar
) {
    final override fun createPath(pattern: String): String =
        pattern.replace("{manager}", type.name)
}

private val simpleScreens = arrayOf(
    Screen.Contests,
    Screen.Community,
    Screen.Accounts,
    Screen.ContestsSettings,
    Screen.CommunitySettings,
    Screen.CommunityFollowList,
    Screen.Development
)

fun NavBackStackEntry.getScreen(): Screen {
    return when(val route = destination.route) {
        ScreenTypes.accountExpanded.route -> {
            Screen.AccountExpanded(type = AccountManagerType.valueOf(requireString("manager")))
        }
        ScreenTypes.accountSettings.route -> {
            Screen.AccountSettings(type = AccountManagerType.valueOf(requireString("manager")))
        }
        ScreenTypes.communityCodeforcesBlog.route -> {
            Screen.CommunityCodeforcesBlog(handle = requireString("handle"))
        }
        else -> simpleScreens.first { it.screenType.route == route }
    }
}


private fun NavBackStackEntry.requireString(key: String): String =
    arguments?.getString(key)!!

