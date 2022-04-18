package com.demich.cps.contests

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.utils.context
import com.demich.cps.utils.isSortedWith
import com.demich.cps.utils.rememberCollect

@Composable
fun ContestsScreen(contestsViewModel: ContestsViewModel) {
    val contests by rememberCollect { contestsViewModel.flowOfContests() }

    val currentTime by collectCurrentTime()

    val contestsSorted by remember(contests) {
        val cached = contests.toMutableList()
        derivedStateOf {
            val comparator = Contest.getComparator(currentTime)
            if (!cached.isSortedWith(comparator)) cached.sortWith(comparator)
            cached
        }
    }

    ContestsList(
        contests = contestsSorted,
        currentTimeMillis = currentTime.toEpochMilliseconds()
    )
}

@Composable
private fun ContestsList(
    contests: List<Contest>,
    currentTimeMillis: Long,
    modifier: Modifier = Modifier
) {
    LazyColumnWithScrollBar(
        modifier = modifier.fillMaxSize()
    ) {
        items(contests, key = { it.compositeId }) { contest ->
            ContestItem(
                contest = contest,
                currentTimeMillis = currentTimeMillis,
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
    CPSIconButton(icon = Icons.Default.Add) {
        contestsViewModel.addRandomContest()
    }
    CPSIconButton(icon = Icons.Default.Settings) {
        navController.navigate(Screen.ContestsSettings.route)
    }
    CPSReloadingButton(loadingStatus = contestsViewModel.loadingStatus) {
        contestsViewModel.reload(context)
    }
}
