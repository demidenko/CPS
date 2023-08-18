package com.demich.cps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.demich.cps.navigation.RootScreen
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateColor
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.cps.utils.swallowInitialEvents
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.launch

typealias AdditionalBottomBarBuilder = @Composable RowScope.() -> Unit

private fun<T> switchAnimationSpec() = spring<T>(stiffness = Spring.StiffnessMediumLow)

@Composable
fun CPSBottomBar(
    navigator: CPSNavigator,
    additionalBottomBar: AdditionalBottomBarBuilder? = null,
    systemUiController: SystemUiController
) {
    if (navigator.isBottomBarEnabled) {
        var layoutSetupEnabled by rememberSaveable { mutableStateOf(false) }

        val backgroundColor = animateColor(
            onColor = cpsColors.backgroundAdditional,
            offColor = cpsColors.backgroundNavigation,
            enabled = layoutSetupEnabled,
            animationSpec = switchAnimationSpec()
        ).also {
            systemUiController.setNavigationBarColor(
                color = it,
                darkIcons = MaterialTheme.colors.isLight
            )
        }

        Column(
            modifier = Modifier
                .layoutId(BottomBarLayoutId)
                .pointerInput(Unit) {},
        ) {
            AnimatedVisibility(
                visible = layoutSetupEnabled,
                exit = shrinkVertically(switchAnimationSpec()),
                enter = expandVertically(switchAnimationSpec())
            ) {
                BottomBarSettings(
                    onDismissRequest = { layoutSetupEnabled = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                        .background(backgroundColor)
                        .padding(all = 8.dp)
                )
            }
            BottomBarBody(
                navigator = navigator,
                additionalBottomBar = additionalBottomBar,
                layoutSettingsEnabled = layoutSetupEnabled,
                onEnableLayoutSettings = { layoutSetupEnabled = true },
                modifier = Modifier
                    .height(CPSDefaults.bottomBarHeight)
                    .fillMaxWidth()
                    .background(backgroundColor)
            )
        }
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
        CPSBottomBarAdditional(
            modifier = Modifier.weight(1f),
            content = additionalBottomBar ?: {}
        )
        CPSBottomBarVerticalDivider()
        CPSBottomBarMain(
            modifier = Modifier.weight(1f),
            selectedRootScreenType = { navigator.currentScreen?.rootScreenType },
            onNavigateToScreen = navigator::navigateTo,
            layoutSettingsEnabled = layoutSettingsEnabled,
            onEnableLayoutSettings = onEnableLayoutSettings
        )
    }
}

@Composable
private fun CPSBottomBarMain(
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

    CPSBottomNavigationMainItems(
        modifier = modifier.fillMaxSize(),
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
    indication: Indication?,
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
                indication = indication,
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
    indication: Indication?,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val fraction by animateFloatAsState(targetValue = if (isSelected) 1f else 0f)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .fillMaxHeight()
            .combinedClickable(
                indication = indication,
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