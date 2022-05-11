package com.demich.cps.news.codeforces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.Contest
import com.demich.cps.news.NewsTabRow
import com.demich.cps.ui.CPSNavigator
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.theme.cpsColors
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun CodeforcesNewsScreen(
    navigator: CPSNavigator
) {


    val tabs = remember {
        mutableStateOf(
            listOf(
                CodeforcesTitle.MAIN,
                CodeforcesTitle.TOP,
                CodeforcesTitle.RECENT,
                CodeforcesTitle.LOST
            )
        )
    }

    val pagerState = rememberPagerState()

    val selectedTab: CodeforcesTitle by remember {
        derivedStateOf {
            tabs.value[pagerState.currentPage]
        }
    }

    LaunchedEffect(key1 = selectedTab) {
        navigator.setSubtitle("news", "codeforces", selectedTab.name)
    }

    Column(Modifier.fillMaxSize()) {
        TabsHeader(
            tabs = tabs,
            pagerState = pagerState,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalPager(
            count = tabs.value.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "$index. ${tabs.value[index]}",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }


}


@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TabsHeader(
    tabs: State<List<CodeforcesTitle>>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedTextColor: Color = cpsColors.textColor,
    unselectedTextColor: Color = cpsColors.textColorAdditional,
) {
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = platformIconPainter(platform = Contest.Platform.codeforces),
            contentDescription = null,
            tint = cpsColors.textColor,
            modifier = Modifier.padding(start = 8.dp, end = 6.dp)
        )
        NewsTabRow(pagerState = pagerState) {
            tabs.value.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier.fillMaxSize()
                        .clickable {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                ) {
                    Text(
                        text = title.name,
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