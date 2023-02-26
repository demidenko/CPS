package com.demich.cps.develop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import com.demich.cps.ui.MonospacedText
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
            title = { MonospacedText("restart ${work.name}?") },
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

    ProvideTimeEachMinute {
        LazyColumn(modifier = modifier) {
            items(items = works, key = { it.name }) { work ->
                WorkerItem(
                    work = work,
                    lastExecutionTime = lastExecutionTime[work.name],
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
    modifier: Modifier = Modifier
) {

    val workState by work.workInfoState()

    WorkerItem(
        name = work.name,
        workState = workState?.state ?: WorkInfo.State.CANCELLED,
        progressInfo = workState?.takeIf { it.state == WorkInfo.State.RUNNING }?.getProgressInfo(),
        lastRunTimeAgo = lastExecutionTime?.let {
            timeAgo(fromTime = it, toTime = LocalCurrentTime.current)
        } ?: "never",
        modifier = modifier
    )
}

@Composable
private fun WorkerItem(
    name: String,
    workState: WorkInfo.State,
    progressInfo: ProgressBarInfo?,
    lastRunTimeAgo: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonospacedText(
                text = name,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "last run: $lastRunTimeAgo",
                fontSize = 14.sp,
                color = cpsColors.contentAdditional,
                modifier = Modifier.padding(top = 3.dp)
            )
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
                color = when (workState) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.FAILED, WorkInfo.State.SUCCEEDED -> cpsColors.content
                    WorkInfo.State.RUNNING -> cpsColors.success
                    WorkInfo.State.BLOCKED -> cpsColors.error
                    WorkInfo.State.CANCELLED -> cpsColors.contentAdditional
                }
            )
            //TODO: fadeIn / fadeOut
            if (progressInfo != null) {
                CPSProgressIndicator(
                    progressBarInfo = progressInfo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                )
            }
        }
    }
}