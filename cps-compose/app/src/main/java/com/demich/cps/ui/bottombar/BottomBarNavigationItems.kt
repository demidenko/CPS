package com.demich.cps.ui.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.demich.cps.ui.bottombar.NavigationLayoutType.center
import com.demich.cps.ui.bottombar.NavigationLayoutType.evenly


enum class NavigationLayoutType {
    start,  //ABC....
    center, //..ABC..
    evenly  //.A.B.C. (tap area as weight(1f))
}

@Composable
internal fun BottomBarNavigationItems(
    modifier: Modifier = Modifier,
    navigationLayoutType: NavigationLayoutType,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.mapIndexed { index, item ->
            val minWidth = when (navigationLayoutType) {
                evenly -> {
                    val w = constraints.maxWidth / measurables.size
                    if (index < constraints.maxWidth % measurables.size) w + 1 else w
                }
                else -> 0
            }
            item.measure(constraints.copy(minWidth = minWidth))
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var x = when (navigationLayoutType) {
                center ->
                    (constraints.maxWidth - placeables.sumOf { it.width }) / 2
                else -> 0
            }
            placeables.forEach {
                it.place(x = x, y = 0)
                x += it.width
            }
        }
    }
}
