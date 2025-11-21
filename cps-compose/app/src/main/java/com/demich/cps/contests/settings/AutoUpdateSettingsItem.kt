package com.demich.cps.contests.settings

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.demich.cps.ui.settings.SelectSubtitled
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.utils.context
import com.demich.cps.workers.ContestsWorker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
context(scope: SettingsContainerScope)
internal fun AutoUpdateSettingsItem() {
    val context = context

    SelectSubtitled(
        title = "Background auto update",
        item = context.settingsContests.autoUpdateInterval,
        options = remember {
            listOf(null, 15.minutes, 30.minutes, 1.hours, 2.hours, 6.hours)
        },
        onOptionSaved = {
            with(ContestsWorker.getWork(context)) {
                if (it == null) stop()
                else enqueueIfEnabled()
            }
        },
        optionTitle = {
            Text(text = title(it))
        }
    )
}

//TODO: to human string (1 hour instead of 1h)
private fun title(duration: Duration?): String =
    if (duration == null) "disabled"
    else "every $duration"
