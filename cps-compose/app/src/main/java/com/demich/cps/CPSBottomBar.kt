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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.demich.cps.ui.*
import com.demich.cps.ui.dialogs.CPSDialog
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
                .height(CPSDefaults.bottomBarHeight)
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

@Composable
private fun CPSBottomBarMain(
    navigator: CPSNavigator,
    modifier: Modifier = Modifier
) {
    val context = context

    val devModeEnabled by rememberCollect { context.settingsDev.devModeEnabled.flow }

    val rootScreens = remember(devModeEnabled) {
        buildList {
            add(Screen.Accounts)
            add(Screen.News)
            add(Screen.Contests)
            if (devModeEnabled) add(Screen.Development)
        }
    }

    var showChangeStartScreenDialog by remember { mutableStateOf(false) }

    CPSBottomNavigationMainItems(
        modifier = modifier.fillMaxSize(),
        rootScreens = rootScreens,
        selectedRootScreenType = navigator.currentScreen?.rootScreenType,
        onSelect = { screen ->
            navigator.navigateTo(screen)
        },
        onLongPress = { showChangeStartScreenDialog = true }
    )

    if (showChangeStartScreenDialog) {
        ChangeStartScreenDialog(
            onDismissRequest = { showChangeStartScreenDialog = false }
        )
    }
}

@Composable
private fun CPSBottomNavigationMainItems(
    modifier: Modifier = Modifier,
    rootScreens: List<Screen>,
    selectedRootScreenType: ScreenTypes?,
    onSelect: (Screen) -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clipToBounds()
    ) {
        for (screen in rootScreens) {
            CPSBottomNavigationItem(
                icon = screen.icon!!,
                isSelected = screen.screenType == selectedRootScreenType,
                onSelect = { onSelect(screen) },
                onLongPress = onLongPress,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CPSBottomNavigationItem(
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onSelect: () -> Unit
) {
    val fraction by animateFloatAsState(targetValue = if (isSelected) 1f else 0f)

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = lerp(start = cpsColors.content, stop = cpsColors.accent, fraction),
        modifier = modifier
            .size(lerp(start = 24.dp, stop = 28.dp, fraction = fraction))
            .combinedClickable(
                indication = rememberRipple(bounded = false, radius = 48.dp),
                interactionSource = remember { MutableInteractionSource() },
                onClick = { if (!isSelected) onSelect() },
                onLongClick = onLongPress
            )
    )
}

@Composable
private fun ChangeStartScreenDialog(
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    val startScreenRoute by rememberCollect { context.settingsUI.startScreenRoute.flow }

    CPSDialog(onDismissRequest = onDismissRequest) {
        Text(text = "Select start screen", fontWeight = FontWeight.Medium)
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            listOf(
                Screen.Accounts,
                Screen.News,
                Screen.Contests
            ).forEach { screen ->
                val selected = screen.routePath == startScreenRoute
                CPSRadioButtonTitled(
                    title = {
                        //TODO: show icon somehow
                        Text(text = screen.screenType.route.replaceFirstChar { it.uppercaseChar() })
                    },
                    selected = selected,
                    onClick = {
                        scope.launch {
                            context.settingsUI.startScreenRoute(newValue = screen.routePath)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        TextButton(
            onClick = onDismissRequest,
            content = { Text(text = "Close") },
            modifier = Modifier.align(Alignment.End)
        )
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
private fun CPSBottomBarVerticalDivider() {
    Box(
        Modifier
            .fillMaxHeight(0.6f)
            .width(1.dp)
            .background(cpsColors.divider)
    )
}