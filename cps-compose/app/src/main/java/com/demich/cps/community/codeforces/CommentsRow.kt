package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.cpsColors
import kotlin.math.roundToInt

@Composable
fun CommentsRow(
    modifier: Modifier = Modifier,
    iconSize: TextUnit,
    spaceSize: Dp,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spaceSize)
    ) {
        CommentsIcon(
            size = iconSize,
            modifier = Modifier
                .alignBy {
                    (it.measuredHeight * 0.77f).roundToInt()
                }
        )
        Box(modifier = Modifier.alignByBaseline()) {
            content()
        }
    }
}

@Composable
private fun CommentsIcon(
    size: TextUnit,
    modifier: Modifier = Modifier
) {
    IconSp(
        imageVector = CPSIcons.Comments,
        size = size,
        color = cpsColors.contentAdditional,
        modifier = modifier
    )
}