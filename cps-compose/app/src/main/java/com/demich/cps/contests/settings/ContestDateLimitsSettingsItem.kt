package com.demich.cps.contests.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.ui.ExpandableSettingsItem
import com.demich.cps.ui.SettingsSubtitle
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlin.time.Duration

@Composable
fun ContestDateLimitsSettingsItem() {
    val context = context
    val settings = remember { context.settingsContests }
    val dateConstraints by rememberCollect { settings.contestsDateConstraints.flow }

    ExpandableSettingsItem(
        title = "Date constraints",
        collapsedContent = { SettingsSubtitle(makeInfoString(dateConstraints)) },
        expandedContent = {
            Column {
                DurationRow(
                    title = "Max duration = ",
                    duration = dateConstraints.maxDuration
                )
                DurationRow(
                    title = "Max start - now = ",
                    duration = dateConstraints.nowToStartTimeMaxDuration
                )
                DurationRow(
                    title = "Max now - end = ",
                    duration = dateConstraints.endTimeToNowMaxDuration
                )
            }
        }
    )
}

private fun makeInfoString(dateConstraints: ContestDateConstraints): String =
    with(dateConstraints) {
        "duration ≤ $maxDuration; start ≤ now + $nowToStartTimeMaxDuration; now - $endTimeToNowMaxDuration ≤ end"
    }

@Composable
private fun DurationRow(
    title: String,
    duration: Duration,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(text = title, modifier = Modifier.weight(1f))
        Text(text = "$duration")
    }
}