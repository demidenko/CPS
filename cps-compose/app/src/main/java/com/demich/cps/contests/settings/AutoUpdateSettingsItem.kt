package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.demich.cps.ui.dialogs.CPSDialogSelect
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.settings.SubtitledByValue
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.workers.ContestsWorker
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun SettingsContainerScope.AutoUpdateSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    var showDialog by rememberSaveable { mutableStateOf(false) }
    SubtitledByValue(
        item = context.settingsContests.autoUpdateInterval,
        title = "Background auto update",
        modifier = Modifier.clickable { showDialog = true }
    ) { interval: Duration? ->
        Text(text = title(interval))
    }

    if (showDialog) {
        IntervalSelectDialog(
            onDismiss = { showDialog = false },
            onSelectOption = {
                scope.launch {
                    context.settingsContests.autoUpdateInterval.setValue(it)
                    with(ContestsWorker.getWork(context)) {
                        if (it == null) stop()
                        else enqueueIfEnabled()
                    }
                }
            }
        )
    }
}

//TODO: to human string (1 hour instead of 1h)
private fun title(duration: Duration?): String =
    if (duration == null) "disabled"
    else "every $duration"

@Composable
private fun IntervalSelectDialog(
    onDismiss: () -> Unit,
    onSelectOption: (Duration?) -> Unit
) {
    val context = context
    val selected by collectItemAsState { context.settingsContests.autoUpdateInterval }

    val options = remember {
        listOf(null, 15.minutes, 30.minutes, 1.hours, 2.hours, 6.hours)
    }

    CPSDialogSelect(
        title = "Autoupdate interval",
        options = options,
        selectedOption = selected,
        optionTitle = { Text(text = title(it)) },
        onDismissRequest = onDismiss,
        onSelectOption = onSelectOption
    )
}