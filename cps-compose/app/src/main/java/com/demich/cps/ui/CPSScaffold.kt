package com.demich.cps.ui

import androidx.compose.material.Surface
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import com.demich.cps.ui.theme.cpsColors

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

            val bottomBarPlaceables = subcompose(ScaffoldLayoutContent.BottomBar, bottomBar).map {
                it.measure(looseConstraints)
            }
            val bottomBarHeight = bottomBarPlaceables.maxHeight()

            val bodyContentHeight = layoutHeight - topBarHeight - bottomBarHeight
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
                it.place(x = 0, y = layoutHeight - bottomBarHeight - progressBarsHeight)
            }

            topBarPlaceables.forEach {
                it.place(x = 0, y = 0)
            }

            bottomBarPlaceables.forEach {
                it.place(x = 0, y = layoutHeight - bottomBarHeight)
            }
        }
    }
}

private enum class ScaffoldLayoutContent { TopBar, MainContent, BottomBar, ProgressBars }

private fun List<Placeable>.maxHeight(): Int =
    maxOfOrNull { it.height } ?: 0