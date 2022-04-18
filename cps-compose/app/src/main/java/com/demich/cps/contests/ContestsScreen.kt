package com.demich.cps.contests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun ContestsScreen(
    navController: NavController,
    contestsViewModel: ContestsViewModel
) {
    val contests by rememberCollect {
        contestsViewModel.flowOfContests().map { contests ->
            contests.sortedBy { it.startTime }
        }.distinctUntilChanged()
    }

    LazyColumnWithScrollBar(
        modifier = Modifier.fillMaxSize()
    ) {
        items(contests, key = { it.compositeId }) { contest ->
            ContestItem(
                contest = contest,
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 7.dp,
                    top = 4.dp,
                    bottom = 3.dp
                )
            )
            Divider()
        }
    }
}

fun contestsBottomBarBuilder(
    navController: NavController,
    contestsViewModel: ContestsViewModel
): AdditionalBottomBarBuilder = {
    val context = context
    CPSIconButton(icon = Icons.Default.Settings) {
        navController.navigate(Screen.ContestsSettings.route)
    }
    CPSReloadingButton(loadingStatus = contestsViewModel.loadingStatus) {
        contestsViewModel.reload(context)
    }
}
