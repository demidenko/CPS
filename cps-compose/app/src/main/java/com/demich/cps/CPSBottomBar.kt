package com.demich.cps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.demich.cps.accounts.AccountsBottomBar
import com.demich.cps.contests.ContestsBottomBar
import com.demich.cps.news.NewsBottomBar
import com.demich.cps.ui.theme.cpsColors
import com.google.accompanist.systemuicontroller.SystemUiController

@Composable
fun CPSBottomBar(
    navController: NavHostController,
    currentBackStackEntry: NavBackStackEntry?,
    cpsViewModels: CPSViewModels,
    systemUiController: SystemUiController,
    devModeEnabled: Boolean
) {
    systemUiController.setNavigationBarColor(
        color = cpsColors.backgroundNavigation,
        darkIcons = MaterialTheme.colors.isLight
    )
    val currentScreen = currentBackStackEntry?.destination?.getScreen() ?: return
    if (currentScreen.enableBottomBar) {
        Row(
            modifier = Modifier
                .height(56.dp) //as BottomNavigationHeight
                .fillMaxWidth()
                .background(cpsColors.backgroundNavigation),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CPSBottomBarAdditional(
                currentScreen = currentScreen,
                navController = navController,
                cpsViewModels = cpsViewModels,
                modifier = Modifier.weight(1f)
            )
            CPSBottomBarVerticalDivider()
            CPSBottomBarMain(
                currentScreen = currentScreen,
                navController = navController,
                devModeEnabled = devModeEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CPSBottomBarMain(
    currentScreen: Screen,
    navController: NavHostController,
    devModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    BottomNavigation(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = cpsColors.backgroundNavigation,
        elevation = 0.dp
    ) {
        Screen.majorScreens.forEach { (screen, icon) ->
            if (screen == Screen.Development && !devModeEnabled) return@forEach
            val isSelected = screen == currentScreen.rootScreen
            BottomNavigationItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (isSelected) 28.dp else 24.dp)
                    )
                },
                onClick = {
                    if(!isSelected) {
                        navController.navigate(screen.route) {
                            popUpTo(currentScreen.route) {
                                saveState = true
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                selected = isSelected,
                selectedContentColor = cpsColors.colorAccent,
                unselectedContentColor = cpsColors.textColor
            )
        }
    }
}

@Composable
private fun CPSBottomBarAdditional(
    currentScreen: Screen,
    navController: NavHostController,
    cpsViewModels: CPSViewModels,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        when (currentScreen) {
            Screen.Accounts -> AccountsBottomBar(cpsViewModels.accountsViewModel)
            Screen.News -> NewsBottomBar()
            Screen.Contests -> ContestsBottomBar(navController)
            else -> Unit
        }
    }
}

@Composable
private fun CPSBottomBarVerticalDivider() = Box(
    Modifier
        .fillMaxHeight(0.6f)
        .width(1.dp)
        .background(cpsColors.dividerColor)
)