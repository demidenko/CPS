package com.demich.cps.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.bottombar.CPSBottomBar
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateToggleColorAsState
import com.demich.cps.utils.backgroundColor
import com.demich.cps.utils.ifThen

@Stable
internal fun <T> switchAnimationSpec() = spring<T>(stiffness = Spring.StiffnessMediumLow)

@Composable
fun CPSScaffold(
    modifier: Modifier = Modifier,
    navigator: CPSNavigator,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = cpsColors.background,
        contentColor = cpsColors.content
    ) {
        NavBarShelf(
            modifier = Modifier.fillMaxSize(),
            selectedRootScreen = { navigator.currentScreen?.rootScreen },
            onNavigateToScreen = navigator::navigateTo,
            bottomBarEnabled = navigator::isBottomBarEnabled,
            content = {
                TopBarsAndContent(
                    topBars = { navigator.TopBarWithStatusBar(modifier = Modifier.fillMaxWidth()) },
                    content = content,
                    modifier = Modifier.fillMaxSize()
                )
            },
            additionalBottomBar = { navigator.additionalBottomBar }
        )
    }
}


@Composable
private fun NavBarShelf(
    modifier: Modifier = Modifier,
    selectedRootScreen: () -> Screen.RootScreen?,
    onNavigateToScreen: (Screen.RootScreen) -> Unit,
    bottomBarEnabled: () -> Boolean,
    content: @Composable () -> Unit,
    additionalBottomBar: () -> AdditionalBottomBarBuilder
) {
    var settingsEnabled by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()

            Scrim(
                show = settingsEnabled,
                onDismiss = { settingsEnabled = false },
                animationSpec = switchAnimationSpec(),
                modifier = Modifier.fillMaxSize()
            )
        }

        BottomBarAndNavBar(
            selectedRootScreen = selectedRootScreen,
            onNavigateToScreen = onNavigateToScreen,
            bottomBarEnabled = bottomBarEnabled,
            bottomBarSettingsEnabled = settingsEnabled,
            onDisableBottomBarSettings = { settingsEnabled = false },
            onEnableBottomBarSettings = { settingsEnabled = true }.withVibration(),
            additionalBottomBar = additionalBottomBar,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun bottomBarBackgroundColorState(enabled: Boolean) =
    animateToggleColorAsState(
        enabledColor = cpsColors.backgroundAdditional,
        disabledColor = cpsColors.backgroundNavigation,
        isEnabled = enabled,
        animationSpec = switchAnimationSpec()
    )


//TODO: merge bottomBarEnabled and bottomBarSettingsEnabled to sealed?
@Composable
private fun BottomBarAndNavBar(
    modifier: Modifier = Modifier,
    selectedRootScreen: () -> Screen.RootScreen?,
    onNavigateToScreen: (Screen.RootScreen) -> Unit,
    bottomBarEnabled: () -> Boolean,
    bottomBarSettingsEnabled: Boolean,
    onDisableBottomBarSettings: () -> Unit,
    onEnableBottomBarSettings: () -> Unit,
    additionalBottomBar: () -> AdditionalBottomBarBuilder
) {
    val backgroundColor by bottomBarBackgroundColorState(bottomBarSettingsEnabled)
    val enabled = bottomBarEnabled()
    Box(
        modifier = modifier
            .backgroundColor {
                if (enabled) backgroundColor
                else Color.Unspecified
            }
            .navigationBarsPadding()
    ) {
        if (enabled) {
            CPSBottomBar(
                selectedRootScreen = selectedRootScreen,
                onNavigateToScreen = onNavigateToScreen,
                additionalContent = additionalBottomBar,
                settingsEnabled = bottomBarSettingsEnabled,
                onEnableSettings = onEnableBottomBarSettings,
                onDisableSettings = onDisableBottomBarSettings,
                backgroundColor = { backgroundColor },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private inline fun TopBarsAndContent(
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
        targetValue = if (show) 0.6f else 0f,
        animationSpec = animationSpec,
        label = "scrim_alpha"
    )

    val visible by remember { derivedStateOf { alpha > 0 } }

    if (visible) {
        Box(
            modifier = modifier.ifThen(show) {
                pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
            }.backgroundColor { Color.Black.copy(alpha = alpha) }
        )
    }
}
