package com.demich.cps.contests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.contests.loaders.makeCombinedMessage
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.develop.settingsDev
import com.demich.cps.room.contestsListDao
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContestsScreen(
    contestsViewModel: ContestsViewModel,
    filterController: ContestsFilterController
) {
    Column(
        modifier = Modifier
            .consumedWindowInsets(PaddingValues(bottom = CPSDefaults.bottomBarHeight))
            .imePadding()
    ) {
        ContestsFilterTextField(
            filterController = filterController,
            modifier = Modifier
                .fillMaxWidth()
        )
        ContestsContent(
            contestsViewModel = contestsViewModel,
            filterController = filterController,
            modifier = Modifier.weight(1f, false)
        )
    }
}

@Composable
private fun ContestsContent(
    contestsViewModel: ContestsViewModel,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    val context = context

    val contestsState = rememberCollect {
        context.contestsListDao.flowOfContests()
            .combine(context.settingsContests.ignoredContests.flow) { list, ignoreList ->
                list.filter { contest -> contest.compositeId !in ignoreList }
            }
    }

    val errorsList by contestsViewModel.getErrorsListState()
    val loadingStatus by contestsViewModel.loadingStatusState

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { contestsViewModel.reloadEnabledPlatforms(context) },
        modifier = modifier
    ) {
        Column {
            LoadingError(
                errors = errorsList,
                modifier = Modifier
                    .fillMaxWidth()
            )
            ContestsList(
                contestsState = contestsState,
                filterController = filterController,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }

    LaunchedEffect(Unit) {
        contestsViewModel.syncEnabledAndLastReloaded(context)
    }
}

@Composable
private fun ContestsList(
    contestsState: State<List<Contest>>,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(contestsState, filterController) {
        snapshotFlow { contestsState.value.isEmpty() }
            .collect {
                filterController.available = !it
            }
    }

    ContestsListSorted(
        contestsState = contestsState,
        filterController = filterController,
        modifier = modifier
    )
}

@Composable
private fun ContestsListSorted(
    contestsState: State<List<Contest>>,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    val currentTime by collectCurrentTimeEachSecond()

    val sortedState = remember(contestsState.value) {
        mutableStateOf(contestsState.value)
    }.apply {
        value.let {
            val comparator = Contest.getComparator(currentTime)
            if (!it.isSortedWith(comparator)) value = it.sortedWith(comparator)
        }
    }

    CompositionLocalProvider(
        LocalCurrentTime provides currentTime,
        //TODO: remember lambda to not recreate it each second, ProvideTimeEachSeconds doesn't helps
        content = remember(sortedState, filterController, modifier) {
            {
                ContestsSortedList(
                    contestsSortedListState = sortedState,
                    filterController = filterController,
                    modifier = modifier
                )
            }
        }
    )

}

@Composable
private fun ContestsSortedList(
    contestsSortedListState: State<List<Contest>>,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    var expandedItems: Set<Pair<Contest.Platform, String>>
        by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(emptySet()) }

    val filteredContests = remember(
        key1 = contestsSortedListState.value,
        key2 = filterController.filter
    ) {
        filterController.filterContests(contestsSortedListState.value)
    }

    LazyColumnWithScrollBar(modifier = modifier) {
        itemsNotEmpty(
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
private fun ContestsFilterTextField(
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    //TODO: show in bottom, focus request
    if (filterController.enabled) {
        OutlinedTextField(
            modifier = modifier,
            singleLine = true,
            textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            value = filterController.filter,
            onValueChange = {
                filterController.filter = it
            },
            trailingIcon = {
                CPSIconButton(icon = CPSIcons.Close) {
                    filterController.enabled = false
                    filterController.filter = ""
                }
            },
            label = { Text("Search") }
        )
    }
}

@Composable
private fun ColumnScope.LoadingError(
    errors: List<Pair<ContestsLoaders,Throwable>>,
    modifier: Modifier = Modifier
) {
    val context = context
    val devModeEnabled by rememberCollect { context.settingsDev.devModeEnabled.flow }
    AnimatedVisibility(visible = errors.isNotEmpty()) {
        Text(
            text = makeCombinedMessage(
                errors = errors,
                developEnabled = devModeEnabled
            ),
            textAlign = TextAlign.Center,
            color = cpsColors.background,
            fontSize = 13.sp,
            modifier = modifier
                .background(color = cpsColors.error)
                .padding(all = 2.dp)
        )
    }
}

fun contestsMenuBuilder(
    navigator: CPSNavigator,
    contestsViewModel: ContestsViewModel
): CPSMenuBuilder = {
    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
        enabled = contestsViewModel.loadingStatusState.value != LoadingStatus.LOADING
    ) {
        navigator.navigateTo(Screen.ContestsSettings)
    }
}

fun contestsBottomBarBuilder(
    contestsViewModel: ContestsViewModel,
    filterController: ContestsFilterController
): AdditionalBottomBarBuilder = {
    val context = context

    if (filterController.available) {
        CPSIconButton(
            icon = CPSIcons.Search,
            onClick = { filterController.enabled = true }
        )
    }

    CPSReloadingButton(
        loadingStatus = contestsViewModel.loadingStatusState.value,
        onClick = { contestsViewModel.reloadEnabledPlatforms(context) }
    )
}
