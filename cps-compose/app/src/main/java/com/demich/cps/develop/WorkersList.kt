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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import com.demich.cps.ui.AttentionIcon
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.bottomprogressbar.CPSProgressIndicator
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.AnimatedVisibleByNotNull
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.context
import com.demich.cps.utils.enterInColumn
import com.demich.cps.utils.exitInColumn
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.rememberCollectWithLifecycle
import com.demich.cps.utils.timeAgo
import com.demich.cps.workers.CPSWork
import com.demich.cps.workers.CPSWorker
import com.demich.cps.workers.CPSWorkersDataStore
import com.demich.cps.workers.getCPSWorks
import com.demich.cps.workers.getProgressInfo
import kotlin.time.Duration

@Composable
fun WorkersList(modifier: Modifier = Modifier) {
    var showRestartDialogFor: CPSWork? by remember { mutableStateOf(null) }

    WorkersList(modifier = modifier, onClick = { showRestartDialogFor = it })

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
}

@Composable
private fun WorkersList(
    modifier: Modifier,
    onClick: (CPSWork) -> Unit
) {
    val context = context
    val works = remember { context.getCPSWorks() }

    val lastExecutionEvents by rememberCollectWithLifecycle {
        CPSWorkersDataStore(context).lastExecutions.flow
    }

    ProvideTimeEachMinute {
        LazyColumn(modifier = modifier) {
            items(items = works, key = { it.name }) { work ->
                WorkerItem(
                    work = work,
                    lastExecutionEvent = lastExecutionEvents[work.name],
                    modifier = Modifier
                        .clickable { onClick(work) }
                        .fillMaxWidth()
                        .padding(all = 4.dp)
                )
                Divider()
            }
        }
    }
}

@Composable
private fun WorkerItem(
    work: CPSWork,
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