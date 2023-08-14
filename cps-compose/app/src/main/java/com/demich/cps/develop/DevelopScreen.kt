package com.demich.cps.develop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.allRatedAccountManagers
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.contests.contestsViewModel
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSRadioButtonTitled
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.bottomprogressbar.progressBarsViewModel
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.utils.context
import com.demich.cps.workers.CodeforcesMonitorLauncherWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Composable
fun DevelopScreen() {
    //TestHandles(modifier = Modifier.fillMaxWidth())
    Column {
        WorkersList(modifier = Modifier.fillMaxWidth())
    }
}


fun developAdditionalBottomBarBuilder(): AdditionalBottomBarBuilder = {
    val context = context
    val progressBarsViewModel = progressBarsViewModel()

    CPSIconButton(icon = CPSIcons.Add) {
        progressBarsViewModel.doJob(
            id = Random.nextLong().toString()
        ) { state ->
            val total = Random.nextInt(5, 15)
            state.value = state.value.copy(total = total, title = total.toString())
            repeat(total) {
                delay(1.seconds)
                state.value++
            }
        }
    }

    var showMonitorDialog by remember { mutableStateOf(false) }
    CPSIconButton(icon = CPSIcons.Monitor) {
        showMonitorDialog = true
    }

    val contestsViewModel = contestsViewModel()
    if (showMonitorDialog) {
        MonitorDialog(onDismissRequest = { showMonitorDialog = false }) {
            contestsViewModel.viewModelScope.launch {
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
private fun MonitorDialog(onDismissRequest: () -> Unit, onStart: (Int) -> Unit) {
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

@Composable
fun ContentLoadingButton(
    text: String,
    block: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var enabled by rememberSaveable { mutableStateOf(true) }
    Button(
        enabled = enabled,
        onClick = {
            enabled = false
            scope.launch {
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
    val managers = remember { allRatedAccountManagers }

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

