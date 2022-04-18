package com.demich.cps.contests

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun ContestItem(
    contest: Contest,
    //currentTimeSeconds: Long, //Instant is not Stable
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ContestItemHeader(
            contest = contest,
            phase = contest.getPhase(LocalCurrentTimeEachSecond.current),
            modifier = Modifier.fillMaxWidth()
        )
        ContestItemFooter(
            contest = contest,
            currentTimeSeconds = LocalCurrentTimeEachSecond.current.epochSeconds,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ContestItemHeader(
    contest: Contest,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContestPlatformIcon(
            platform = contest.platform,
            modifier = Modifier.padding(end = 4.dp),
            size = 18.sp
        )
        ContestColoredTitle(
            contest = contest,
            phase = phase
        )
    }
}

@Composable
private fun ContestColoredTitle(
    contest: Contest,
    phase: Contest.Phase
) {
    Text(
        text = buildAnnotatedString {
            val (title, brackets) = cutTrailingBrackets(contest.title.trim())
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ContestItemFooter(
    contest: Contest,
    currentTimeSeconds: Long,
    modifier: Modifier = Modifier
) {
    val currentTime = Instant.fromEpochSeconds(currentTimeSeconds)

    val date: String
    val counter: String
    when (contest.getPhase(currentTime)) {
        Contest.Phase.RUNNING -> {
            date = "ends " + contest.endTime.contestDate()
            counter = "left " + contestTimeDifference(currentTime, contest.endTime)
        }
        Contest.Phase.BEFORE -> {
            date = contest.dateRange()
            counter = "in " + contestTimeDifference(currentTime, contest.startTime)
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
                emit(currentTime)
                println(currentTime)
                kotlinx.coroutines.delay(1000 - currentTime.toEpochMilliseconds() % 1000)
            }
        }
    }.collectAsStateLifecycleAware(initial = remember { getCurrentTime() })
}

@Composable
fun ContestPlatformIcon(
    platform: Contest.Platform,
    modifier: Modifier = Modifier,
    size: TextUnit
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
        painter = iconId?.let { painterResource(it) } ?: rememberVectorPainter(Icons.Default.EmojiEvents),
        modifier = modifier.size(with(LocalDensity.current) { size.toDp() }),
        tint = cpsColors.textColorAdditional,
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
    fun Char.isOpenBracket() = this == '(' || this == '（'
    fun Char.isClosedBracket() = this == ')' || this == '）'

    if (title.isEmpty() || !title.last().isClosedBracket()) return title to ""
    var i = title.length-2
    var ballance = 1
    while (ballance > 0 && i > 0) {
        val c = title[i]
        when {
            c.isOpenBracket() -> --ballance
            c.isClosedBracket() -> ++ballance
        }
        if (ballance == 0) break
        --i
    }
    if (ballance != 0) return title to ""
    return title.substring(0, i) to title.substring(i)
}