package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import kotlin.math.roundToInt

@Composable
fun CommentsRow(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    fontSize: TextUnit,
    iconSize: TextUnit,
    spaceSize: Dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spaceSize)
    ) {
        IconSp(
            imageVector = CPSIcons.Comments,
            size = iconSize,
            color = cpsColors.contentAdditional,
            modifier = Modifier
                .alignBy {
                    (it.measuredHeight * 0.77f).roundToInt()
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