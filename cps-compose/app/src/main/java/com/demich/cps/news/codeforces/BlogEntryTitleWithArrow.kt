package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.cpsColors

@Composable
fun BlogEntryTitleWithArrow(
    title: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    singleLine: Boolean
) {
    Row(
        modifier = modifier
    ) {
        IconSp(
            imageVector = Icons.Default.ArrowRightAlt,
            size = fontSize,
            color = cpsColors.contentAdditional,
            modifier = Modifier.alignBy {
                it.measuredHeight * 5 / 6
            }
        )
        Text(
            text = title,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = Modifier.alignByBaseline()
        )
    }
}