package com.demich.cps.contests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.list_items.ContestItem
import com.demich.cps.contests.loading.makeCombinedMessage
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.CodeforcesMonitorWidget
import com.demich.cps.contests.monitors.flowOfContestData
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesContestPhase
import com.demich.cps.ui.*
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.workers.ContestsWorker
import com.demich.datastore_itemized.add
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContestsScreen(
    contestsListController: ContestsListController,
    filterController: ContestsFilterController,
    isReloading: () -> Boolean,
    onReload: () -> Unit
) {
    val context = context
    val contestsViewModel = contestsViewModel()

    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()

    Column(
        modifier = Modifier
            .consumeWindowInsets(PaddingValues(bottom = CPSDefaults.bottomBarHeight))
            .imePadding()
    ) {
        CodeforcesMonitor(modifier = Modifier.fillMaxWidth())
        if (isAnyPlatformEnabled) {
            ContestsReloadableContent(
                contestsListController = contestsListController,
                filterController = filterController,
                isReloading = isReloading,
                onReload = onReload,
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
    contestsListController: ContestsListController,
    filterController: ContestsFilterController,
    isReloading: () -> Boolean,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    CPSSwipeRefreshBox(
        isRefreshing = isReloading,
        onRefresh = onReload,
        modifier = modifier
    ) {
        ContestsContent(
            contestsListController = contestsListController,
            filterController = filterController
        )
    }
}

@Composable
private fun ContestsContent(
    contestsListController: ContestsListController,
    filterController: ContestsFilterController
) {
    val context = context

    val contestsViewModel = contestsViewModel()
    val errorsMessage by rememberCollect {
        combine(
            flow = contestsViewModel.flowOfLoadingErrors(),
            flow2 = context.settingsUI.devModeEnabled.flow,
            transform = ::makeCombinedMessage
        )
    }

    Column {
        LoadingError(
            errorsMessage = { errorsMessage },
            modifier = Modifier
                .fillMaxWidth()
        )
        ContestsPager(
            contestsListController = contestsListController,
            filterController = filterController,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Composable
private fun ContestsPager(
    contestsListController: ContestsListController,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    val (
        contestsState: State<SortedContests>,
        currentTimeState: State<Instant>
    ) = produceSortedContestsWithTime()

    LaunchedEffect(contestsState, filterController, contestsListController) {
        snapshotFlow { contestsState.value.contests }
            .collect { contests ->
                filterController.available = contests.isNotEmpty()
                contestsListController.applyContests(contests)
            }
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    ProvideCurrentTime(currentTimeState) {
        val showFinished = contestsListController.showFinished
        saveableStateHolder.SaveableStateProvider(key = showFinished) {
            ContestsPage(
                contests = { contestsState.value.sublist(showFinished) },
                contestsListController = contestsListController,
                filterController = filterController,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ContestsPage(
    contests: () -> List<Contest>,
    contestsListController: ContestsListController,
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    val context = context
    val scope = rememberCoroutineScope()

    val filtered by remember(contests, filterController) {
        derivedStateOf {
            filterController.filterContests(contests())
        }
    }

    ContestsColumn(
        contests = { filtered },
        contestsListController = contestsListController,
        modifier = modifier,
        onDeleteRequest = { contest ->
            scope.launch {
                ContestsInfoDataStore(context).ignoredContests.edit {
                    put(contest.compositeId, getCurrentTime())
                }
            }
        }
    )
}

@Composable
private fun ContestsColumn(
    contests: () -> List<Contest>,
    contestsListController: ContestsListController,
    onDeleteRequest: (Contest) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumnOfData(
        modifier = modifier,
        items = contests,
        /*key = { it.compositeId }*/ //TODO: key effects jumping on reorder
    ) { contest ->
        ContestItem(
            contest = contest,
            isExpanded = { contestsListController.isExpanded(contest) },
            collisionType = { contestsListController.collisionType(contest) },
            onDeleteRequest = { onDeleteRequest(contest) },
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoRipple { contestsListController.toggleExpanded(contest) }
                .contestItemPaddings()
                .animateContentSize()
        )
        Divider()
    }
}

internal fun Modifier.contestItemPaddings() =
    padding(
        start = 4.dp,
        end = 7.dp,
        top = 3.dp,
        bottom = 4.dp
    )

@Composable
private fun ContestsFilterTextField(
    filterController: ContestsFilterController,
    modifier: Modifier = Modifier
) {
    if (filterController.enabled) {
        val focusRequester = rememberFocusOnCreationRequester()
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
    contestsListController: ContestsListController,
    filterController: ContestsFilterController,
    loadingStatus: () -> LoadingStatus,
    onReloadClick: () -> Unit
): AdditionalBottomBarBuilder = {
    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()

    ContestsPageSwitchButton(
        showFinished = contestsListController.showFinished,
        onClick = {
            contestsListController.showFinished = it
        }
    )

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
private fun ContestsPageSwitchButton(
    showFinished: Boolean,
    onClick: (Boolean) -> Unit
) {
    CPSIconButton(
        icon = CPSIcons.Swap,
        onClick = {
            onClick(!showFinished)
        }
    )
}

@Composable
private fun rememberIsAnyPlatformEnabled(): State<Boolean> {
    val context = context
    return rememberCollect {
        context.settingsContests.flowOf { prefs ->
            val any1 = prefs[enabledPlatforms].any { it != Contest.Platform.unknown }
            val any2 = prefs[clistAdditionalResources].isNotEmpty()
            any1 || any2
        }
    }
}


@Composable
fun rememberCombinedLoadingStatusState(): State<LoadingStatus> {
    val context = context
    val contestsViewModel = contestsViewModel()

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

    val contestDataState = rememberCollectWithLifecycle {
        monitor.flowOfContestData().map { data ->
            data?.takeIf { it.contestPhase.phase != CodeforcesContestPhase.UNDEFINED }
        }
    }

    AnimatedVisibleByNotNull(
        value = { contestDataState.value },
        enter = enterInColumn(),
        exit = exitInColumn()
    ) {
        val requestFailed by rememberCollectWithLifecycle { monitor.lastRequest.flow.map { it == false } }
        CodeforcesMonitorWidget(
            contestData = it,
            requestFailed = requestFailed,
            modifier = modifier,
            onOpenInBrowser = {
                context.openUrlInBrowser(url = CodeforcesApi.urls.contest(it.contestId))
            },
            onStop = {
                scope.launch {
                    monitor.reset()
                    CodeforcesAccountManager().dataStore(context).monitorCanceledContests.add(
                        it.contestId to getCurrentTime()
                    )
                }
            }
        )
    }
}