package com.demich.cps.contests.monitors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.*
import com.demich.datastore_itemized.flowBy
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


@Composable
fun CodeforcesMonitorWidget(
    modifier: Modifier = Modifier,
    onOpenInBrowser: () -> Unit,
    onStop: () -> Unit
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    ContentWithCPSDropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        content = {
            CodeforcesMonitor(
                modifier = modifier
                    .clip(shape = RoundedCornerShape(6.dp))
                    .clickable { showMenu = true }
                    .background(color = cpsColors.backgroundAdditional)
                    .padding(vertical = 8.dp)
            )
        }
    ) {
        CPSDropdownMenuItem(title = "Browse", icon = CPSIcons.OpenInBrowser, onClick = onOpenInBrowser)
        CPSDropdownMenuItem(title = "Close", icon = CPSIcons.Close, onClick = onStop)
    }
}



@Composable
private fun CodeforcesMonitor(
    modifier: Modifier
) {
    val context = context

    val monitor = CodeforcesMonitorDataStore(context)

    val contestData by rememberCollect {
        monitor.flowBy { prefs ->
            val contest = prefs[contestInfo]
            val phase = when (contest.phase) {
                CodeforcesContestPhase.CODING -> CodeforcesMonitorData.ContestPhase.Coding(contest.startTime + contest.duration)
                CodeforcesContestPhase.SYSTEM_TEST -> CodeforcesMonitorData.ContestPhase.SystemTesting(prefs[sysTestPercentage])
                else -> CodeforcesMonitorData.ContestPhase.Other(contest.phase)
            }
            val contestantRank = CodeforcesMonitorData.ContestRank(
                rank = prefs[contestantRank],
                participationType = prefs[participationType]
            )
            CodeforcesMonitorData(
                contestInfo = contest,
                contestPhase = phase,
                contestantRank = contestantRank
            )
        }
    }

    val problems by rememberCollect {
        monitor.problemResults.flow
    }

    val problemsFailedSysTest by rememberCollect {
        monitor.submissionsInfo.flow.map { info ->
            info.mapNotNull { (index, list) ->
                if (list.any { it.testset == CodeforcesTestset.TESTS && it.verdict.isResult() && it.verdict != CodeforcesProblemVerdict.OK }) {
                    index
                } else {
                    null
                }
            }.toSet()
        }
    }

    val lastRequest by rememberCollect { monitor.lastRequest.flow }

    Column(modifier) {
        Title(
            contestData = contestData,
            requestFailed = lastRequest == false,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
        )
        StandingsRow(
            problems = problems,
            problemsFailedSysTest = problemsFailedSysTest,
            contestData = contestData,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Title(
    contestData: CodeforcesMonitorData,
    requestFailed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (requestFailed) {
            IconSp(
                imageVector = CPSIcons.Error,
                size = 16.sp,
                color = cpsColors.error,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        PhaseTitle(
            contestPhase = contestData.contestPhase,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun PhaseTitle(
    contestPhase: CodeforcesMonitorData.ContestPhase,
    modifier: Modifier = Modifier
) {
    when (contestPhase) {
        is CodeforcesMonitorData.ContestPhase.Coding -> {
            val currentTime by collectCurrentTimeAsState(period = 1.seconds)
            PhaseTitle(
                phase = contestPhase.phase,
                modifier = modifier,
                info = (contestPhase.endTime - currentTime).coerceAtLeast(Duration.ZERO).let {
                    if (it < 1.hours) it.toMMSS() else it.toHHMMSS()
                }
            )
        }
        is CodeforcesMonitorData.ContestPhase.SystemTesting -> {
            PhaseTitle(
                phase = contestPhase.phase,
                modifier = modifier,
                info = contestPhase.percentage?.let { "$it%" } ?: ""
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
        text = phase.title + " " + info,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}


@Composable
private fun StandingsRow(
    problems: List<CodeforcesMonitorProblemResult>,
    problemsFailedSysTest: Set<String>,
    contestData: CodeforcesMonitorData,
    modifier: Modifier = Modifier
) {
    val textStyle = rememberWith(problems.size) {
        //TODO: horizontal scroll??
        TextStyle.Default.copy(
            fontSize = when {
                this < 9 -> 16.sp
                this < 12 -> 15.sp
                else -> 14.sp
            }
        )
    }
    if (problems.isNotEmpty()) {
        CompositionLocalProvider(LocalTextStyle provides textStyle) {
            Row(modifier = modifier) {
                RankColumn(
                    rank = contestData.contestantRank.rank,
                    participationType = contestData.contestantRank.participationType,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                problems.forEach {
                    ProblemColumn(
                        problemResult = it,
                        isFailedSysTest = it.problemIndex in problemsFailedSysTest,
                        phase = contestData.contestPhase.phase,
                        contestType = contestData.contestInfo.type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProblemColumn(
    problemResult: CodeforcesMonitorProblemResult,
    isFailedSysTest: Boolean,
    phase: CodeforcesContestPhase,
    contestType: CodeforcesContestType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = problemResult.problemIndex)
        if (isFailedSysTest) {
            ProblemFailedCell(iconSize = 18.sp)
            /*ProblemResultCell(
                text = "✖",
                color = cpsColors.error
            )*/
        } else {
            ProblemResultCell(
                problemResult = problemResult,
                phase = phase,
                contestType = contestType
            )
        }
    }
}

@Composable
private fun ProblemResultCell(
    problemResult: CodeforcesMonitorProblemResult,
    phase: CodeforcesContestPhase,
    contestType: CodeforcesContestType,
    modifier: Modifier = Modifier
) {
    if (contestType == CodeforcesContestType.ICPC) {
        ProblemResultCell(
            text = if (problemResult.points == 0.0) "" else "+",
            color = cpsColors.success,
            modifier = modifier
        )
    } else {
        if (phase.isSystemTestOrFinished() && problemResult.type == CodeforcesProblemStatus.PRELIMINARY) {
            ProblemResultCell(
                text = "?",
                color = cpsColors.contentAdditional,
                modifier = modifier
            )
        } else {
            ProblemPointsCell(
                points = problemResult.points,
                phase = phase,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ProblemPointsCell(
    points: Double,
    phase: CodeforcesContestPhase,
    modifier: Modifier = Modifier
) {
    ProblemResultCell(
        text = if (points != 0.0) points.toString().removeSuffix(".0") else "",
        color = if (phase.isSystemTestOrFinished()) cpsColors.success else cpsColors.content,
        modifier = modifier
    )
}

@Composable
private fun ProblemResultCell(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun ProblemFailedCell(
    iconSize: TextUnit,
    modifier: Modifier = Modifier
) {
    IconSp(
        imageVector = CPSIcons.FailedSystemTest,
        color = cpsColors.error,
        size = iconSize,
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
        Text(text = when {
                rank <= 0 -> ""
                participationType == CodeforcesParticipationType.CONTESTANT -> "$rank"
                else -> "*$rank"
            }
        )
    }
}