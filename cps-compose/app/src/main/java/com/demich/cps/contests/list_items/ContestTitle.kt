package com.demich.cps.contests.list_items

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append
import com.demich.kotlin_stdlib_boost.splitTrailingBrackets

@Composable
internal fun ContestTitleCollapsed(
    title: String,
    phase: Contest.Phase,
    isVirtual: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.makeTitle(),
        color = colorFor(phase, isVirtual),
        fontSize = CPSFontSize.itemTitle,
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
    isVirtual: Boolean,
    modifier: Modifier = Modifier
) {
    // TODO: find better solution
    var isMultiline by rememberSaveable(title) { mutableStateOf(false) }
    Text(
        text = title.makeTitle(useNewLine = isMultiline),
        color = colorFor(phase, isVirtual),
        fontSize = CPSFontSize.itemTitle,
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
private fun colorFor(phase: Contest.Phase, isVirtual: Boolean): Color =
    if (phase == Contest.Phase.RUNNING && !isVirtual) cpsColors.success
    else cpsColors.content


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

