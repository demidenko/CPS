package com.demich.cps.develop

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatedUserInfo
import com.demich.cps.accounts.managers.allAccountManagers
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.ui.*
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.*
import com.demich.cps.workers.CodeforcesMonitorLauncherWorker
import com.demich.datastore_itemized.ItemizedDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds


class SettingsDev(context: Context): ItemizedDataStore(context.settings_dev_dataStore) {
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

    Column {
        WorkersList(modifier = Modifier.fillMaxWidth())
        MonitorFields(modifier = Modifier.fillMaxWidth())
    }
}


@Composable
private fun MonitorFields(modifier: Modifier = Modifier) {
    val context = context
    val scope = rememberCoroutineScope()

    val handle by with(LocalCodeforcesAccountManager.current) {
        rememberCollect {
            flowOfInfo().map { it.handle }
        }
    }

    var contestId by rememberSaveable { mutableStateOf("") }

    Row(modifier = modifier) {
        TextField(
            value = contestId,
            onValueChange = { contestId = it },
            label = { Text("contestId") }
        )
        Button(
            content = { Text("start") },
            onClick = {
                scope.launch {
                    CodeforcesMonitorLauncherWorker.startMonitor(
                        contestId = contestId.toIntOrNull() ?: return@launch,
                        handle = handle,
                        context = context
                    )
                }
            }
        )
    }
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
                delay(1.seconds)
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

