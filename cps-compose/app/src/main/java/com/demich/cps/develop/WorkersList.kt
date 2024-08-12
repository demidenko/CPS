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
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.contestsViewModel
import com.demich.cps.ui.AttentionIcon
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.bottomprogressbar.CPSProgressIndicator
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.ui.AnimatedVisibleByNotNull
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.context
import com.demich.cps.utils.enterInColumn
import com.demich.cps.utils.exitInColumn
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.rememberCollectWithLifecycle
import com.demich.cps.utils.timeAgo
import com.demich.cps.workers.CPSOneTimeWork
import com.demich.cps.workers.CPSPeriodicWork
import com.demich.cps.workers.CPSWorker
import com.demich.cps.workers.CPSWorkersDataStore
import com.demich.cps.workers.CodeforcesMonitorLauncherWorker
import com.demich.cps.workers.getCPSWorks
import com.demich.cps.workers.getCodeforcesMonitorWork
import com.demich.cps.workers.getProgressInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun WorkersList(modifier: Modifier = Modifier) {
    var showRestartDialogFor: CPSPeriodicWork? by remember { mutableStateOf(null) }
    var showMonitorDialog by remember { mutableStateOf(false) }

    WorkersList(
        modifier = modifier,
        onClick = { showRestartDialogFor = it },
        onCodeforcesMonitorClick = { showMonitorDialog = true }
    )

    showRestartDialogFor?.let { work ->
        CPSYesNoDialog(
            title = {
                Text(
                    text = "restart ${work.name}?",
                    style = CPSDefaults.MonospaceTextStyle
                )
            },
            onDismissRequest = { showRestartDialogFor = null },
            onConfirmRequest = { work.startImmediate() }
        )
    }

    val contestsViewModel = contestsViewModel()
    val context = context
    if (showMonitorDialog) {
        CodeforcesMonitorDialog(onDismissRequest = { showMonitorDialog = false }) {
            contestsViewModel.viewModelScope.launch {
                delay(5.seconds)
                CodeforcesMonitorLauncherWorker.startMonitor(
                    contestId = it,
                    context = context,
                    handle = CodeforcesAccountManager()
                        .dataStore(context)
                        .getSavedInfo()?.handle ?: return@launch
                )
            }
        }
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
    val monitorWork = remember { getCodeforcesMonitorWork(context) }

    val lastExecutionEvents by rememberCollectWithLifecycle {
        CPSWorkersDataStore(context).lastExecutions.flow
    }

    ProvideTimeEachMinute {
        LazyColumn(modifier = modifier) {
            items(items = periodicWorks, key = { it.name }) { work ->
                WorkerItem(
                    work = work,
                    lastExecutionEvent = lastExecutionEvents[work.name],
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(work) }
                        .padding(all = 4.dp)
                )
                Divider()
            }
            item {
                WorkerItem(
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
}

@Composable
private fun WorkerItem(
    work: CPSPeriodicWork,
    lastExecutionEvent: CPSWorker.ExecutionEvent?,
    modifier: Modifier = Modifier
) {
    val workInfo by remember(key1 = work, calculation = work::flowOfWorkInfo).collectAsState(initial = null)

    WorkerItem(
        name = work.name,
        workState = workInfo?.state ?: WorkInfo.State.CANCELLED,
        progressInfo = workInfo?.takeIf { it.state == WorkInfo.State.RUNNING }?.getProgressInfo(),
        lastRunTimeAgo = lastExecutionEvent?.let {
            timeAgo(fromTime = it.start, toTime = localCurrentTime)
        } ?: "never",
        lastResult = lastExecutionEvent?.resultType,
        lastDuration = lastExecutionEvent?.duration?.toNiceString() ?: "",
        modifier = modifier
    )
}

@Composable
private fun WorkerItem(
    work: CPSOneTimeWork,
    modifier: Modifier = Modifier
) {
    val workInfo by remember(key1 = work, calculation = work::flowOfWorkInfo).collectAsState(initial = null)

    WorkerItem(
        name = work.name,
        workState = workInfo?.state ?: WorkInfo.State.CANCELLED,
        progressInfo = null,
        lastRunTimeAgo = "",
        lastResult = null,
        lastDuration = "",
        modifier = modifier
    )
}

private fun Duration.toNiceString(): String {
    val ms = inWholeMilliseconds
    if (ms < 1000) return "${ms}ms"
    val s = ((ms + 50) / 100).toDouble() / 10
    return "${s}s"
}

@Composable
private fun WorkerItem(
    name: String,
    workState: WorkInfo.State,
    progressInfo: ProgressBarInfo?,
    lastRunTimeAgo: String,
    lastResult: CPSWorker.ResultType?,
    lastDuration: String,
    modifier: Modifier = Modifier
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
            Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (lastResult != null) {
                    ResultIcon(
                        result = lastResult,
                        modifier = Modifier.padding(end = 3.dp)
                    )
                }
                Text(
                    text = lastRunTimeAgo,
                    fontSize = 14.sp,
                    color = cpsColors.contentAdditional
                )
                if (lastDuration.isNotEmpty()) {
                    Text(
                        text = "($lastDuration)",
                        fontSize = 14.sp,
                        color = cpsColors.contentAdditional,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
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
                value = { progressInfo },
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
        WorkInfo.State.ENQUEUED, WorkInfo.State.FAILED, WorkInfo.State.SUCCEEDED -> content
        WorkInfo.State.RUNNING -> success
        WorkInfo.State.BLOCKED -> error
        WorkInfo.State.CANCELLED -> contentAdditional
    }
}


@Composable
private fun CodeforcesMonitorDialog(onDismissRequest: () -> Unit, onStart: (Int) -> Unit) {
    var contestId by rememberSaveable { mutableStateOf("") }
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