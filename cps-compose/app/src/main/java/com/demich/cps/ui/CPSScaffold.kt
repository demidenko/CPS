package com.demich.cps.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.RootScreen
import com.demich.cps.navigation.ScreenTypes
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.bottombar.CPSBottomBar
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateColorAsState
import com.demich.cps.utils.background
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
        bottomBarEnabled = { navigator.isBottomBarEnabled },
        bottomBarSettingsEnabled = bottomBarSettingsEnabled,
        onDisableBottomBarSettings = onDisableBottomBarSettings,
        onEnableBottomBarSettings = onEnableBottomBarSettings,
        topBars = { navigator.TopBarWithStatusBar(modifier = Modifier.fillMaxWidth()) },
        content = content,
        additionalBottomBar = { navigator.additionalBottomBar ?: {} }
    )
}

@Composable
private fun Scaffold(
    modifier: Modifier = Modifier,
    selectedRootScreenType: () -> ScreenTypes?,
    onNavigateToScreen: (RootScreen) -> Unit,
    bottomBarEnabled: () -> Boolean,
    bottomBarSettingsEnabled: Boolean,
    onDisableBottomBarSettings: () -> Unit,
    onEnableBottomBarSettings: () -> Unit,
    topBars: @Composable () -> Unit,
    content: @Composable () -> Unit,
    additionalBottomBar: () -> AdditionalBottomBarBuilder
) {
    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            TopBarsAndContent(
                topBars = topBars,
                content = content,
                modifier = Modifier.fillMaxSize()
            )

            //TODO: move to cpsBottomBar as header??
            Scrim(
                show = bottomBarSettingsEnabled,
                onDismiss = onDisableBottomBarSettings,
                animationSpec = switchAnimationSpec(),
                modifier = Modifier.fillMaxSize()
            )
        }

        BottomBarAndNavBar(
            selectedRootScreenType = selectedRootScreenType,
            onNavigateToScreen = onNavigateToScreen,
            bottomBarEnabled = bottomBarEnabled,
            bottomBarSettingsEnabled = bottomBarSettingsEnabled,
            onDisableBottomBarSettings = onDisableBottomBarSettings,
            onEnableBottomBarSettings = onEnableBottomBarSettings,
            additionalBottomBar = additionalBottomBar,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun bottomBarBackgroundColorState(enabled: Boolean) =
    animateColorAsState(
        enabledColor = cpsColors.backgroundAdditional,
        disabledColor = cpsColors.backgroundNavigation,
        enabled = enabled,
        animationSpec = switchAnimationSpec()
    )


//TODO: merge bottomBarEnabled and bottomBarSettingsEnabled to sealed?
@Composable
private fun BottomBarAndNavBar(
    modifier: Modifier = Modifier,
    selectedRootScreenType: () -> ScreenTypes?,
    onNavigateToScreen: (RootScreen) -> Unit,
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
            .background {
                if (enabled) backgroundColor
                else Color.Unspecified
            }
            .navigationBarsPadding()
    ) {
        if (enabled) {
            CPSBottomBar(
                selectedRootScreenType = selectedRootScreenType,
                onNavigateToScreen = onNavigateToScreen,
                additionalBottomBar = additionalBottomBar,
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
        targetValue = if (show) 0.32f else 0f,
        animationSpec = animationSpec,
        label = "scrim_alpha"
    )
    if (alpha > 0f) {
        Canvas(
            modifier = modifier.ifThen(show) {
                pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
            }
        ) {
            drawRect(color = Color.Black.copy(alpha = alpha))
        }
    }
}
