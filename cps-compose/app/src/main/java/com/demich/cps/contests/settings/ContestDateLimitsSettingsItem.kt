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
fun ContestDateLimitsSettingsItem(

) {
    val context = context
    val settings = remember { context.settingsContests }
    val dateLimits by rememberCollect { settings.contestsDateLimits.flow }

    ExpandableSettingsItem(
        title = "Date limits",
        collapsedContent = {
            SettingsSubtitle(
                text = "duration ≤ ${dateLimits.maxContestDuration}; start - now ≤ ${dateLimits.nowToStartTimeMaxDuration}; now - end ≤ ${dateLimits.endTimeToNowMaxDuration}"
            )
        },
        expandedContent = {
            Column {
                DurationRow(
                    title = "Max duration = ",
                    duration = dateLimits.maxContestDuration
                )
                DurationRow(
                    title = "Max start - now = ",
                    duration = dateLimits.nowToStartTimeMaxDuration
                )
                DurationRow(
                    title = "Max now - end = ",
                    duration = dateLimits.endTimeToNowMaxDuration
                )
            }
        }
    )
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