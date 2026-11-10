package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.demich.cps.contests.monitors.CodeforcesMonitorWidget
import com.demich.cps.contests.monitors.contestId
import com.demich.cps.contests.monitors.flowOfContestData
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.profiles.managers.CodeforcesProfileManager
import com.demich.cps.ui.AnimatedVisibleByNotNull
import com.demich.cps.utils.add
import com.demich.cps.utils.backgroundCoroutineScope
import com.demich.cps.utils.collectAsStateWithLifecycle
import com.demich.cps.utils.context
import com.demich.cps.utils.enterInColumn
import com.demich.cps.utils.exitInColumn
import com.demich.cps.utils.getSystemTime
import com.demich.cps.workers.CodeforcesMonitorWorker
import com.demich.cps.workers.state
import kotlinx.coroutines.launch

@Composable
fun CodeforcesMonitorPanel(modifier: Modifier = Modifier) {
    val context = context
    val unitsFactory = LocalContestsUnitsFactory.current
    val uriHandler = LocalUriHandler.current

    val contestDataState = collectAsStateWithLifecycle {
        unitsFactory.codeforcesMonitorStorage().flowOfContestData()
    }

    AnimatedVisibleByNotNull(
        value = { contestDataState.value },
        enter = enterInColumn(),
        exit = exitInColumn()
    ) { contestData ->
        val scope = backgroundCoroutineScope

        // TODO: merge with contestData
        val requestFailed by remember { unitsFactory.codeforcesMonitorStorage().lastRequest }
            .collectAsStateWithLifecycle(initial = null)

        CodeforcesMonitorWidget(
            contestData = contestData,
            requestFailed = requestFailed ?: false,
            modifier = modifier,
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

        // restart failed monitor
        // TODO: check args instead of contestData?
        LaunchedEffect(Unit) {
            if (CodeforcesMonitorWorker.getWork(context).state() == FAILED) {
                val args = unitsFactory.codeforcesMonitorStorage().args() ?: return@LaunchedEffect
                CodeforcesMonitorWorker.start(startArgs = args, context = context)
            }
        }
    }
}