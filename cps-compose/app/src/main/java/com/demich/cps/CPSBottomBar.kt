package com.demich.cps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

typealias AdditionalBottomBarBuilder = @Composable RowScope.() -> Unit

@Composable
fun CPSBottomBar(
    navController: NavController,
    currentScreen: Screen?,
    additionalBottomBar: AdditionalBottomBarBuilder? = null
) {
    if (currentScreen == null || currentScreen.enableBottomBar) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CPSBottomBarMain(
    currentScreen: Screen?,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = context
    val scope = rememberCoroutineScope()

    val devModeEnabled by rememberCollect { context.settingsDev.devModeEnabled.flow }

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

    var showChangeStartScreenDialogFor: Screen? by remember { mutableStateOf(null) }

    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        for ((screen, icon) in rootScreens) {
            CPSBottomNavigationItem(
                icon = icon,
                isSelected = screen == currentScreen?.rootScreen,
                onLongPress = {
                    showChangeStartScreenDialogFor = screen
                }
            ) {
                navController.navigate(screen.route) {
                    //TODO: wtf it works?
                    popUpTo(currentScreen!!.rootScreen.route) {
                        saveState = true
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    showChangeStartScreenDialogFor?.let { screen ->
        ChangeStartScreenDialog(
            screen = screen,
            onDismissRequest = { showChangeStartScreenDialogFor = null },
            onConfirmRequest = {
                scope.launch {
                    context.settingsUI.startScreenRoute(newValue = screen.route)
                    showChangeStartScreenDialogFor = null
                }
            }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.CPSBottomNavigationItem(
    icon: ImageVector,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onSelect: () -> Unit
) {
    val size by animateDpAsState(
        if (isSelected) 28.dp else 24.dp
    )

    val color by animateColorAsState(
        if (isSelected) cpsColors.colorAccent else cpsColors.textColor
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(size)
            .weight(1f)
            .combinedClickable(
                indication = rememberRipple(bounded = false, radius = 48.dp),
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (!isSelected) onSelect()
                },
                onLongClick = onLongPress.takeIf { isSelected }
            )
    )
}

@Composable
private fun ChangeStartScreenDialog(
    screen: Screen,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Set ${screen.route.replaceFirstChar { it.uppercaseChar() }} as start page?")
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("No")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmRequest) {
                Text("Yes")
            }
        }
    )
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