package com.demich.cps

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkInfo
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatedUserInfo
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.ui.*
import com.demich.cps.ui.bottomprogressbar.LinearProgressIndicatorRounded
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.workers.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.random.Random


class SettingsDev(context: Context): CPSDataStore(context.settings_dev_dataStore) {
    companion object {
        private val Context.settings_dev_dataStore by preferencesDataStore("settings_develop")
    }

    val devModeEnabled = itemBoolean(name = "develop_enabled", defaultValue = false)
}

val Context.settingsDev: SettingsDev
    get() = SettingsDev(this)


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevelopScreen() {
    val scope = rememberCoroutineScope()

    val context = context

    //TestHandles(modifier = Modifier.fillMaxWidth())

    WorkersList(modifier = Modifier.fillMaxWidth())
}


fun developAdditionalBottomBarBuilder(
    progressBarsViewModel: ProgressBarsViewModel
): AdditionalBottomBarBuilder = {
    CPSIconButton(icon = CPSIcons.Add) {
        progressBarsViewModel.doJob(
            id = Random.nextLong().toString()
        ) { state ->
            val total = Random.nextInt(5, 15)
            state.value = state.value.copy(total = total, title = total.toString())
            repeat(total) {
                delay(1000)
                state.value++
            }
        }
    }
}

@Composable
fun ContentLoadingButton(
    text: String,
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit
) {
    var enabled by rememberSaveable { mutableStateOf(true) }
    Button(
        enabled = enabled,
        onClick = {
            enabled = false
            coroutineScope.launch {
                block()
                enabled = true
            }
        }
    ) {
        Text(text = text)
    }
}

@Composable
private fun TestHandles(
    modifier: Modifier = Modifier
) {
    val context = context
    val managers = remember {
        context.allAccountManagers
            .filterIsInstance<RatedAccountManager<*>>()
    }

    var selectedType by rememberSaveable {
        mutableStateOf(AccountManagers.codeforces)
    }

    Row(modifier = modifier) {
        HandlesList(
            manager = managers.first { it.type == selectedType },
            modifier = Modifier.weight(1f)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            managers.forEach { manager ->
                CPSRadioButtonTitled(
                    title = { Text(text = manager.type.name) },
                    selected = selectedType == manager.type
                ) {
                    selectedType = manager.type
                }
            }
        }
    }
}

@Composable
private fun HandlesList(
    manager: RatedAccountManager<out RatedUserInfo>,
    modifier: Modifier = Modifier
) {
    LazyColumnWithScrollBar(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(4000) { rating ->
            Text(
                text = manager.makeRatedSpan(text = rating.toString(), rating = rating)
            )
        }
    }
}

@Composable
private fun WorkersList(
    modifier: Modifier = Modifier
) {
    val context = context
    val works = remember { context.getCPSWorks() }

    val lastExecutionTime by rememberCollect {
        CPSWorkersDataStore(context).lastExecutionTime.flow
    }

    val currentTime by collectCurrentTimeEachSecond()
    CompositionLocalProvider(LocalCurrentTime provides currentTime) {
        LazyColumn(modifier = modifier) {
            items(items = works, key = { it.name }) { work ->
                WorkerItem(
                    work = work,
                    lastExecutionTime = lastExecutionTime[work.name],
                    modifier = Modifier
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
        workState = workState?.state,
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
    workState: WorkInfo.State?,
    progressInfo: ProgressBarInfo?,
    lastRunTimeAgo: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonospacedText(
                text = name,
                fontSize = 20.sp
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
                text = workState?.name ?: "???",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = when (workState) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.FAILED, WorkInfo.State.SUCCEEDED -> cpsColors.content
                    WorkInfo.State.RUNNING -> cpsColors.success
                    WorkInfo.State.BLOCKED -> cpsColors.error
                    WorkInfo.State.CANCELLED, null -> cpsColors.contentAdditional
                }
            )
            if (progressInfo != null) {
                LinearProgressIndicatorRounded(
                    progress = progressInfo.fraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
            }
        }
    }
}