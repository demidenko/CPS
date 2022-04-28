package com.demich.cps.contests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.CPSMenuBuilder
import com.demich.cps.Screen
import com.demich.cps.room.contestsListDao
import com.demich.cps.settingsDev
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.ktor.client.features.*
import io.ktor.http.*
import kotlinx.coroutines.flow.combine
import java.net.UnknownHostException

@Composable
fun ContestsScreen(
    contestsViewModel: ContestsViewModel,
    searchEnabledState: MutableState<Boolean>
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    Column {
        if (searchEnabledState.value) {
            //TODO: show in bottom, imePadding, focus request
            ContestsSearchField(
                modifier = Modifier.fillMaxWidth(),
                value = searchText,
                onValueChange = {
                    searchText = it
                },
                onClose = {
                    searchEnabledState.value = false
                    searchText = ""
                }
            )
        }
        ContestsScreen(
            contestsViewModel = contestsViewModel,
            searchText = searchText
        )
    }
}

@Composable
private fun ContestsScreen(
    contestsViewModel: ContestsViewModel,
    searchText: String
) {
    val context = context

    val contests by rememberCollect {
        context.contestsListDao.flowOfContests()
            .combine(context.settingsContests.ignoredContests.flow) { list, ignoreList ->
                list.filter { contest -> contest.compositeId !in ignoreList }
            }
    }

    val error by contestsViewModel.flowOfError().collectAsState()

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = contestsViewModel.loadingStatus == LoadingStatus.LOADING),
        onRefresh = { contestsViewModel.reloadEnabledPlatforms(context) }
    ) {
        Column {
            LoadingError(
                error = error,
                modifier = Modifier
                    .fillMaxWidth()
            )
            ContestsList(
                contests = contests,
                searchText = searchText,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}


@Composable
private fun ContestsList(
    contests: List<Contest>,
    searchText: String,
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
            contestsSortedListState = sortedState,
            searchText = searchText,
            modifier = modifier
        )
    }

}

@Composable
private fun ContestsSortedList(
    contestsSortedListState: State<List<Contest>>,
    searchText: String,
    modifier: Modifier = Modifier
) {
    var expandedItems: Set<Pair<Contest.Platform, String>>
        by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(emptySet()) }

    val filteredContests = remember(contestsSortedListState.value, searchText) {
        contestsSortedListState.value.filter { contest ->
            val inTitle = contest.title.contains(searchText, ignoreCase = true)
            val inPlatformName = contest.platform.name.contains(searchText, ignoreCase = true)
            inTitle || inPlatformName
        }
    }

    LazyColumnWithScrollBar(modifier = modifier) {
        items(
            items = filteredContests,
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
private fun ContestsSearchField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        trailingIcon = {
            CPSIconButton(icon = CPSIcons.Close) {
                onClose()
            }
        },
        label = { Text("Search") }
    )
}

@Composable
private fun ColumnScope.LoadingError(
    error: Throwable?,
    modifier: Modifier = Modifier
) {
    val context = context
    val devModeEnabled by rememberCollect { context.settingsDev.devModeEnabled.flow }
    val message = when {
        error == null -> ""
        error is UnknownHostException
            -> "Connection failed"
        error is ClientRequestException && error.response.status == HttpStatusCode.Unauthorized
            -> "Incorrect clist::api access"
        error is ClientRequestException && error.response.status == HttpStatusCode.TooManyRequests
            -> "Too many requests"
        else -> {
            if(devModeEnabled) "$error" else ""
        }
    }
    AnimatedVisibility(visible = message.isNotBlank()) {
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

fun contestsMenuBuilder(
    navController: NavController
): CPSMenuBuilder = {
    CPSDropdownMenuItem(title = "Settings", icon = CPSIcons.Settings) {
        navController.navigate(Screen.ContestsSettings.route)
    }
}

fun contestsBottomBarBuilder(
    contestsViewModel: ContestsViewModel,
    onEnableSearch: () -> Unit
): AdditionalBottomBarBuilder = {
    val context = context
    CPSIconButton(icon = CPSIcons.Search) {
        onEnableSearch()
    }
    CPSReloadingButton(loadingStatus = contestsViewModel.loadingStatus) {
        contestsViewModel.reloadEnabledPlatforms(context)
    }
}
