@file:OptIn(ExperimentalPagerApi::class)

package com.demich.cps.news

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.theme.cpsColors
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import kotlin.math.max
import kotlin.math.min

@Composable
fun NewsTabRow(
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

@Composable
fun NewsTab(
    title: String,
    index: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedTextColor: Color,
    unselectedTextColor: Color
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = title,
            color = tabColor(
                index = index,
                selectedIndex = pagerState.currentPage,
                selectedOffset = pagerState.currentPageOffset,
                selectedTextColor = selectedTextColor,
                unselectedTextColor = unselectedTextColor
            ),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.075.em,
            modifier = Modifier.align(Alignment.Center)
        )
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