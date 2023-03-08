package com.demich.cps.contests.monitors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectCurrentTimeAsState
import com.demich.cps.utils.rememberWith
import com.demich.cps.utils.toHHMMSS
import com.demich.cps.utils.toMMSS
import com.demich.cps.data.api.CodeforcesContestPhase
import com.demich.cps.data.api.CodeforcesContestType
import com.demich.cps.data.api.CodeforcesParticipationType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


@Composable
fun CodeforcesMonitorWidget(
    contestData: CodeforcesMonitorData,
    requestFailed: Boolean,
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
                contestData = contestData,
                requestFailed = requestFailed,
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
    contestData: CodeforcesMonitorData,
    requestFailed: Boolean,
    modifier: Modifier
) {
    Column(modifier) {
        Title(
            contestData = contestData,
            requestFailed = requestFailed,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
        )
        StandingsRow(
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
    contestData: CodeforcesMonitorData,
    modifier: Modifier = Modifier
) {
    val textStyle = rememberWith(contestData.problems.size) {
        //TODO: horizontal scroll??
        TextStyle.Default.copy(
            fontSize = when {
                this < 9 -> 16.sp
                this < 10 -> 15.sp
                else -> 14.sp
            }
        )
    }

    ProvideTextStyle(value = textStyle) {
        if (contestData.problems.isNotEmpty()) {
            Row(modifier = modifier) {
                RankColumn(
                    rank = contestData.contestantRank.rank,
                    participationType = contestData.contestantRank.participationType,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                contestData.problems.forEach {
                    ProblemColumn(
                        problemName = it.first,
                        problemResult = it.second,
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
    problemName: String,
    problemResult: CodeforcesMonitorData.ProblemResult,
    contestType: CodeforcesContestType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = problemName)
        ProblemResultCell(
            problemResult = problemResult,
            contestType = contestType
        )
    }
}

@Composable
private fun ProblemResultCell(
    problemResult: CodeforcesMonitorData.ProblemResult,
    contestType: CodeforcesContestType,
    modifier: Modifier = Modifier
) {
    when (problemResult) {
        is CodeforcesMonitorData.ProblemResult.FailedSystemTest -> {
            ProblemResultCell(
                text = CodeforcesMonitorData.ProblemResult.failedSystemTestSymbol,
                color = cpsColors.error,
                modifier = modifier
            )
        }
        is CodeforcesMonitorData.ProblemResult.Pending -> {
            ProblemResultCell(
                text = "?",
                color = cpsColors.contentAdditional,
                modifier = modifier
            )
        }
        is CodeforcesMonitorData.ProblemResult.Empty -> {
            Text(
                text = "",
                modifier = modifier
            )
        }
        is CodeforcesMonitorData.ProblemResult.Points -> {
            ProblemResultCell(
                text = if (contestType == CodeforcesContestType.ICPC) "+" else {
                    problemResult.pointsToNiceString()
                },
                color = if (problemResult.isFinal) cpsColors.success else cpsColors.content,
                modifier = modifier
            )
        }
    }
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

@Preview(showBackground = true)
@Composable
private fun TestProblemColumns() {
    CPSTheme {
        Row(modifier = Modifier.fillMaxWidth()) {
            ProblemColumn(
                problemName = "A",
                problemResult = CodeforcesMonitorData.ProblemResult.Points(1.0, true),
                contestType = CodeforcesContestType.ICPC,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "A1",
                problemResult = CodeforcesMonitorData.ProblemResult.Points(500.0, false),
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "A2",
                problemResult = CodeforcesMonitorData.ProblemResult.Points(500.0, true),
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "F",
                problemResult = CodeforcesMonitorData.ProblemResult.FailedSystemTest,
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "P",
                problemResult = CodeforcesMonitorData.ProblemResult.Pending,
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "E",
                problemResult = CodeforcesMonitorData.ProblemResult.Empty,
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
        }
    }
}