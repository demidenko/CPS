package com.demich.cps.contests

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.R
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.ui.CPSDropdownMenu
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun ContestItem(
    contest: Contest,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (!expanded) ContestItemContent(contest = contest)
        else ContestExpandedItemContent(contest = contest)
    }
}

@Composable
private fun ContestItemContent(contest: Contest) {
    val currentTime = LocalCurrentTimeEachSecond.current
    ContestItemHeader(
        platform = contest.platform,
        contestTitle = contest.title,
        phase = contest.getPhase(currentTime),
        modifier = Modifier.fillMaxWidth()
    )
    ContestItemFooter(
        contest = contest,
        currentTime = currentTime,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ContestItemHeader(
    platform: Contest.Platform?,
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
            color = cpsColors.textColorAdditional
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
            if (brackets.isNotBlank()) withStyle(SpanStyle(color = cpsColors.textColorAdditional)) {
                append(brackets)
            }
        },
        color = when (phase) {
            Contest.Phase.BEFORE -> cpsColors.textColor
            Contest.Phase.RUNNING -> cpsColors.success
            Contest.Phase.FINISHED -> cpsColors.textColorAdditional
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
    contest: Contest,
    currentTime: Instant,
    modifier: Modifier = Modifier
) {
    val date: String
    val counter: String
    when (contest.getPhase(currentTime)) {
        Contest.Phase.BEFORE -> {
            date = contest.dateRange()
            counter = "in " + contestTimeDifference(currentTime, contest.startTime)
        }
        Contest.Phase.RUNNING -> {
            date = "ends " + contest.endTime.contestDate()
            counter = "left " + contestTimeDifference(currentTime, contest.endTime)
        }
        Contest.Phase.FINISHED -> {
            date = contest.startTime.contestDate() + " - " + contest.endTime.contestDate()
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
            color = cpsColors.textColorAdditional,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        MonospacedText(
            text = counter,
            fontSize = 15.sp,
            color = cpsColors.textColorAdditional,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}



@Composable
private fun ContestExpandedItemContent(contest: Contest) {
    val currentTime = LocalCurrentTimeEachSecond.current
    ContestExpandedItemHeader(
        platform = contest.platform,
        contestTitle = contest.title,
        phase = contest.getPhase(currentTime),
        modifier = Modifier.fillMaxWidth()
    )
    ContestExpandedItemFooter(
        contest = contest,
        currentTime = currentTime,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ContestExpandedItemHeader(
    platform: Contest.Platform?,
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
            color = if (phase == Contest.Phase.FINISHED) cpsColors.textColorAdditional else cpsColors.textColor
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
    contest: Contest,
    currentTime: Instant,
    modifier: Modifier = Modifier
) {
    ContestExpandedItemFooter(
        contest = contest,
        counter = when (contest.getPhase(currentTime)) {
            Contest.Phase.BEFORE -> "starts in " + contestTimeDifference(currentTime, contest.startTime)
            Contest.Phase.RUNNING -> "ends in " + contestTimeDifference(currentTime, contest.endTime)
            Contest.Phase.FINISHED -> ""
        },
        modifier = modifier
    )
}

@Composable
private fun ContestExpandedItemFooter(
    contest: Contest,
    counter: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        ContestExpandedItemDatesAndMenuButton(
            contest = contest,
            modifier = Modifier.fillMaxWidth()
        )
        if (counter.isNotBlank()) {
            MonospacedText(
                text = counter,
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional
            )
        }
    }
}

@Composable
private fun ContestExpandedItemDatesAndMenuButton(
    contest: Contest,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            MonospacedText(
                text = contest.startTime.contestDate(),
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional
            )
            MonospacedText(
                text = contest.endTime.contestDate(),
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional
            )
        }
        ContestItemMenuButton(
            contest = contest,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun ContestItemMenuButton(
    contest: Contest,
    modifier: Modifier = Modifier
) {
    val context = context
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        CPSIconButton(
            icon = CPSIcons.More,
            color = cpsColors.textColorAdditional,
            onClick = { showMenu = true }
        )
        CPSDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (contest.link != null) {
                CPSDropdownMenuItem(title = "Open in browser", icon = CPSIcons.OpenInBrowser) {
                    context.openUrlInBrowser(contest.link)
                }
            }
            CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete) {
                showDeleteDialog = true
            }
        }
    }
    if (showDeleteDialog) {
        ContestDeleteDialog(
            onDismissRequest = { showDeleteDialog = false },
            onDeleteRequest = {
                scope.launch {
                    context.settingsContests.ignoredContests.add(contest.compositeId)
                }
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun ContestDeleteDialog(
    onDeleteRequest: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                content = { Text(text = "Delete", color = cpsColors.errorColor) },
                onClick = onDeleteRequest
            )
        },
        dismissButton = {
            TextButton(
                content = { Text("Cancel") },
                onClick = onDismissRequest
            )
        },
        title = { Text("Delete contest from list?") },
        backgroundColor = cpsColors.background
    )
}

private fun contestTimeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    if(t < 24.hours * 2) return t.toHHMMSS()
    return timeDifference(fromTime, toTime)
}


val LocalCurrentTimeEachSecond = compositionLocalOf { getCurrentTime() }

@Composable
fun collectCurrentTime(): State<Instant> {
    return remember {
        flow {
            while (currentCoroutineContext().isActive) {
                val currentTime = getCurrentTime()
                emit(Instant.fromEpochSeconds(currentTime.epochSeconds))
                println(currentTime)
                val rem = currentTime.toEpochMilliseconds() % 1000
                delay(timeMillis = if (rem == 0L) 1000 else 1000 - rem)
            }
        }
    }.collectAsStateLifecycleAware(initial = remember { getCurrentTime() })
}

@Composable
fun ContestPlatformIcon(
    platform: Contest.Platform?,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color
) {
    val iconId = when (platform) {
        Contest.Platform.codeforces -> R.drawable.ic_logo_codeforces
        Contest.Platform.atcoder -> R.drawable.ic_logo_atcoder
        Contest.Platform.topcoder -> R.drawable.ic_logo_topcoder
        Contest.Platform.codechef -> R.drawable.ic_logo_codechef
        Contest.Platform.google -> R.drawable.ic_logo_google
        Contest.Platform.dmoj -> R.drawable.ic_logo_dmoj
        else -> null
    }

    Icon(
        painter = iconId?.let { painterResource(it) } ?: rememberVectorPainter(CPSIcons.Contest),
        modifier = modifier.size(with(LocalDensity.current) { size.toDp() }),
        tint = color,
        contentDescription = null
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