package com.demich.cps.contests.monitors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesContestPhase
import com.demich.cps.utils.codeforces.CodeforcesParticipationType
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


@Composable
fun CodeforcesMonitorWidget(modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalTextStyle provides TextStyle.Default.copy(fontSize = 16.sp)) {
        val context = context

        val monitor = CodeforcesMonitorDataStore(context)

        val phase by rememberCollect {
            combine(
                flow = monitor.contestInfo.flow,
                flow2 = monitor.sysTestPercentage.flow
            ) { contestInfo, sysTestPercentage ->
                when (contestInfo.phase) {
                    CodeforcesContestPhase.CODING -> ContestPhase.Coding(contestInfo.startTime + contestInfo.duration)
                    CodeforcesContestPhase.SYSTEM_TEST -> ContestPhase.SystemTesting(sysTestPercentage)
                    else -> ContestPhase.Other(contestInfo.phase)
                }
            }
        }

        val problems by rememberCollect {
            monitor.problemResults.flow
        }

        val lastRequest by rememberCollect {
            monitor.lastRequest.flow
        }

        val rank by rememberCollect {
            monitor.contestantRank.flow
        }

        val participationType by rememberCollect {
            monitor.participationType.flow
        }

        Column(modifier) {
            Title(
                contestPhase = phase,
                requestFailed = lastRequest == false,
                modifier = Modifier.fillMaxWidth()
            )
            StandingsRow(
                problems = problems,
                phase = phase.phase,
                rank = rank,
                participationType = participationType,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

}

private sealed class ContestPhase(val phase: CodeforcesContestPhase) {
    data class Coding(val endTime: Instant): ContestPhase(CodeforcesContestPhase.CODING)
    data class SystemTesting(val percentage: Int?): ContestPhase(CodeforcesContestPhase.SYSTEM_TEST)
    class Other(phase: CodeforcesContestPhase): ContestPhase(phase)
}

@Composable
private fun Title(
    contestPhase: ContestPhase,
    requestFailed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (requestFailed) {
            Box(modifier = Modifier
                .size(width = 16.dp, height = 16.dp)
                .background(color = cpsColors.error)
                .align(Alignment.CenterStart)
            )
        }
        PhaseTitle(
            contestPhase = contestPhase,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun PhaseTitle(
    contestPhase: ContestPhase,
    modifier: Modifier = Modifier
) {
    when (contestPhase) {
        is ContestPhase.Coding -> {
            val currentTime by collectCurrentTimeAsState(period = 1.seconds)
            PhaseTitle(
                phase = contestPhase.phase,
                modifier = modifier,
                info = (contestPhase.endTime - currentTime).coerceAtLeast(Duration.ZERO).let {
                    if (it < 1.hours) it.toMMSS() else it.toHHMMSS()
                }
            )
        }
        is ContestPhase.SystemTesting -> {
            PhaseTitle(
                phase = contestPhase.phase,
                modifier = modifier,
                info = contestPhase.percentage?.let { "%$it" } ?: ""
            )
        }
        else -> {
            PhaseTitle(phase = contestPhase.phase, modifier = modifier)
        }
    }
}

@Composable
private fun PhaseTitle(
    phase: CodeforcesContestPhase,
    modifier: Modifier = Modifier,
    info: String = ""
) {
    Text(
        text = phase.getTitle() + " " + info,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}


@Composable
private fun StandingsRow(
    problems: List<CodeforcesMonitorProblemResult>,
    phase: CodeforcesContestPhase,
    rank: Int,
    participationType: CodeforcesParticipationType,
    modifier: Modifier = Modifier
) {
    if (problems.isNotEmpty()) {
        Row(modifier = modifier) {
            RankColumn(
                rank = rank,
                participationType = participationType
            )
            problems.forEach {
                ProblemColumn(
                    problemResult = it,
                    phase = phase,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProblemColumn(
    problemResult: CodeforcesMonitorProblemResult,
    phase: CodeforcesContestPhase,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = problemResult.problemIndex,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        ProblemPointsCell(
            problemResult = problemResult,
            phase = phase,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ProblemPointsCell(
    problemResult: CodeforcesMonitorProblemResult,
    phase: CodeforcesContestPhase,
    modifier: Modifier = Modifier
) {
    Text(
        text = problemResult.pointsToNiceString(),
        fontWeight = FontWeight.Bold,
        color = if (phase == CodeforcesContestPhase.SYSTEM_TEST || phase == CodeforcesContestPhase.FINISHED) cpsColors.success else cpsColors.content,
        modifier = modifier
    )
}

@Composable
private fun RankColumn(
    rank: Int,
    participationType: CodeforcesParticipationType,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "rank")
        Text(text = if (participationType == CodeforcesParticipationType.CONTESTANT) "$rank" else "*$rank")
    }
}