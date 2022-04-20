package com.demich.cps.contests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.settingsDev
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.ktor.client.features.*
import io.ktor.http.*

@Composable
fun ContestsScreen(contestsViewModel: ContestsViewModel) {
    val context = context

    val contests by contestsViewModel.flowOfContests().collectAsState()
    val error by contestsViewModel.flowOfError().collectAsState()

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = contestsViewModel.loadingStatus == LoadingStatus.LOADING),
        onRefresh = { contestsViewModel.reload(context) }
    ) {
        Column {
            LoadingError(
                error = error,
                modifier = Modifier
                    .fillMaxWidth()
            )
            ContestsList(
                contests = contests,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}


@Composable
private fun ContestsList(
    contests: List<Contest>,
    modifier: Modifier = Modifier
) {
    val currentTime by collectCurrentTime()

    val sortedState = remember(contests) {
        mutableStateOf(contests)
    }.apply {
        value.let {
            val comparator = Contest.getComparator(currentTime)
            if (!it.isSortedWith(comparator)) value = it.sortedWith(comparator)
        }
    }

    CompositionLocalProvider(LocalCurrentTimeEachSecond provides currentTime) {
        ContestsSortedList(
            contestsSortedState = sortedState,
            modifier = modifier
        )
    }

}

@Composable
private fun ContestsSortedList(
    contestsSortedState: State<List<Contest>>,
    modifier: Modifier = Modifier
) {
    var expandedItems: Set<Pair<Contest.Platform?, String>>
        by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(emptySet()) }
    LazyColumnWithScrollBar(modifier = modifier) {
        items(
            items = contestsSortedState.value,
            /*key = { it.compositeId }*/ //TODO: key effects jumping on reorder
        ) { contest ->
            ContestItem(
                contest = contest,
                expanded = contest.compositeId in expandedItems,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val id = contest.compositeId
                        if (id in expandedItems) expandedItems -= id
                        else expandedItems += id
                    }
                    .padding(
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

@Composable
private fun LoadingError(
    error: Throwable?,
    modifier: Modifier = Modifier
) {
    val context = context
    val devModeEnabled by rememberCollect { context.settingsDev.devModeEnabled.flow }
    val message = when {
        error == null -> null
        error is ClientRequestException && error.response.status == HttpStatusCode.Unauthorized
            -> "Incorrect clist::api access"
        error is ClientRequestException && error.response.status == HttpStatusCode.TooManyRequests
            -> "Too many requests, try later"
        else -> {
            if (devModeEnabled) "Unknown error: ${error.message}"
            else null
        }
    }
    if (message != null) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = cpsColors.background,
            fontSize = 13.sp,
            modifier = modifier
                .background(color = cpsColors.errorColor)
                .padding(all = 2.dp)
        )
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
