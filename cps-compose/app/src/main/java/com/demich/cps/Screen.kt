package com.demich.cps

import androidx.navigation.NavBackStackEntry
import com.demich.cps.accounts.managers.AccountManagers

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
    develop("develop")
}

val Screen.subtitle: String
    get() = when (this) {
        Screen.Accounts -> listOf("accounts")
        is Screen.AccountExpanded -> listOf("accounts", type.name)
        is Screen.AccountSettings -> listOf("accounts", type.name, "settings")
        Screen.Contests -> listOf("contests")
        Screen.ContestsSettings -> listOf("contests", "settings")
        Screen.Development -> listOf("develop")
        Screen.News -> listOf("news")
        Screen.NewsSettings -> listOf("news", "settings")
    }.joinToString(prefix = "::", separator = ".")

sealed class Screen(
    val screenType: ScreenTypes,
    val rootScreenType: ScreenTypes = screenType,
    val enableBottomBar: Boolean = true,
) {

    protected open fun createPath(pattern: String): String = pattern

    val routePath get() = createPath(screenType.route)

    object Accounts: Screen(ScreenTypes.accounts)

    data class AccountExpanded(val type: AccountManagers)
        : Screen(ScreenTypes.accountExpanded, rootScreenType = ScreenTypes.accounts) {
            override fun createPath(pattern: String) = pattern.replace("{manager}", type.name)
        }

    data class AccountSettings(val type: AccountManagers)
        : Screen(ScreenTypes.accountSettings, rootScreenType = ScreenTypes.accounts, enableBottomBar = false) {
        override fun createPath(pattern: String) = pattern.replace("{manager}", type.name)
    }

    object News: Screen(ScreenTypes.news)
    object NewsSettings: Screen(ScreenTypes.newsSettings, rootScreenType = ScreenTypes.news, enableBottomBar = false)

    object Contests: Screen(ScreenTypes.contests)
    object ContestsSettings: Screen(ScreenTypes.contestsSettings, rootScreenType = ScreenTypes.contests, enableBottomBar = false)

    object Development: Screen(ScreenTypes.develop)

}

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
    return listOf(
        Screen.Accounts,
        Screen.News,
        Screen.NewsSettings,
        Screen.Contests,
        Screen.ContestsSettings,
        Screen.Development
    ).first { it.screenType.route == route }
}


