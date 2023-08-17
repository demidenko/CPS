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
import com.demich.cps.navigation.RootScreen
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.ui.*
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
    val scope = rememberCoroutineScope()
    val context = context

    val devModeEnabled by rememberCollect { context.settingsUI.devModeEnabled.flow }
    val layoutType by rememberCollect { context.settingsUI.navigationLayoutType.flow }

    val rootScreens = remember(devModeEnabled) {
        buildList {
            add(Screen.Accounts)
            add(Screen.News)
            add(Screen.Contests)
            if (devModeEnabled) add(Screen.Development)
        }
    }

    CPSBottomNavigationMainItems(
        modifier = modifier.fillMaxSize(),
        rootScreens = rootScreens,
        selectedRootScreenType = navigator.currentScreen?.rootScreenType,
        layoutType = layoutType,
        onSelect = { screen ->
            if (screen !is Screen.Development) {
                scope.launch { context.settingsUI.startScreenRoute(screen.routePath) }
            }
            navigator.navigateTo(screen)
        },
        onLongPress = {
            //TODO: setup layout
        }
    )
}

enum class NavigationLayoutType {
    start,  //ABC....
    center, //..ABC..
    evenly  //.A.B.C. (tap area as weight(1f))
}

@Composable
private fun CPSBottomNavigationMainItems(
    modifier: Modifier = Modifier,
    rootScreens: List<RootScreen>,
    selectedRootScreenType: ScreenTypes?,
    layoutType: NavigationLayoutType,
    onSelect: (RootScreen) -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clipToBounds(),
        horizontalArrangement = when (layoutType) {
            NavigationLayoutType.start -> Arrangement.Start
            else -> Arrangement.Center
        }
    ) {
        for (screen in rootScreens) {
            CPSBottomNavigationItem(
                icon = screen.icon,
                isSelected = screen.screenType == selectedRootScreenType,
                onClick = {
                    if (screen.screenType != selectedRootScreenType) onSelect(screen)
                },
                onLongPress = onLongPress,
                modifier = if (layoutType == NavigationLayoutType.evenly) Modifier.weight(1f) else Modifier
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
    onClick: () -> Unit
) {
    val fraction by animateFloatAsState(targetValue = if (isSelected) 1f else 0f)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .fillMaxHeight()
            .combinedClickable(
                indication = rememberRipple(bounded = false, radius = 48.dp),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onLongClick = onLongPress?.withVibration()
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = lerp(start = cpsColors.content, stop = cpsColors.accent, fraction),
            modifier = Modifier
                .align(Alignment.Center)
                .size(lerp(start = 24.dp, stop = 28.dp, fraction = fraction))
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