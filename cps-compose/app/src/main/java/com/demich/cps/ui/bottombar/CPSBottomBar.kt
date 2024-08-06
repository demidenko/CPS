package com.demich.cps.ui.bottombar

import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.demich.cps.navigation.RootScreen
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.cps.utils.swallowInitialEvents
import kotlinx.coroutines.launch

typealias AdditionalBottomBarBuilder = @Composable RowScope.() -> Unit

@Composable
fun CPSBottomBar(
    navigator: CPSNavigator,
    additionalBottomBar: AdditionalBottomBarBuilder?,
    layoutSettingsEnabled: Boolean,
    onEnableLayoutSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.swallowInitialEvents(enabled = layoutSettingsEnabled)
    ) {
        BottomBarBodyAdditional(
            modifier = Modifier.weight(1f),
            content = additionalBottomBar ?: {}
        )
        BottomBarVerticalDivider()
        BottomBarBodyMain(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            selectedRootScreenType = { navigator.currentScreen?.rootScreenType },
            onNavigateToScreen = navigator::navigateTo,
            layoutSettingsEnabled = layoutSettingsEnabled,
            onEnableLayoutSettings = onEnableLayoutSettings
        )
    }
}

@Composable
private fun BottomBarBodyMain(
    selectedRootScreenType: () -> ScreenTypes?,
    onNavigateToScreen: (RootScreen) -> Unit,
    layoutSettingsEnabled: Boolean,
    onEnableLayoutSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = context

    val devModeEnabled by rememberCollect { context.settingsUI.devModeEnabled.flow }
    val layoutType by rememberCollect { context.settingsUI.navigationLayoutType.flow }

    val rootScreens = remember(devModeEnabled) {
        buildList {
            add(Screen.Accounts)
            add(Screen.Community)
            add(Screen.Contests)
            if (devModeEnabled) add(Screen.Development)
        }
    }

    BottomNavigationMainItems(
        modifier = modifier,
        rootScreens = rootScreens,
        selectedRootScreenType = if (layoutSettingsEnabled) null else selectedRootScreenType(),
        indication = if (layoutSettingsEnabled) null else rememberRipple(bounded = false, radius = 48.dp),
        layoutType = layoutType,
        onSelect = { screen ->
            scope.launch { context.settingsUI.startScreenRoute(screen.routePath) }
            onNavigateToScreen(screen)
        },
        onLongPress = onEnableLayoutSettings
    )
}

@Composable
private fun BottomNavigationMainItems(
    modifier: Modifier = Modifier,
    rootScreens: List<RootScreen>,
    selectedRootScreenType: ScreenTypes?,
    indication: Indication?,
    layoutType: NavigationLayoutType,
    onSelect: (RootScreen) -> Unit,
    onLongPress: () -> Unit
) {
    MainNavItemsRow(
        modifier = modifier.clipToBounds(),
        navigationLayoutType = layoutType
    ) {
        rootScreens.forEach { screen ->
            CPSBottomNavigationItem(
                icon = screen.icon,
                isSelected = screen.screenType == selectedRootScreenType,
                onClick = {
                    if (screen.screenType != selectedRootScreenType) onSelect(screen)
                },
                onLongPress = onLongPress,
                indication = indication,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
private fun BottomBarBodyAdditional(
    modifier: Modifier = Modifier,
    content: AdditionalBottomBarBuilder
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        content = content
    )
}

@Composable
private fun BottomBarVerticalDivider() {
    Box(
        Modifier
            .fillMaxHeight(0.6f)
            .width(1.dp)
            .background(cpsColors.divider)
    )
}