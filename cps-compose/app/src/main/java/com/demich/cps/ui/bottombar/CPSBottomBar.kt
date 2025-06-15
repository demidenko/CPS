package com.demich.cps.ui.bottombar

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.demich.cps.navigation.Screen
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.StartScreenDataStore
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.switchAnimationSpec
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.background
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.ignoreInputEvents
import kotlinx.coroutines.launch

typealias AdditionalBottomBarBuilder = @Composable RowScope.() -> Unit

@Composable
fun CPSBottomBar(
    selectedRootScreen: () -> Screen.RootScreen?,
    onNavigateToScreen: (Screen.RootScreen) -> Unit,
    additionalContent: () -> AdditionalBottomBarBuilder,
    settingsEnabled: Boolean,
    onEnableSettings: () -> Unit,
    onDisableSettings: () -> Unit,
    backgroundColor: () -> Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        androidx.compose.animation.AnimatedVisibility(
            visible = settingsEnabled,
            exit = shrinkVertically(switchAnimationSpec()),
            enter = expandVertically(switchAnimationSpec()),
            modifier = Modifier
                .placeAboveParent()
                .fillMaxWidth()
        ) {
            BottomBarSettings(
                onCloseRequest = onDisableSettings,
                modifier = Modifier
                    .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                    .background(backgroundColor)
                    .pointerInput(Unit) {} //for not send to scrim
                    .padding(horizontal = 8.dp)
            )
        }

        BottomBarRow(
            selectedRootScreen = selectedRootScreen,
            onNavigateToScreen = onNavigateToScreen,
            additionalContent = additionalContent,
            settingsEnabled = settingsEnabled,
            onEnableSettings = onEnableSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(CPSDefaults.bottomBarHeight)
        )
    }
}

@Composable
private fun BottomBarRow(
    selectedRootScreen: () -> Screen.RootScreen?,
    onNavigateToScreen: (Screen.RootScreen) -> Unit,
    additionalContent: () -> AdditionalBottomBarBuilder,
    settingsEnabled: Boolean,
    onEnableSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.ignoreInputEvents(enabled = settingsEnabled)
    ) {
        BottomBarBodyAdditional(
            modifier = Modifier.weight(1f),
            content = additionalContent()
        )
        BottomBarVerticalDivider()
        BottomBarBodyMain(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            selectedRootScreen = selectedRootScreen,
            onNavigateToScreen = onNavigateToScreen,
            settingsEnabled = settingsEnabled,
            onEnableSettings = onEnableSettings
        )
    }
}

@Composable
private fun BottomBarBodyMain(
    selectedRootScreen: () -> Screen.RootScreen?,
    onNavigateToScreen: (Screen.RootScreen) -> Unit,
    settingsEnabled: Boolean,
    onEnableSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = context

    val devModeEnabled by collectItemAsState { context.settingsUI.devModeEnabled }
    val layoutType by collectItemAsState { context.settingsUI.navigationLayoutType }

    val rootScreens = remember(devModeEnabled) {
        buildList {
            add(Screen.Profiles)
            add(Screen.Community)
            add(Screen.Contests)
            if (devModeEnabled) add(Screen.Development)
        }
    }

    BottomBarNavigationItems(
        modifier = modifier,
        rootScreens = rootScreens,
        selectedRootScreen = if (settingsEnabled) null else selectedRootScreen(),
        indication = if (settingsEnabled) null else ripple(bounded = false, radius = 48.dp),
        layoutType = layoutType,
        onSelect = { screen ->
            scope.launch { StartScreenDataStore(context).startRootScreen(screen) }
            onNavigateToScreen(screen)
        },
        onLongPress = onEnableSettings
    )
}

private val Screen.RootScreen.bottomBarIcon: ImageVector
    get() = when (this) {
        Screen.Profiles -> CPSIcons.Profile
        Screen.Community -> CPSIcons.Community
        Screen.Contests -> CPSIcons.Contest
        Screen.Development -> CPSIcons.Development
    }

@Composable
private fun BottomBarNavigationItems(
    modifier: Modifier = Modifier,
    rootScreens: List<Screen.RootScreen>,
    selectedRootScreen: Screen.RootScreen?,
    indication: Indication?,
    layoutType: NavigationLayoutType,
    onSelect: (Screen.RootScreen) -> Unit,
    onLongPress: () -> Unit
) {
    BottomBarNavigationItems(
        modifier = modifier.clipToBounds(),
        navigationLayoutType = layoutType
    ) {
        rootScreens.forEach { screen ->
            CPSBottomNavigationItem(
                icon = screen.bottomBarIcon,
                isSelected = screen == selectedRootScreen,
                onClick = {
                    if (screen != selectedRootScreen) onSelect(screen)
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

private fun Modifier.placeAboveParent() = layout { measurable, constrains ->
    val placeable = measurable.measure(constrains)
    layout(width = placeable.width, height = 0) {
        placeable.placeRelative(x = 0, y = -placeable.height)
    }
}