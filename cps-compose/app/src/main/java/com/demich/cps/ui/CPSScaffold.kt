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
            navigator = navigator,
            bottomBarSettingsEnabled = bottomBarSettingsEnabled,
            onDisableBottomBarSettings = { bottomBarSettingsEnabled = false },
            onEnableBottomBarSettings = { bottomBarSettingsEnabled = true }.withVibration(),
            content = content,
            modifier = Modifier.fillMaxSize()
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
    val bottomBarBackgroundColor by animateColorAsState(
        enabledColor = cpsColors.backgroundAdditional,
        disabledColor = cpsColors.backgroundNavigation,
        enabled = bottomBarSettingsEnabled,
        animationSpec = switchAnimationSpec()
    )

    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            ScaffoldContent(
                statusBar = { StatusBarBox(navigator = navigator) },
                topBar = { navigator.TopBar() },
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
        if (navigator.isBottomBarEnabled) {
            CPSBottomBar(
                navigator = navigator,
                additionalBottomBar = navigator.additionalBottomBar,
                layoutSettingsEnabled = bottomBarSettingsEnabled,
                onEnableLayoutSettings = onEnableBottomBarSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .background { bottomBarBackgroundColor }
                    .navigationBarsPadding()
                    .height(CPSDefaults.bottomBarHeight)
            )
        }
    }
}

@Composable
private fun ScaffoldContent(
    modifier: Modifier = Modifier,
    statusBar: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            statusBar()
            topBar()
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
