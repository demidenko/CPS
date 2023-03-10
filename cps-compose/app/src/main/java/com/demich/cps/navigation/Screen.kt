package com.demich.cps.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.ui.CPSIcons

enum class ScreenTypes(
    val route: String
) {
    accounts("accounts"),
    accountExpanded("account/{manager}"),
    accountSettings("account.settings/{manager}"),
    contests("contests"),
    contestsSettings("contests.settings"),
    news("news"),
    newsSettings("news.settings"),
    newsFollowList("news.follow"),
    newsCodeforcesBlog("codeforces.blog/{handle}"),
    develop("develop")
}

sealed class Screen(
    val screenType: ScreenTypes,
    val rootScreenType: ScreenTypes,
    val enableBottomBar: Boolean = true
) {

    protected open fun createPath(pattern: String): String = pattern

    val routePath get() = createPath(screenType.route)

    object Accounts: RootScreen(ScreenTypes.accounts, icon = CPSIcons.Account)

    data class AccountExpanded(val type: AccountManagers)
        : Screen(ScreenTypes.accountExpanded, rootScreenType = ScreenTypes.accounts) {
            override fun createPath(pattern: String) = pattern.replace("{manager}", type.name)
        }

    data class AccountSettings(val type: AccountManagers)
        : Screen(ScreenTypes.accountSettings, rootScreenType = ScreenTypes.accounts, enableBottomBar = false) {
            override fun createPath(pattern: String) = pattern.replace("{manager}", type.name)
        }

    object News: RootScreen(ScreenTypes.news, icon = CPSIcons.News)
    object NewsSettings: Screen(ScreenTypes.newsSettings, rootScreenType = ScreenTypes.news, enableBottomBar = false)
    object NewsFollowList: Screen(ScreenTypes.newsFollowList, rootScreenType = ScreenTypes.news)
    data class NewsCodeforcesBlog(val handle: String)
        : Screen(ScreenTypes.newsCodeforcesBlog, rootScreenType = ScreenTypes.news) {
            override fun createPath(pattern: String) = pattern.replace("{handle}", handle)
        }

    object Contests: RootScreen(ScreenTypes.contests, icon = CPSIcons.Contest)
    object ContestsSettings: Screen(ScreenTypes.contestsSettings, rootScreenType = ScreenTypes.contests, enableBottomBar = false)

    object Development: RootScreen(ScreenTypes.develop, icon = CPSIcons.Development)

}

sealed class RootScreen(
    screenType: ScreenTypes,
    val icon: ImageVector
): Screen(
    screenType = screenType,
    rootScreenType = screenType
)

private val simpleScreens = arrayOf(
    Screen.Contests,
    Screen.News,
    Screen.Accounts,
    Screen.ContestsSettings,
    Screen.NewsSettings,
    Screen.NewsFollowList,
    Screen.Development
)

fun NavBackStackEntry.getScreen(): Screen {
    val route = destination.route
    if (route == ScreenTypes.accountExpanded.route) {
        val type = AccountManagers.valueOf(arguments?.getString("manager")!!)
        return Screen.AccountExpanded(type)
    }
    if (route == ScreenTypes.accountSettings.route) {
        val type = AccountManagers.valueOf(arguments?.getString("manager")!!)
        return Screen.AccountSettings(type)
    }
    if (route == ScreenTypes.newsCodeforcesBlog.route) {
        val handle = arguments?.getString("handle")!!
        return Screen.NewsCodeforcesBlog(handle)
    }
    return simpleScreens.first { it.screenType.route == route }
}

