package com.demich.cps.contests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.loaders.makeCombinedMessage
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.CodeforcesMonitorWidget
import com.demich.cps.contests.monitors.flowOfContestData
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.develop.settingsDev
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.workers.ContestsWorker
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.datastore_itemized.add
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.flowBy
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContestsScreen(
    contestsViewModel: ContestsViewModel,
    filterController: ContestsFilterController,
    isReloading: () -> Boolean,
    onReload: () -> Unit
) {
    val context = context
    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()
    val errorsMessage by rememberCollect {
        combine(
            flow = contestsViewModel.flowOfLoadingErrors(),
            flow2 = context.settingsDev.devModeEnabled.flow,
            transform = ::makeCombinedMessage
        )
    }

    Column(
        modifier = Modifier
            .consumeWindowInsets(PaddingValues(bottom = CPSDefaults.bottomBarHeight))
            .imePadding()
    ) {
        CodeforcesMonitor(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .fillMaxWidth()
        )
        if (isAnyPlatformEnabled) {
            ContestsReloadableContent(
                filterController = filterController,
                isReloading = isReloading,
                onReload = onReload,
                errorsMessage = { errorsMessage },
                modifier = Modifier.weight(1f, false)
            )
            ContestsFilterTextField(
                filterController = filterController,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            NoneEnabledMessage(modifier = Modifier.fillMaxSize())
        }
    }

    LaunchedEffect(Unit) {
        contestsViewModel.syncEnabledAndLastReloaded(context)
    }
}

@Composable
private fun ContestsReloadableContent(
    filterController: ContestsFilterController,
    isReloading: () -> Boolean,
    onReload: () -> Unit,
    errorsMessage: () -> String,
    modifier: Modifier = Modifier
) {
    CPSSwipeRefreshBox(
        isRefreshing = isReloading,
        onRefresh = onReload,
        modifier = modifier
    ) {
        ContestsContent(
            filterController = filterController,
            errorsMessage = errorsMessage
        )
    }
}


@Composable
private fun ContestsContent(
    filterController: ContestsFilterController,
    errorsMessage: () -> String
) {
    val context = context
    val contestsToShowState = rememberCollectWithLifecycle {
        context.contestsListDao.flowOfContests()
            .distinctUntilChanged()
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

    Column {
        LoadingError(
            errorsMessage = errorsMessage,
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
    val context = context
    val scope = rememberCoroutineScope()

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
                onDeleteRequest = {
                    scope.launch {
                        context.settingsContests.ignoredContests.edit {
                            put(contest.compositeId, getCurrentTime())
                        }
                    }
                },
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
    errorsMessage: () -> String,
    modifier: Modifier = Modifier
) {
    val message = errorsMessage()
    AnimatedVisibility(visible = message.isNotBlank()) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = cpsColors.background,
            fontSize = 13.sp,
            modifier = modifier
                .background(color = cpsColors.error)
                .padding(all = 2.dp)
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}

fun contestsMenuBuilder(
    onOpenSettings: () -> Unit,
    isReloading: () -> Boolean
): CPSMenuBuilder = {
    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
        enabled = !isReloading(),
        onClick = onOpenSettings
    )
}

fun contestsBottomBarBuilder(
    filterController: ContestsFilterController,
    loadingStatus: () -> LoadingStatus,
    onReloadClick: () -> Unit
): AdditionalBottomBarBuilder = {
    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()

    if (isAnyPlatformEnabled && filterController.available && !filterController.enabled) {
        CPSIconButton(
            icon = CPSIcons.Search,
            onClick = { filterController.enabled = true }
        )
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus(),
        enabled = isAnyPlatformEnabled,
        onClick = onReloadClick
    )
}

@Composable
private fun rememberIsAnyPlatformEnabled(): State<Boolean> {
    val context = context
    return rememberCollect {
        context.settingsContests.flowBy { prefs ->
            val any1 = prefs[enabledPlatforms].any { it != Contest.Platform.unknown }
            val any2 = prefs[clistAdditionalResources].isNotEmpty()
            any1 || any2
        }
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
                ContestsWorker.getWork(context).workInfoLiveData().asFlow()
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

    val contestDataState = rememberCollectWithLifecycle { monitor.flowOfContestData() }

    contestDataState.value?.let { contestData ->
        val requestFailed by rememberCollectWithLifecycle { monitor.lastRequest.flow.map { it == false } }
        CodeforcesMonitorWidget(
            contestData = contestData,
            requestFailed = requestFailed,
            modifier = modifier,
            onOpenInBrowser = {
                context.openUrlInBrowser(url = CodeforcesApi.urls.contest(contestData.contestId))
            },
            onStop = {
                scope.launch {
                    monitor.reset()
                    CodeforcesAccountManager(context).getSettings().monitorCanceledContests.add(
                        contestData.contestId to getCurrentTime()
                    )
                }
            }
        )
    }
}