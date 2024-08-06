package com.demich.cps.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.ui.bottombar.BottomBarSettings
import com.demich.cps.ui.bottombar.CPSBottomBar
import com.demich.cps.ui.bottombar.Scrim
import com.demich.cps.ui.bottomprogressbar.CPSBottomProgressBarsColumn
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateColor
import com.demich.cps.utils.background


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

        val bottomBarBackgroundColor = animateColor(
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
                Column(modifier = Modifier.fillMaxSize()) {
                    StatusBarBox(navigator = navigator)
                    navigator.TopBar()
                    content()
                }
                CPSBottomProgressBarsColumn()
                Scrim(
                    show = bottomBarSettingsEnabled,
                    onDismiss = { bottomBarSettingsEnabled = false },
                    animationSpec = switchAnimationSpec(),
                    modifier = Modifier.fillMaxSize()
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = bottomBarSettingsEnabled,
                    exit = shrinkVertically(switchAnimationSpec()),
                    enter = expandVertically(switchAnimationSpec())
                ) {
                    BottomBarSettings(
                        onDismissRequest = { bottomBarSettingsEnabled = false },
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
                    onEnableLayoutSettings = { bottomBarSettingsEnabled = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background { bottomBarBackgroundColor }
                        .navigationBarsPadding()
                        .height(CPSDefaults.bottomBarHeight)
                )
            }
        }
    }
}

//based on Material::Scaffold source code
@Composable
fun CPSScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    progressBars: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = cpsColors.background
    ) {
        ScaffoldLayout(
            topBar = topBar,
            bottomBar = bottomBar,
            progressBars = progressBars,
            content = content
        )
    }
}

@Composable
private fun ScaffoldLayout(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    progressBars: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        layout(width = layoutWidth, height = layoutHeight) {
            val topBarPlaceables = subcompose(ScaffoldLayoutContent.TopBar, topBar).map {
                it.measure(looseConstraints)
            }
            val topBarHeight = topBarPlaceables.maxHeight()

            val bottomBarPlaceable = subcompose(ScaffoldLayoutContent.BottomBar, bottomBar)
                .find { it.layoutId == BottomBarLayoutId }
                ?.measure(looseConstraints)

            val bottomBarBodyHeight =
                if (bottomBarPlaceable == null) 0
                else CPSDefaults.bottomBarHeight.roundToPx()

            val bodyContentHeight = layoutHeight - topBarHeight - bottomBarBodyHeight
            val bodyContentPlaceables = subcompose(ScaffoldLayoutContent.MainContent, content).map {
                it.measure(looseConstraints.copy(maxHeight = bodyContentHeight))
            }

            val progressBarsPlaceables = subcompose(ScaffoldLayoutContent.ProgressBars, progressBars).map {
                it.measure(looseConstraints.copy(maxHeight = bodyContentHeight))
            }
            val progressBarsHeight = progressBarsPlaceables.maxHeight()

            bodyContentPlaceables.forEach {
                it.place(x = 0, y = topBarHeight)
            }

            progressBarsPlaceables.forEach {
                it.place(x = 0, y = layoutHeight - bottomBarBodyHeight - progressBarsHeight)
            }

            topBarPlaceables.forEach {
                it.place(x = 0, y = 0)
            }

            bottomBarPlaceable?.apply {
                place(x = 0, y = layoutHeight - height)
            }
        }
    }
}

data object BottomBarLayoutId

private enum class ScaffoldLayoutContent { TopBar, MainContent, BottomBar, ProgressBars }

private fun List<Placeable>.maxHeight(): Int =
    maxOfOrNull { it.height } ?: 0