package com.demich.cps.develop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.allRatedAccountManagers
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSRadioButtonTitled
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.bottomprogressbar.progressBarsViewModel
import com.demich.cps.ui.lazylist.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
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

