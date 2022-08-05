package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.cpsColors

@Composable
fun BlogEntryTitleWithArrow(
    title: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    singleLine: Boolean,
    titleColor: Color = cpsColors.content
) {
    Row(
        modifier = modifier
    ) {
        IconSp(
            imageVector = CPSIcons.ArrowRight,
            size = fontSize,
            color = cpsColors.contentAdditional,
            modifier = Modifier.alignBy {
                it.measuredHeight * 5 / 6
            }
        )
        Text(
            text = title,
            fontSize = fontSize,
            color = titleColor,
            fontWeight = FontWeight.Medium,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = Modifier.alignByBaseline()
        )
    }
}