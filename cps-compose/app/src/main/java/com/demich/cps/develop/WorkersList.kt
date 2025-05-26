package com.demich.cps.develop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.ui.AnimatedVisibleByNotNull
import com.demich.cps.ui.AttentionIcon
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.bottomprogressbar.CPSProgressIndicator
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.bottomprogressbar.progressBarsViewModel
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.collectAsStateWithLifecycle
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.enterInColumn
import com.demich.cps.utils.exitInColumn
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.timeAgo
import com.demich.cps.workers.CPSOneTimeWork
import com.demich.cps.workers.CPSPeriodicWork
import com.demich.cps.workers.CPSWork
import com.demich.cps.workers.CPSWorker
import com.demich.cps.workers.CPSWorkersDataStore
import com.demich.cps.workers.CodeforcesMonitorWorker
import com.demich.cps.workers.getCPSWorks
import com.demich.cps.workers.getProgressInfo
import com.demich.cps.workers.isRunning
import com.demich.cps.workers.nextScheduleTime
import com.demich.cps.workers.repeatInterval
import com.demich.cps.workers.stateOrCancelled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun WorkersList(modifier: Modifier = Modifier) {
    var showMonitorDialog by remember { mutableStateOf(false) }
    var showRestartDialogFor: CPSPeriodicWork? by remember { mutableStateOf(null) }
    ProvideTimeEachMinute {
        WorkersList(
            modifier = modifier,
            onClick = { showRestartDialogFor = it },
            onCodeforcesMonitorClick = { showMonitorDialog = true }
        )

        showRestartDialogFor?.let { work ->
            WorkerDialog(
                work = work,
                onDismissRequest = { showRestartDialogFor = null }
            )
        }
    }


    val context = context
    val progressBarsViewModel = progressBarsViewModel()
    if (showMonitorDialog) {
        CodeforcesMonitorDialog(onDismissRequest = { showMonitorDialog = false }) { contestId ->
            progressBarsViewModel.doJob(id = "run_cf_monitor $contestId") { progress ->
                val handle = CodeforcesAccountManager()
                    .dataStore(context)
                    .getSavedInfo()?.handle ?: return@doJob
                var progressInfo = ProgressBarInfo(total = 5, title = "cf monitor")
                progress(progressInfo)
                repeat(progressInfo.total) {
                    delay(1.seconds)
                    progressInfo++
                    progress(progressInfo)
                }
                CodeforcesMonitorWorker.start(
                    contestId = contestId,
                    context = context,
                    handle = handle
                )
            }
        }
    }
}

@Composable
private fun WorkerDialog(work: CPSPeriodicWork, onDismissRequest: () -> Unit) {
    val workInfo by work.workInfoAsState()
    val events by work.eventsState()

    CPSDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest
    ) {
        ProvideTextStyle(value = CPSDefaults.MonospaceTextStyle) {
            Text(text = work.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                when (val state = workInfo.stateOrCancelled) {
                    WorkInfo.State.ENQUEUED -> {
                        workInfo?.nextScheduleTime?.let { nextTime ->
                            val d = nextTime - localCurrentTime
                            Text(text = "next: in ${d.dropSeconds()}")
                        }
                    }
                    else -> {
                        Text(text = "$state")
                    }
                }

                workInfo?.repeatInterval?.let {
                    Text(text = "repeat interval: $it")
                }

                Text(text = buildString {
                    append("events: ")
                    append(events.count { it.resultType == CPSWorker.ResultType.SUCCESS })
                    append(" / ")
                    append(events.size)
                })
            }
        }

        Button(
            content = { Text(text = "restart") },
            onClick = {
                work.startImmediate()
                onDismissRequest()
            }
        )
    }
}

@Composable
private fun WorkersList(
    modifier: Modifier,
    onClick: (CPSPeriodicWork) -> Unit,
    onCodeforcesMonitorClick: (CPSOneTimeWork) -> Unit
) {
    val context = context
    val periodicWorks = remember { context.getCPSWorks() }
    val monitorWork = remember { CodeforcesMonitorWorker.getWork(context) }

    val executionEvents by collectAsStateWithLifecycle {
        CPSWorkersDataStore(context).executions.flow
    }

    LazyColumn(modifier = modifier) {
        items(items = periodicWorks, key = { it.name }) { work ->
            WorkerItem(
                work = work,
                executionEvents = executionEvents[work.name],
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(work) }
                    .padding(all = 4.dp)
            )
            Divider()
        }
        item {
            CodeforcesMonitorWorkItem(
                work = monitorWork,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCodeforcesMonitorClick(monitorWork) }
                    .padding(all = 4.dp)
            )
            Divider()
        }
    }
}

@Composable
private fun CPSWork.workInfoAsState(): State<WorkInfo?> =
    remember(key1 = this, calculation = ::flowOfWorkInfo)
        .collectAsStateWithLifecycle(initialValue = null)

