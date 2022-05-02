package com.demich.cps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

typealias AdditionalBottomBarBuilder = @Composable RowScope.() -> Unit

@Composable
fun CPSBottomBar(
    navController: NavHostController,
    currentScreen: Screen?,
    additionalBottomBar: AdditionalBottomBarBuilder? = null
) {
    if (currentScreen != null && currentScreen.enableBottomBar) {
        Row(
            modifier = Modifier
                .height(56.dp) //as BottomNavigationHeight
                .fillMaxWidth()
                .background(cpsColors.backgroundNavigation),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CPSBottomBarAdditional(
                modifier = Modifier.weight(1f),
                content = additionalBottomBar ?: {}
            )
            CPSBottomBarVerticalDivider()
            CPSBottomBarMain(
                currentScreen = currentScreen,
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
    val rootScreens = remember(devModeEnabled) {
        buildList {
            add(Screen.Accounts to CPSIcons.Account)
            add(Screen.News to CPSIcons.News)
            add(Screen.Contests to CPSIcons.Contest)
            if (devModeEnabled) {
                add(Screen.Development to CPSIcons.Development)
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
    modifier: Modifier = Modifier,
    content: AdditionalBottomBarBuilder
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        content = content
    )
}

@Composable
private fun CPSBottomBarVerticalDivider() = Box(
    Modifier
        .fillMaxHeight(0.6f)
        .width(1.dp)
        .background(cpsColors.dividerColor)
)