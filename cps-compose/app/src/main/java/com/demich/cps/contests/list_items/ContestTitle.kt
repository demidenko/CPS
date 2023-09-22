package com.demich.cps.contests.list_items

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append

@Composable
internal fun ContestTitleCollapsed(
    title: String,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.makeTitle(),
        color = colorFor(phase),
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun ContestTitleExpanded(
    title: String,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    var isMultiline by remember(title) { mutableStateOf(false) }
    Text(
        text = title.makeTitle(useNewLine = isMultiline),
        color = colorFor(phase),
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
        textAlign = TextAlign.Center,
        onTextLayout = {
            isMultiline = it.lineCount > 1
        }
    )
}

@Composable
@ReadOnlyComposable
private fun colorFor(phase: Contest.Phase): Color =
    when (phase) {
        Contest.Phase.BEFORE -> cpsColors.content
        Contest.Phase.RUNNING -> cpsColors.success
        Contest.Phase.FINISHED -> cpsColors.contentAdditional
    }


@Composable
@ReadOnlyComposable
private fun String.makeTitle(useNewLine: Boolean = false): AnnotatedString =
    buildAnnotatedString {
        splitTrailingBrackets { title, brackets ->
            append(title)
            if (brackets.isNotBlank()) {
                if (useNewLine) append('\n')
                append(brackets, color = cpsColors.contentAdditional)
            }
        }
    }

private fun trailingBracketsStart(title: String): Int {
    if (title.isEmpty() || title.last() != ')') return title.length
    var i = title.length - 2
    var ballance = 1
    while (ballance > 0 && i > 0) {
        when (title[i]) {
            '(' -> --ballance
            ')' -> ++ballance
        }
        if (ballance == 0) return i
        --i
    }
    return title.length
}

private inline fun String.splitTrailingBrackets(block: (String, String) -> Unit) {
    val i = trailingBracketsStart(this)
    block(substring(0, i), substring(i))
}