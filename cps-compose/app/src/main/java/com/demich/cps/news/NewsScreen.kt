package com.demich.cps.news

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.contests.Contest
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus

@Composable
fun NewsScreen(navigator: CPSNavigator) {
    TabsHeader(
        navigator = navigator,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TabsHeader(
    navigator: CPSNavigator,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf(CodeforcesTitle.MAIN) }

    LaunchedEffect(key1 = selected) {
        navigator.setSubtitle("news", "codeforces", selected.name)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = platformPainter(platform = Contest.Platform.codeforces),
            contentDescription = null,
            tint = cpsColors.textColor,
            modifier = Modifier.padding(start = 8.dp, end = 6.dp)
        )
        NewsTabRow(
            modifier = Modifier.height(48.dp),
            selectedIndex = selected.ordinal,
        ) {
            CodeforcesTitle.values().forEach { title ->
                Tab(
                    selected = title == selected,
                    onClick = { selected = title },
                    selectedContentColor = cpsColors.textColor,
                    unselectedContentColor = cpsColors.textColorAdditional
                ) {
                    Text(
                        text = title.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.075.em
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsTabRow(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    tabs: @Composable () -> Unit
) {
    TabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = selectedIndex,
        backgroundColor = cpsColors.background,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = cpsColors.colorAccent
            )
        },
        divider = { },
        tabs = tabs
    )
}

@Composable
fun NewsSettingsScreen() {

}

fun newsBottomBarBuilder(

): AdditionalBottomBarBuilder = {
    CPSReloadingButton(loadingStatus = LoadingStatus.PENDING) {

    }
}

fun newsMenuBuilder(
    navigator: CPSNavigator
): CPSMenuBuilder = {
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings) {
        navigator.navigateTo(Screen.NewsSettings)
    }
    //CPSDropdownMenuItem(title = "Follow List", icon = CPSIcons.Accounts) { }
}