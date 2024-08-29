package com.demich.cps.contests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.demich.cps.ui.AnimatedVisibleByNotNull
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.CPSSwipeRefreshBox
import com.demich.cps.ui.EmptyMessageBox
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.filter.FilterIconButton
import com.demich.cps.ui.filter.FilterState
import com.demich.cps.ui.filter.FilterTextField
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.ProvideCurrentTime
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.context
import com.demich.cps.utils.enterInColumn
import com.demich.cps.utils.exitInColumn
import com.demich.cps.utils.filterByTokensAsSubsequence
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.rememberCollect
import com.demich.cps.utils.collectAsStateWithLifecycle
import com.demich.cps.workers.ContestsWorker
import com.demich.datastore_itemized.add
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@Composable
fun ContestsScreen(
    contestsListController: ContestsListController,
    filterState: FilterState,
    isReloading: () -> Boolean,
    onReload: () -> Unit
) {
    val context = context
    val contestsViewModel = contestsViewModel()

    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()

    Column {
        CodeforcesMonitor(modifier = Modifier.fillMaxWidth())
        if (isAnyPlatformEnabled) {
            ContestsReloadableContent(
                contestsListController = contestsListController,
                filterState = filterState,
                isReloading = isReloading,
                onReload = onReload,
                modifier = Modifier.weight(1f, false)
            )
            FilterTextField(
                filterState = filterState,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            NoneEnabledMessage(modifier = Modifier.fillMaxSize())
        }
    }

    DisposableEffect(contestsViewModel) {
        contestsViewModel.syncEnabledAndLastReloaded(context)
        onDispose { }
    }
}

@Composable
private fun ContestsReloadableContent(
    contestsListController: ContestsListController,
    filterState: FilterState,
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
            filterState = filterState
        )
    }
}

@Composable
private fun ContestsContent(
    contestsListController: ContestsListController,
    filterState: FilterState
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
            filterState = filterState,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Composable
private fun ContestsPager(
    contestsListController: ContestsListController,
    filterState: FilterState,
    modifier: Modifier = Modifier
) {
    val (
        contestsState: State<SortedContests>,
        currentTimeState: State<Instant>
    ) = produceSortedContestsWithTime()

    LaunchedEffect(contestsState, filterState, contestsListController) {
        snapshotFlow { contestsState.value.contests }
            .collect { contests ->
                filterState.available = contests.isNotEmpty()
                contestsListController.applyContests(contests)
            }
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    ProvideCurrentTime(currentTimeState) {
        val page = contestsListController.contestsPage
        saveableStateHolder.SaveableStateProvider(key = page) {
            ContestsPage(
                contests = { contestsState.value.sublist(page) },
                contestsListController = contestsListController,
                filterState = filterState,
                modifier = modifier
            )
        }
    }
}

private fun FilterState.filterContests(contests: List<Contest>) =
    contests.filterByTokensAsSubsequence(filter) {
        sequence {
            yield(title)
            if (platform != Contest.Platform.unknown) yield(platform.name)
            host?.let { yield(it) }
        }
    }

@Composable
private fun ContestsPage(
    contests: () -> List<Contest>,
    contestsListController: ContestsListController,
    filterState: FilterState,
    modifier: Modifier = Modifier
) {
    val context = context
    val scope = rememberCoroutineScope()

    val filtered by remember(contests, filterState) {
        derivedStateOf {
            filterState.filterContests(contests())
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

@OptIn(ExperimentalFoundationApi::class)
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
        key = { it.compositeId }
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
                .animateItemPlacement(spring())
                .animateContentSize(spring())
        )
        Divider(Modifier.animateItemPlacement(spring()))
    }
}

internal fun Modifier.contestItemPaddings() =
    this.padding(
        start = 4.dp,
        end = 7.dp,
        top = 3.dp,
        bottom = 4.dp
    )

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
    filterState: FilterState,
    loadingStatus: () -> LoadingStatus,
    onReloadClick: () -> Unit
): AdditionalBottomBarBuilder = {
    val isAnyPlatformEnabled by rememberIsAnyPlatformEnabled()

    if (isAnyPlatformEnabled) {
        ContestsPageSwitchButton(
            contestsPage = contestsListController.contestsPage,
            onClick = {
                contestsListController.contestsPage = it
            }
        )
    }

    if (isAnyPlatformEnabled) {
        FilterIconButton(filterState = filterState)
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus(),
        enabled = isAnyPlatformEnabled,
        onClick = onReloadClick
    )
}

@Composable
private fun ContestsPageSwitchButton(
    contestsPage: ContestsListController.ContestsPage,
    onClick: (ContestsListController.ContestsPage) -> Unit
) {
    CPSIconButton(
        icon = CPSIcons.Swap,
        onClick = {
            onClick(
                when (contestsPage) {
                    ContestsListController.ContestsPage.Finished -> ContestsListController.ContestsPage.RunningOrFuture
                    ContestsListController.ContestsPage.RunningOrFuture -> ContestsListController.ContestsPage.Finished
                }
            )
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
            .combine(ContestsWorker.getWork(context).flowOfWorkInfo()) { loadingStatus, workInfo ->
                if (workInfo?.state == WorkInfo.State.RUNNING) LoadingStatus.LOADING
                else loadingStatus
            }.collect {
                value = it
            }
    }
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

    val contestDataState = collectAsStateWithLifecycle {
        monitor.flowOfContestData().map { data ->
            data?.takeIf { it.contestPhase.phase != CodeforcesContestPhase.UNDEFINED }
        }
    }
    //TODO: restart killed worker if data not null

    AnimatedVisibleByNotNull(
        value = { contestDataState.value },
        enter = enterInColumn(),
        exit = exitInColumn()
    ) {
        val requestFailed by collectAsStateWithLifecycle { monitor.lastRequest.flow.map { it == false } }
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