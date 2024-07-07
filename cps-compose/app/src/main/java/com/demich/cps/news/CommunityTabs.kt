package com.demich.cps.news

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSCountBadge
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.AnimatedVisibleByNotNull
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommunityTabRow(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabs: @Composable () -> Unit
) {
    TabRow(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp),
        selectedTabIndex = pagerState.currentPage,
        backgroundColor = cpsColors.background,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.pagerTabIndicatorOffset(
                    pagerState = pagerState,
                    tabPositions = tabPositions
                ),
                color = cpsColors.accent
            )
        },
        divider = { },
        tabs = tabs
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommunityTab(
    title: String,
    index: Int,
    badgeCount: () -> Int?,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedTextColor: Color,
    unselectedTextColor: Color
) {
    Box(modifier = modifier.fillMaxSize()) {
        BadgedBox(
            modifier = Modifier.align(Alignment.Center),
            badge = {
                AnimatedVisibleByNotNull(
                    value = badgeCount,
                    enter = scaleIn(),
                    exit = scaleOut(),
                    content = { CPSCountBadge(count = it) }
                )
            }
        ) {
            Text(
                text = title,
                color = tabColor(
                    index = index,
                    selectedIndex = pagerState.currentPage,
                    selectedOffset = pagerState.currentPageOffsetFraction,
                    selectedTextColor = selectedTextColor,
                    unselectedTextColor = unselectedTextColor
                ),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.075.em,
            )
        }
    }
}

private fun tabColor(
    index: Int,
    selectedIndex: Int,
    selectedOffset: Float,
    selectedTextColor: Color,
    unselectedTextColor: Color
): Color {
    val l: Float = selectedIndex + selectedOffset
    val r: Float = l + 1
    val i = index.toFloat()
    if (i <= r && l <= i + 1) return lerp(
        start = unselectedTextColor,
        stop = selectedTextColor,
        fraction = min(r, i+1) - max(l, i)
    )
    return unselectedTextColor
}

//copy from accompanist
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pagerTabIndicatorOffset(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
    pageIndexMapping: (Int) -> Int = { it },
): Modifier = layout { measurable, constraints ->
    if (tabPositions.isEmpty()) {
        // If there are no pages, nothing to show
        layout(constraints.maxWidth, 0) {}
    } else {
        val currentPage = minOf(tabPositions.lastIndex, pageIndexMapping(pagerState.currentPage))
        val currentTab = tabPositions[currentPage]
        val previousTab = tabPositions.getOrNull(currentPage - 1)
        val nextTab = tabPositions.getOrNull(currentPage + 1)
        val fraction = pagerState.currentPageOffsetFraction
        val indicatorWidth = if (fraction > 0 && nextTab != null) {
            androidx.compose.ui.unit.lerp(currentTab.width, nextTab.width, fraction).roundToPx()
        } else if (fraction < 0 && previousTab != null) {
            androidx.compose.ui.unit.lerp(currentTab.width, previousTab.width, -fraction).roundToPx()
        } else {
            currentTab.width.roundToPx()
        }
        val indicatorOffset = if (fraction > 0 && nextTab != null) {
            androidx.compose.ui.unit.lerp(currentTab.left, nextTab.left, fraction).roundToPx()
        } else if (fraction < 0 && previousTab != null) {
            androidx.compose.ui.unit.lerp(currentTab.left, previousTab.left, -fraction).roundToPx()
        } else {
            currentTab.left.roundToPx()
        }
        val placeable = measurable.measure(
            Constraints(
                minWidth = indicatorWidth,
                maxWidth = indicatorWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
        )
        layout(constraints.maxWidth, maxOf(placeable.height, constraints.minHeight)) {
            placeable.placeRelative(
                indicatorOffset,
                maxOf(constraints.minHeight - placeable.height, 0)
            )
        }
    }
}