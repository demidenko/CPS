package com.demich.cps.develop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import com.demich.cps.utils.*
import com.demich.cps.workers.*
import kotlinx.datetime.Instant

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

    val lastExecutionTime by rememberCollect {
        CPSWorkersDataStore(context).lastExecutionTime.flow
    }

    val lastResult by rememberCollect {
        CPSWorkersDataStore(context).lastResult.flow
    }

    ProvideTimeEachMinute {
        LazyColumn(modifier = modifier) {
            items(items = works, key = { it.name }) { work ->
                WorkerItem(
                    work = work,
                    lastExecutionTime = lastExecutionTime[work.name],
                    lastResult = lastResult[work.name],
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
    lastExecutionTime: Instant?,
    lastResult: CPSWorker.ResultTypes?,
    modifier: Modifier = Modifier
) {

    val workState by remember(key1 = work, calculation = work::workInfoLiveData).observeAsState()

    WorkerItem(
        name = work.name,
        workState = workState?.state ?: WorkInfo.State.CANCELLED,
        progressInfo = workState?.takeIf { it.state == WorkInfo.State.RUNNING }?.getProgressInfo(),
        lastRunTimeAgo = lastExecutionTime?.let {
            timeAgo(fromTime = it, toTime = localCurrentTime)
        } ?: "never",
        lastResult = lastResult,
        modifier = modifier
    )
}

@Composable
private fun WorkerItem(
    name: String,
    workState: WorkInfo.State,
    progressInfo: ProgressBarInfo?,
    lastRunTimeAgo: String,
    lastResult: CPSWorker.ResultTypes?,
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
    result: CPSWorker.ResultTypes,
    modifier: Modifier = Modifier
) {
    if (result == CPSWorker.ResultTypes.SUCCESS) {
        IconSp(
            imageVector = CPSIcons.Done,
            color = cpsColors.success,
            size = 16.sp,
            modifier = modifier
        )
    } else {
        AttentionIcon(
            dangerType = if (result == CPSWorker.ResultTypes.RETRY) DangerType.WARNING else DangerType.DANGER,
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