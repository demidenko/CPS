package com.demich.cps.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.RootScreen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.bottombar.BottomBarSettings
import com.demich.cps.ui.bottombar.CPSBottomBar
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateColorAsState
import com.demich.cps.utils.background

@Stable
private fun<T> switchAnimationSpec() = spring<T>(stiffness = Spring.StiffnessMediumLow)

@Composable
fun CPSScaffold(
    modifier: Modifier = Modifier,
    navigator: CPSNavigator,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = cpsColors.background
    ) {
        var bottomBarSettingsEnabled by rememberSaveable { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            navigator = navigator,
            bottomBarSettingsEnabled = bottomBarSettingsEnabled,
            onDisableBottomBarSettings = { bottomBarSettingsEnabled = false },
            onEnableBottomBarSettings = { bottomBarSettingsEnabled = true }.withVibration(),
            content = content
        )
    }
}

@Composable
private fun Scaffold(
    modifier: Modifier = Modifier,
    navigator: CPSNavigator,
    bottomBarSettingsEnabled: Boolean,
    onDisableBottomBarSettings: () -> Unit,
    onEnableBottomBarSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier,
        selectedRootScreenType = { navigator.currentScreen?.rootScreenType },
        onNavigateToScreen = navigator::navigateTo,
        bottomBarEnabled = navigator.isBottomBarEnabled,
        bottomBarSettingsEnabled = bottomBarSettingsEnabled,
        onDisableBottomBarSettings = onDisableBottomBarSettings,
        onEnableBottomBarSettings = onEnableBottomBarSettings,
        topBars = { navigator.TopBarWithStatusBar(modifier = Modifier.fillMaxWidth()) },
        content = content,
        additionalBottomBar = navigator.additionalBottomBar,
    )
}

@Composable
private fun backgroundColorState(enabled: Boolean) =
    animateColorAsState(
        enabledColor = cpsColors.backgroundAdditional,
        disabledColor = cpsColors.backgroundNavigation,
        enabled = enabled,
        animationSpec = switchAnimationSpec()
    )

@Composable
private fun Scaffold(
    modifier: Modifier = Modifier,
    selectedRootScreenType: () -> ScreenTypes?,
    onNavigateToScreen: (RootScreen) -> Unit,
    bottomBarEnabled: Boolean,
    bottomBarSettingsEnabled: Boolean,
    onDisableBottomBarSettings: () -> Unit,
    onEnableBottomBarSettings: () -> Unit,
    topBars: @Composable () -> Unit,
    content: @Composable () -> Unit,
    additionalBottomBar: AdditionalBottomBarBuilder?
) {
    val bottomBarBackgroundColor by backgroundColorState(bottomBarSettingsEnabled)

    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ScaffoldContent(
                topBars = topBars,
                content = content,
                modifier = Modifier.fillMaxSize()
            )
            Scrim(
                show = bottomBarSettingsEnabled,
                onDismiss = onDisableBottomBarSettings,
                animationSpec = switchAnimationSpec(),
                modifier = Modifier.fillMaxSize()
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = bottomBarSettingsEnabled,
                exit = shrinkVertically(switchAnimationSpec()),
                enter = expandVertically(switchAnimationSpec())
            ) {
                BottomBarSettings(
                    onCloseRequest = onDisableBottomBarSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                        .background { bottomBarBackgroundColor }
                        .pointerInput(Unit) {} //for not send to scrim
                        .padding(horizontal = 8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background {
                    if (bottomBarEnabled) bottomBarBackgroundColor
                    else Color.Unspecified
                }
                .navigationBarsPadding()
        ) {
            if (bottomBarEnabled) {
                CPSBottomBar(
                    selectedRootScreenType = selectedRootScreenType,
                    onNavigateToScreen = onNavigateToScreen,
                    additionalBottomBar = additionalBottomBar,
                    layoutSettingsEnabled = bottomBarSettingsEnabled,
                    onEnableLayoutSettings = onEnableBottomBarSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CPSDefaults.bottomBarHeight)
                )
            }
        }
    }
}

@Composable
private inline fun ScaffoldContent(
    modifier: Modifier = Modifier,
    topBars: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBars()
            content()
        }
        CPSBottomProgressBarsColumn(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun Scrim(
    show: Boolean,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float>,
    onDismiss: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (show) 0.32f else 0f,
        animationSpec = animationSpec,
        label = "scrim_alpha"
    )
    if (alpha > 0f) {
        Canvas(modifier = modifier.let {
            if (!show) it
            else it.pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
        }) {
            drawRect(color = Color.Black.copy(alpha = alpha))
        }
    }
}
