package com.demich.cps.contests.monitors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.codeforces.CodeforcesContestPhase
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.flow.map


@Composable
fun CodeforcesMonitorWidget() {
    val context = context
    val monitor = CodeforcesMonitorDataStore(context)

    val phase by rememberCollect {
        monitor.contestInfo.flow.map { it.phase }
    }

    val percentage by rememberCollect {
        monitor.sysTestPercentage.flow
    }

    val problems by rememberCollect {
        monitor.problemIndices.flow
    }

    val lastRequest by rememberCollect {
        monitor.lastRequest.flow
    }

    Column {
        Row {
            Text(text = phase.getTitle())
            if (lastRequest == false) {
                Box(modifier = Modifier.size(width = 16.dp, height = 16.dp).background(
                    color = cpsColors.error
                ))
            }
        }
        if (phase == CodeforcesContestPhase.SYSTEM_TEST) {
            Text(text = percentage.toString())
        }
        if (problems.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                problems.forEach {
                    Text(text = it)
                }
            }
        }
    }
}