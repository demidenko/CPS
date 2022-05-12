package com.demich.cps

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

typealias AdditionalBottomBarBuilder = @Composable RowScope.() -> Unit

@Composable
fun CPSBottomBar(
    navigator: CPSNavigator,
    additionalBottomBar: AdditionalBottomBarBuilder? = null
) {
    if (navigator.isBottomBarEnabled) {
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
                navigator = navigator,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CPSBottomBarMain(
    navigator: CPSNavigator,
    modifier: Modifier = Modifier
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

    CPSBottomNavigationMainItems(
        modifier = modifier.fillMaxSize(),
        rootScreens = rootScreens,
        selectedRootScreen = navigator.currentScreen?.rootScreenType,
        onSelect = { screen ->
            navigator.navigateTo(screen)
        },
        onLongPress = { screen ->
            showChangeStartScreenDialogFor = screen
        }
    )

    showChangeStartScreenDialogFor?.let { screen ->
        ChangeStartScreenDialog(
            screen = screen,
            onDismissRequest = { showChangeStartScreenDialogFor = null },
            onConfirmRequest = {
                scope.launch {
                    context.settingsUI.startScreenRoute(newValue = screen.screenType.route)
                    showChangeStartScreenDialogFor = null
                }
            }
        )
    }
}

@Composable
private fun CPSBottomNavigationMainItems(
    modifier: Modifier = Modifier,
    rootScreens: List<Pair<Screen, ImageVector>>,
    selectedRootScreen: ScreenTypes?,
    onSelect: (Screen) -> Unit,
    onLongPress: (Screen) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clipToBounds()
    ) {
        for ((screen, icon) in rootScreens) {
            CPSBottomNavigationItem(
                icon = icon,
                isSelected = screen.screenType == selectedRootScreen,
                onSelect = { onSelect(screen) },
                onLongPress = { onLongPress(screen) }
            )
        }
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
    val fraction by animateFloatAsState(targetValue = if (isSelected) 1f else 0f)

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = lerp(start = cpsColors.content, stop = cpsColors.accent, fraction),
        modifier = Modifier
            .size(lerp(start = 24.dp, stop = 28.dp, fraction))
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
            //TODO: show icon; consider radiobuttons;
            Text(text = "Set ${screen.screenType.route.replaceFirstChar { it.uppercaseChar() }} as start page?")
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
        .background(cpsColors.divider)
)