@Composable
private fun CPSWork.eventsState(): State<List<CPSWorker.ExecutionEvent>> =
    collectAsStateWithLifecycle {
        CPSWorkersDataStore(context).executions.flow.map {
            it.getOrElse(name) { emptyList() }
        }
    }

@Composable
private fun WorkerItem(
    work: CPSPeriodicWork,
    executionEvents: List<CPSWorker.ExecutionEvent>?,
    modifier: Modifier = Modifier
) {
    val workInfo by work.workInfoAsState()
    val lastExecutionEvent = executionEvents?.maxByOrNull { it.start }

    WorkerItem(
        name = work.name,
        workState = workInfo.stateOrCancelled,
        progressInfo = { workInfo?.takeIf { it.isRunning }?.getProgressInfo() },
        lastExecTime = lastExecutionEvent?.start,
        lastResult = lastExecutionEvent?.resultType,
        lastDuration = lastExecutionEvent?.duration,
        modifier = modifier
    )
}

@Composable
private fun CodeforcesMonitorWorkItem(
    work: CPSOneTimeWork,
    modifier: Modifier = Modifier
) {
    val context = context
    val workInfo by work.workInfoAsState()
    val contestId by collectItemAsState { CodeforcesMonitorDataStore(context).contestId }

    WorkerItem(
        modifier = modifier,
        name = work.name,
        workState = workInfo.stateOrCancelled,
        progressInfo = { null }
    ) {
        Text(text = "contestId = $contestId")
    }
}

private fun Duration.toNiceString(): String {
    if (this < 1.seconds) return toString(unit = DurationUnit.MILLISECONDS)
    return toString(unit = DurationUnit.SECONDS, decimals = 1).replace(',', '.')
}

private fun Duration.dropSeconds(): Duration {
    return inWholeMinutes.minutes
}

@Composable
private fun WorkerItem(
    modifier: Modifier = Modifier,
    name: String,
    workState: WorkInfo.State,
    progressInfo: () -> ProgressBarInfo?,
    lastExecTime: Instant?,
    lastResult: CPSWorker.ResultType?,
    lastDuration: Duration?
) {
    WorkerItem(
        modifier = modifier,
        name = name,
        workState = workState,
        progressInfo = progressInfo
    ) {
        Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (lastResult != null) {
                ResultIcon(
                    result = lastResult,
                    modifier = Modifier.padding(end = 3.dp)
                )
            }
            Text(text = lastExecTime?.let { timeAgo(fromTime = it, toTime = localCurrentTime) } ?: "never")
            if (lastDuration != null) {
                Text(
                    text = "(${lastDuration.toNiceString()})",
                    modifier = Modifier.padding(start = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkerItem(
    modifier: Modifier = Modifier,
    name: String,
    workState: WorkInfo.State,
    progressInfo: () -> ProgressBarInfo?,
    additional: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = CPSDefaults.MonospaceTextStyle.copy(fontSize = 18.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ProvideTextStyle(TextStyle(
                fontSize = 14.sp,
                color = cpsColors.contentAdditional
            )) {
                additional()
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(IntrinsicSize.Min)
        ) {
            Text(
                text = workState.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorFor(workState)
            )
            AnimatedVisibleByNotNull(
                value = progressInfo,
                enter = enterInColumn(),
                exit = exitInColumn()
            ) {
                CPSProgressIndicator(
                    progressBarInfo = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultIcon(
    result: CPSWorker.ResultType,
    modifier: Modifier = Modifier
) {
    if (result == CPSWorker.ResultType.SUCCESS) {
        IconSp(
            imageVector = CPSIcons.Done,
            color = cpsColors.success,
            size = 16.sp,
            modifier = modifier
        )
    } else {
        AttentionIcon(
            dangerType = if (result == CPSWorker.ResultType.RETRY) DangerType.WARNING else DangerType.DANGER,
            modifier = modifier
        )
    }
}

@Composable
@ReadOnlyComposable
private fun colorFor(workState: WorkInfo.State) = with(cpsColors) {
    when (workState) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.SUCCEEDED -> content
        WorkInfo.State.RUNNING -> success
        WorkInfo.State.BLOCKED, WorkInfo.State.FAILED -> error
        WorkInfo.State.CANCELLED -> contentAdditional
    }
}


@Composable
private fun CodeforcesMonitorDialog(onDismissRequest: () -> Unit, onStart: (Int) -> Unit) {
    val context = context
    var contestId: String by rememberSaveable {
        mutableStateOf(value = runBlocking {
            CodeforcesMonitorDataStore(context).contestId()?.toString() ?: ""
        })
    }
    CPSYesNoDialog(
        onDismissRequest = onDismissRequest,
        onConfirmRequest = { contestId.toIntOrNull()?.let(onStart) },
        title = {
            TextField(
                value = contestId,
                onValueChange = { contestId = it },
                label = { Text("contestId") },
                isError = contestId.toIntOrNull() == null
            )
        }
    )
}