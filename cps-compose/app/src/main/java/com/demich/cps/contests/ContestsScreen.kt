package com.demich.cps.contests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.contests.loaders.makeCombinedMessage
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.CodeforcesMonitorWidget
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.develop.settingsDev
import com.demich.cps.room.contestsListDao
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.workers.ContestsWorker
import com.demich.datastore_itemized.add
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContestsScreen(
    contestsViewModel: ContestsViewModel,
    filterController: ContestsFilterController,
    loadingStatusState: State<LoadingStatus>
) {
    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()

    if (isAnyPlatformEnabled) {
        Column(
            modifier = Modifier
                .consumedWindowInsets(PaddingValues(bottom = CPSDefaults.bottomBarHeight))
                .imePadding()
        ) {
            CodeforcesMonitor(
                modifier = Modifier.padding(horizontal = 3.dp).fillMaxWidth()
            )
            ContestsContent(
                contestsViewModel = contestsViewModel,
                filterController = filterController,
                loadingStatusState = loadingStatusState,
                modifier = Modifier.weight(1f, false)
            )
            ContestsFilterTextField(
                filterController = filterController,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        NoneEnabledMessage(modifier = Modifier.fillMaxSize())
    }

    val context = context
    LaunchedEffect(Unit) {
        contestsViewModel.syncEnabledAndLastReloaded(context)
    }
}

@Composable
private fun ContestsContent(
    contestsViewModel: ContestsViewModel,
    filterController: ContestsFilterController,
    loadingStatusState: State<LoadingStatus>,
    modifier: Modifier = Modifier
) {
    val context = context

    val contestsToShowState = rememberCollect {
        context.contestsListDao.flowOfContests()
            .combine(context.settingsContests.ignoredContests.flow) { list, ignored ->
                if (ignored.isEmpty()) list
                else list.filter { contest -> contest.compositeId !in ignored }
            }
    }

    LaunchedEffect(contestsToShowState, filterController) {
        snapshotFlow { contestsToShowState.value.isEmpty() }
            .collect {
                filterController.available = !it
            }
    }

    val errorsList by rememberCollect { contestsViewModel.flowOfLoadingErrors() }
    val loadingStatus by loadingStatusState

    CPSSwipeRefresh(
        isRefreshing = loadingStatus == LoadingStatus.LOADING,
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
                contestsState = contestsToShowState,
                filterController = filterController,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun ContestsList(
    contestsState: State<List<Contest>>,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    ProvideTimeEachSecond {
        val sortedState = rememberWith(contestsState.value) {
            mutableStateOf(this)
        }.apply {
            value.let {
                val comparator = Contest.getComparator(LocalCurrentTime.current)
                if (!it.isSortedWith(comparator)) value = it.sortedWith(comparator)
            }
        }

        //TODO: without remember ContestsSortedList called each seconds (no skip!)
        remember<@Composable () -> Unit>(sortedState, filterController, modifier) {
            {
                ContestsSortedList(
                    contestsSortedListState = sortedState,
                    filterController = filterController,
                    modifier = modifier
                )
            }
        }()
    }
}

@Composable
private fun ContestsSortedList(
    contestsSortedListState: State<List<Contest>>,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    var expandedItems: List<Pair<Contest.Platform, String>>
        by rememberSaveable(stateSaver = jsonCPS.saver()) { mutableStateOf(emptyList()) }

    val filteredContests by remember(contestsSortedListState, filterController) {
        derivedStateOf {
            filterController.filterContests(contestsSortedListState.value)
        }
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
    if (filterController.enabled) {
        val focusRequester = rememberFocusOnCreationRequester(true)
        OutlinedTextField(
            modifier = modifier.focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            value = filterController.filter,
            onValueChange = {
                filterController.filter = it
            },
            label = { Text("filter") },
            leadingIcon = {
                Icon(
                    imageVector = CPSIcons.Search,
                    tint = cpsColors.content,
                    contentDescription = null
                )
            },
            trailingIcon = {
                CPSIconButton(icon = CPSIcons.Close) {
                    filterController.enabled = false
                }
            }
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
    loadingStatusState: State<LoadingStatus>
): CPSMenuBuilder = {
    val loadingStatus by loadingStatusState
    
    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
        enabled = loadingStatus != LoadingStatus.LOADING
    ) {
        navigator.navigateTo(Screen.ContestsSettings)
    }
}

fun contestsBottomBarBuilder(
    loadingStatusState: State<LoadingStatus>,
    filterController: ContestsFilterController,
    onReloadClick: () -> Unit
): AdditionalBottomBarBuilder = {
    val loadingStatus by loadingStatusState
    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()

    if (isAnyPlatformEnabled && filterController.available && !filterController.enabled) {
        CPSIconButton(
            icon = CPSIcons.Search,
            onClick = { filterController.enabled = true }
        )
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus,
        enabled = isAnyPlatformEnabled,
        onClick = onReloadClick
    )
}

@Composable
private fun rememberIsAnyPlatformEnabled(): State<Boolean> {
    val context = context
    return rememberCollect {
        val settings = context.settingsContests
        combine(
            flow = settings.enabledPlatforms.flow.map { it.any { it != Contest.Platform.unknown } },
            flow2 = settings.clistAdditionalResources.flow.map { it.isNotEmpty() }
        ) { any1, any2 -> any1 || any2 }
    }
}


@Composable
fun rememberCombinedLoadingStatusState(contestsViewModel: ContestsViewModel): State<LoadingStatus> {
    val context = context

    return produceState(
        initialValue = LoadingStatus.PENDING,
        key1 = contestsViewModel
    ) {
        contestsViewModel.flowOfLoadingStatus()
            .combine(
                ContestsWorker.getWork(context).flowOfInfo()
            ) { loadingStatus, workInfo ->
                if (workInfo?.state == WorkInfo.State.RUNNING) LoadingStatus.LOADING
                else loadingStatus
            }.collect {
                value = it
            }
    }

    /*val loadingStatus by rememberCollect { contestsViewModel.flowOfLoadingStatus() }
    val workInfo by remember { ContestsWorker.getWork(context) }.workInfoState()
    return remember {
        derivedStateOf {
            if (workInfo?.state == WorkInfo.State.RUNNING) LoadingStatus.LOADING
            else loadingStatus
        }
    }*/
}

@Composable
private fun NoneEnabledMessage(modifier: Modifier = Modifier) {
    EmptyMessageBox(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Platforms are not selected")
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("goto ")
                Icon(imageVector = CPSIcons.More, contentDescription = null)
                Icon(imageVector = CPSIcons.ArrowRight, contentDescription = null)
                Icon(imageVector = CPSIcons.Settings, contentDescription = null)
            }
        }
    }
}


@Composable
private fun CodeforcesMonitor(modifier: Modifier = Modifier) {
    val context = context
    val scope = rememberCoroutineScope()
    val monitor = remember { CodeforcesMonitorDataStore(context) }

    val contestIdState = rememberCollect { monitor.contestId.flow }

    contestIdState.value?.let { contestId ->
        CodeforcesMonitorWidget(
            modifier = modifier,
            onOpenInBrowser = {
                context.openUrlInBrowser(url = CodeforcesApi.urls.contest(contestId))
            },
            onStop = {
                scope.launch {
                    monitor.reset()
                    CodeforcesAccountManager(context).getSettings().monitorCanceledContests.add(contestId to getCurrentTime())
                }
            }
        )
    }
}