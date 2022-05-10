package com.demich.cps

import androidx.navigation.NavBackStackEntry
import com.demich.cps.accounts.managers.AccountManagers


sealed class Screen(
    val routePattern: String,
    val enableBottomBar: Boolean = true,
    private val root: Screen? = null
) {
    val rootScreen: Screen get() = root ?: this
    open val subtitle: String get() = "::$routePattern"

    open val routePath: String get() = routePattern

    object Accounts: Screen("accounts")
    class AccountExpanded(val type: AccountManagers)
        : Screen(routePattern = routePattern, root = Accounts) {
        override val routePath: String
            get() = routePattern.replace("{manager}", type.name)
        override val subtitle get() = "::accounts.$type"
        override fun equals(other: Any?) = other is AccountExpanded && other.type == type
        override fun hashCode() = type.ordinal
        companion object {
            const val routePattern = "account/{manager}"
        }
    }
    class AccountSettings(val type: AccountManagers)
        : Screen(routePattern = routePattern, root = Accounts, enableBottomBar = false) {
        override val routePath: String
            get() = routePattern.replace("{manager}", type.name)
        override val subtitle get() = "::accounts.$type.settings"
        override fun equals(other: Any?) = other is AccountSettings && other.type == type
        override fun hashCode() = type.ordinal
        companion object {
            const val routePattern = "account_settings/{manager}"
        }
    }

    object News: Screen("news")
    object NewsSettings: Screen("news.settings", root = News, enableBottomBar = false)

    object Contests: Screen("contests")
    object ContestsSettings: Screen("contests.settings", root = Contests, enableBottomBar = false)

    object Development: Screen("develop")

}

fun NavBackStackEntry.getScreen(): Screen {
    val route = destination.route
    if (route == Screen.AccountExpanded.routePattern) {
        val type = AccountManagers.valueOf(arguments?.getString("manager")!!)
        return Screen.AccountExpanded(type)
    }
    if (route == Screen.AccountSettings.routePattern) {
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
    ).first { it.routePattern == route }
}


