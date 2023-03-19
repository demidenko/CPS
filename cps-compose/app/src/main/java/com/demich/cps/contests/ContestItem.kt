package com.demich.cps.contests

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.*
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun ContestItem(
    contest: Contest,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Column(modifier = modifier) {
        if (!expanded) ContestItemContent(contest = contest)
        else ContestExpandedItemContent(contest = contest, onDeleteRequest = onDeleteRequest)
    }
}

@Immutable
private data class ContestData(
    val contest: Contest,
    val phase: Contest.Phase,
    val startsIn: String,
    val endsIn: String
) {
    constructor(contest: Contest, currentTime: Instant): this(
        contest = contest,
        phase = contest.getPhase(currentTime),
        startsIn = contestTimeDifference(currentTime, contest.startTime),
        endsIn = contestTimeDifference(currentTime, contest.endTime)
    )
}

@Composable
private fun ContestItemContent(contest: Contest) {
    //TODO: recompose twice per second! (wtf?)
    val data = ContestData(
        contest = contest,
        currentTime = LocalCurrentTime.current
    )
    ContestItemHeader(
        platform = contest.platform,
        contestTitle = contest.title,
        phase = data.phase,
        modifier = Modifier.fillMaxWidth()
    )
    ContestItemFooter(
        data = data,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ContestItemHeader(
    platform: Contest.Platform,
    contestTitle: String,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContestPlatformIcon(
            platform = platform,
            modifier = Modifier.padding(end = 4.dp),
            size = 18.sp,
            color = cpsColors.contentAdditional
        )
        ContestColoredTitle(
            contestTitle = contestTitle,
            phase = phase,
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ContestColoredTitle(
    contestTitle: String,
    phase: Contest.Phase,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            val (title, brackets) = cutTrailingBrackets(contestTitle.trim())
            append(title)
            if (brackets.isNotBlank()) append(brackets, color = cpsColors.contentAdditional)
        },
        color = when (phase) {
            Contest.Phase.BEFORE -> cpsColors.content
            Contest.Phase.RUNNING -> cpsColors.success
            Contest.Phase.FINISHED -> cpsColors.contentAdditional
        },
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier
    )
}

@Composable
private fun ContestItemFooter(
    data: ContestData,
    modifier: Modifier = Modifier
) {
    val date: String
    val counter: String
    when (data.phase) {
        Contest.Phase.BEFORE -> {
            date = data.contest.dateRange()
            counter = "in " + data.startsIn
        }
        Contest.Phase.RUNNING -> {
            date = "ends " + data.contest.endTime.contestDate()
            counter = "left " + data.endsIn
        }
        Contest.Phase.FINISHED -> {
            date = data.contest.startTime.contestDate() + " - " + data.contest.endTime.contestDate()
            counter = ""
        }
    }

    ContestItemFooter(
        date = date,
        counter = counter,
        modifier = modifier
    )
}

@Composable
private fun ContestItemFooter(
    date: String,
    counter: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        MonospacedText(
            text = date,
            fontSize = 15.sp,
            color = cpsColors.contentAdditional,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        MonospacedText(
            text = counter,
            fontSize = 15.sp,
            color = cpsColors.contentAdditional,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}



@Composable
private fun ContestExpandedItemContent(
    contest: Contest,
    onDeleteRequest: () -> Unit
) {
    val data = ContestData(
        contest = contest,
        currentTime = LocalCurrentTime.current
    )
    ContestExpandedItemHeader(
        platform = contest.platform,
        contestTitle = contest.title,
        phase = data.phase,
        modifier = Modifier.fillMaxWidth()
    )
    ContestExpandedItemFooter(
        data = data,
        modifier = Modifier.fillMaxWidth(),
        onDeleteRequest = onDeleteRequest
    )
}

@Composable
private fun ContestExpandedItemHeader(
    platform: Contest.Platform,
    contestTitle: String,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        ContestPlatformIcon(
            platform = platform,
            modifier = Modifier
                .padding(end = 4.dp)
                .padding(all = 5.dp)
                .align(Alignment.Top),
            size = 30.sp,
            color = if (phase == Contest.Phase.FINISHED) cpsColors.contentAdditional else cpsColors.content
        )
        ContestColoredTitle(
            contestTitle = contestTitle,
            phase = phase,
            singleLine = false,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun ContestExpandedItemFooter(
    data: ContestData,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    ContestExpandedItemFooter(
        startTime = data.contest.startTime.contestDate(),
        endTime = data.contest.endTime.contestDate(),
        contestLink = data.contest.link,
        counter = when (data.phase) {
            Contest.Phase.BEFORE -> {
                "starts in " + data.startsIn
            }
            Contest.Phase.RUNNING -> {
                "ends in " + data.endsIn
            }
            Contest.Phase.FINISHED -> ""
        },
        modifier = modifier,
        onDeleteRequest = onDeleteRequest
    )
}

@Composable
private fun ContestExpandedItemFooter(
    startTime: String,
    endTime: String,
    contestLink: String?,
    counter: String,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        ContestExpandedItemDatesAndMenuButton(
            startTime = startTime,
            endTime = endTime,
            contestLink = contestLink,
            modifier = Modifier.fillMaxWidth(),
            onDeleteRequest = onDeleteRequest
        )
        if (counter.isNotBlank()) {
            MonospacedText(
                text = counter,
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
            )
        }
    }
}

@Composable
private fun ContestExpandedItemDatesAndMenuButton(
    startTime: String,
    endTime: String,
    contestLink: String?,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            MonospacedText(
                text = startTime,
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
            )
            MonospacedText(
                text = endTime,
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
            )
        }
        ContestItemMenuButton(
            contestLink = contestLink,
            modifier = Modifier.align(Alignment.CenterEnd),
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
private fun ContestItemMenuButton(
    contestLink: String?,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    val context = context
    var showDeleteDialog by remember { mutableStateOf(false) }
    CPSDropdownMenuButton(
        icon = CPSIcons.More,
        color = cpsColors.contentAdditional,
        modifier = modifier
    ) {
        if (contestLink != null) {
            CPSDropdownMenuItem(title = "Open in browser", icon = CPSIcons.OpenInBrowser) {
                context.openUrlInBrowser(contestLink)
            }
        }
        CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete) {
            showDeleteDialog = true
        }
    }
    if (showDeleteDialog) {
        CPSDeleteDialog(
            title = "Delete contest from list?",
            onDismissRequest = { showDeleteDialog = false },
            onConfirmRequest = onDeleteRequest
        )
    }
}

private fun contestTimeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    if(t < 24.hours * 2) return t.toHHMMSS()
    return timeDifference(fromTime, toTime)
}


@Composable
fun ContestPlatformIcon(
    platform: Contest.Platform,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color
) {
    IconSp(
        painter = platformIconPainter(platform),
        size = size,
        modifier = modifier,
        color = color
    )
}

private fun Instant.contestDate() = format("dd.MM E HH:mm")

private fun Contest.dateRange(): String {
    val start = startTime.contestDate()
    val end = if (duration < 1.days) endTime.format("HH:mm") else "..."
    return "$start-$end"
}


private fun cutTrailingBrackets(title: String): Pair<String, String> {
    if (title.isEmpty() || title.last() != ')') return title to ""
    var i = title.length-2
    var ballance = 1
    while (ballance > 0 && i > 0) {
        when(title[i]) {
            '(' -> --ballance
            ')' -> ++ballance
        }
        if (ballance == 0) break
        --i
    }
    if (ballance != 0) return title to ""
    return title.substring(0, i) to title.substring(i)
}