package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.cpsColors

@Composable
fun CommentsRow(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    fontSize: TextUnit,
    iconSize: TextUnit,
    spaceSize: Dp
) {
    Row(
        modifier = modifier
    ) {
        IconSp(
            imageVector = CPSIcons.Comments,
            size = iconSize,
            color = cpsColors.contentAdditional,
            modifier = Modifier
                .padding(end = spaceSize)
                .alignBy {
                    (it.measuredHeight * 0.77f).toInt()
                }
        )
        Text(
            text = text,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alignByBaseline()
        )
    }
}