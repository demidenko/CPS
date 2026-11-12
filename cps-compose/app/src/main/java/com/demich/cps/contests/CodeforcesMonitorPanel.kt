package com.demich.cps.contests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.contests.monitors.CodeforcesMonitorFullData
import com.demich.cps.contests.monitors.CodeforcesMonitorWidget
import com.demich.cps.contests.monitors.contestId
import com.demich.cps.contests.monitors.flowOfFullData
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.profiles.managers.CodeforcesProfileManager
import com.demich.cps.ui.AnimatedVisibleOfNotNull
import com.demich.cps.utils.add
import com.demich.cps.utils.backgroundCoroutineScope
import com.demich.cps.utils.context
import com.demich.cps.utils.enterInColumn
import com.demich.cps.utils.exitInColumn
import com.demich.cps.utils.getSystemTime
import com.demich.cps.utils.onNotNull
import com.demich.cps.workers.CodeforcesMonitorWorker
import com.demich.cps.workers.state
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CodeforcesMonitorPanel(modifier: Modifier = Modifier) {
    val context = context
    val unitsFactory = LocalContestsUnitsFactory.current
    val uriHandler = LocalUriHandler.current

    val state = remember {
        unitsFactory.codeforcesMonitorStorage().flowOfFullData().map {
            PanelState(it)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    state.onNotNull { panelState ->
        AnimatedVisibleOfNotNull(
            modifier = modifier,
            value = panelState.data,
            enter = enterInColumn(),
            exit = exitInColumn()
        ) { data ->
            val contestData = data.monitorData
            if (contestData != null) {
                val scope = backgroundCoroutineScope
                CodeforcesMonitorWidget(
                    contestData = contestData,
                    requestFailed = data.failureMsg != null,
                    modifier = Modifier.fillMaxWidth(),
                    onOpenInBrowser = {
                        uriHandler.openUri(CodeforcesUrls.contest(contestData.contestId))
                    },
                    onStop = {
                        val contestId = contestData.contestId
                        scope.launch {
                            CodeforcesProfileManager().profileStorage(context)
                                .monitorCanceledContests.add(contestId, getSystemTime())
                            unitsFactory.codeforcesMonitorStorage().reset()
                        }
                    }
                )
            } else {
                // TODO
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("monitor no data")
                    data.failureMsg?.let { msg ->
                        Text(text = msg, fontSize = 14.sp)
                    }
                }
            }

            // TODO: show worker status (failed)

            // restart failed monitor
            LaunchedEffect(data.args) {
                if (CodeforcesMonitorWorker.getWork(context).state() == FAILED) {
                    CodeforcesMonitorWorker.start(startArgs = data.args, context = context)
                }
            }
        }
    }
}

private data class PanelState(
    val data: CodeforcesMonitorFullData?
)