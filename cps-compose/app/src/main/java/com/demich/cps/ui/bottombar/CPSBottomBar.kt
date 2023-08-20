package com.demich.cps.ui.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import com.demich.cps.navigation.RootScreen
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.ui.BottomBarLayoutId
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateColor
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.cps.utils.swallowInitialEvents
import kotlinx.coroutines.launch

typealias AdditionalBottomBarBuilder = @Composable RowScope.() -> Unit

@Composable
fun CPSBottomBar(
    navigator: CPSNavigator,
    additionalBottomBar: AdditionalBottomBarBuilder? = null,
    onSetSystemNavColor: (Color) -> Unit
) {
    if (navigator.isBottomBarEnabled) {
        var layoutSetupEnabled by rememberSaveable { mutableStateOf(false) }

        Box(modifier = Modifier.layoutId(BottomBarLayoutId)) {
            Scrim(
                show = layoutSetupEnabled,
                onDismiss = { layoutSetupEnabled = false },
                modifier = Modifier.fillMaxSize()
            )
            BottomBarContent(
                navigator = navigator,
                additionalBottomBar = additionalBottomBar,
                layoutSetupEnabled = layoutSetupEnabled,
                onEnableLayoutSetup = { layoutSetupEnabled = true },
                onDismissLayoutSetup = { layoutSetupEnabled = false },
                onSetSystemNavColor = onSetSystemNavColor,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun<T> switchAnimationSpec() = spring<T>(stiffness = Spring.StiffnessMediumLow)

@Composable
private fun BottomBarContent(
    navigator: CPSNavigator,
    additionalBottomBar: AdditionalBottomBarBuilder?,
    layoutSetupEnabled: Boolean,
    onEnableLayoutSetup: () -> Unit,
    onDismissLayoutSetup: () -> Unit,
    onSetSystemNavColor: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = animateColor(
        onColor = cpsColors.backgroundAdditional,
        offColor = cpsColors.backgroundNavigation,
        enabled = layoutSetupEnabled,
        animationSpec = switchAnimationSpec()
    ).also(onSetSystemNavColor)

    Column(modifier = modifier.pointerInput(Unit) {}) {
        AnimatedVisibility(
            visible = layoutSetupEnabled,
            exit = shrinkVertically(switchAnimationSpec()),
            enter = expandVertically(switchAnimationSpec())
        ) {
            BottomBarSettings(
                onDismissRequest = onDismissLayoutSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 8.dp)
            )
        }
        BottomBarBody(
            navigator = navigator,
            additionalBottomBar = additionalBottomBar,
            layoutSettingsEnabled = layoutSetupEnabled,
            onEnableLayoutSettings = onEnableLayoutSetup,
            modifier = Modifier
                .height(CPSDefaults.bottomBarHeight)
                .fillMaxWidth()
                .background(backgroundColor)
        )
    }
}

@Composable
private fun BottomBarBody(
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
            add(Screen.News)
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
