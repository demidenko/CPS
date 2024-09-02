package com.demich.cps.develop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.allRatedAccountManagers
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.community.codeforces.CodeforcesNewEntriesDataStore
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSRadioButtonTitled
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.bottomprogressbar.progressBarsViewModel
import com.demich.cps.ui.lazylist.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Composable
fun DevelopScreen() {
    //TestHandles(modifier = Modifier.fillMaxWidth())
    Column {
        WorkersList(modifier = Modifier.fillMaxWidth().weight(1f))
        NewEntriesCfInfo(modifier = Modifier.fillMaxWidth().padding(all = 12.dp))
    }
}


fun developAdditionalBottomBarBuilder(): AdditionalBottomBarBuilder = {
    val progressBarsViewModel = progressBarsViewModel()

    CPSIconButton(icon = CPSIcons.Add) {
        progressBarsViewModel.doJob(
            id = Random.nextLong().toString()
        ) { progress ->
            val total = Random.nextInt(5, 15)
            progress(ProgressBarInfo(total = total, title = total.toString()))
            repeat(total) {
                delay(1.seconds)
                progress(ProgressBarInfo(total = total, title = total.toString(), current = it + 1))
            }
        }
    }
}

@Composable
private fun NewEntriesCfInfo(
    modifier: Modifier = Modifier
) {
    val context = context
    val count by collectAsState {
        CodeforcesNewEntriesDataStore(context).commonNewEntries.flow.map { it.size }
    }
    Text(
        modifier = modifier,
        text = "cf new entries: $count",
        style = CPSDefaults.MonospaceTextStyle.copy(fontSize = 16.sp)
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
        mutableStateOf(AccountManagerType.codeforces)
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
                text = manager.makeRatedSpan(
                    text = rating.toString(),
                    rating = rating,
                    cpsColors = cpsColors
                )
            )
        }
    }
}

