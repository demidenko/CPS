package com.demich.cps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllOut
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.demich.cps.accounts.AccountsBottomBar
import com.demich.cps.contests.ContestsBottomBar
import com.demich.cps.news.NewsBottomBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController

@Composable
fun CPSBottomBar(
    navController: NavHostController,
    currentScreen: Screen?,
    cpsViewModels: CPSViewModels,
    systemUiController: SystemUiController
) {
    systemUiController.setNavigationBarColor(
        color = cpsColors.backgroundNavigation,
        darkIcons = MaterialTheme.colors.isLight
    )

    currentScreen?.takeIf { it.enableBottomBar }?.let { screen ->
        Row(
            modifier = Modifier
                .height(56.dp) //as BottomNavigationHeight
                .fillMaxWidth()
                .background(cpsColors.backgroundNavigation),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CPSBottomBarAdditional(
                currentScreen = screen,
                navController = navController,
                cpsViewModels = cpsViewModels,
                modifier = Modifier.weight(1f)
            )
            CPSBottomBarVerticalDivider()
            CPSBottomBarMain(
                currentScreen = screen,
                navController = navController,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CPSBottomBarMain(
    currentScreen: Screen,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val devModeEnabled by with(context) { rememberCollect { settingsDev.devModeEnabled.flow } }
    val rootScreens by remember {
        derivedStateOf {
            buildList {
                add(Screen.Accounts to Icons.Rounded.Person)
                add(Screen.News to Icons.Default.Subtitles)
                add(Screen.Contests to Icons.Filled.EmojiEvents)
                if (devModeEnabled) {
                    add(Screen.Development to Icons.Default.AllOut)
                }
            }
        }
    }
    BottomNavigation(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = cpsColors.backgroundNavigation,
        elevation = 0.dp
    ) {
        for ((screen, icon) in rootScreens) {
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
                            //TODO: wtf it works?
                            popUpTo(currentScreen.rootScreen.route) {
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