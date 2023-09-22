package com.demich.cps.contests.list_items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType


@Composable
internal fun AttentionText(
    text: String,
    collisionType: DangerType,
    modifier: Modifier = Modifier
) = AttentionWithMark(text, collisionType, modifier)

@Composable
@ReadOnlyComposable
private fun colorFor(collisionType: DangerType): Color =
    when (collisionType) {
        DangerType.SAFE -> Color.Unspecified
        DangerType.WARNING -> cpsColors.warning
        DangerType.DANGER -> cpsColors.error
    }

@Composable
private fun AttentionHighlighted(
    text: String,
    collisionType: DangerType,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = colorFor(collisionType)
    )
}

@Composable
private fun AttentionWithMark(
    text: String,
    collisionType: DangerType,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = text)
        if (collisionType != DangerType.SAFE) {
            IconSp(
                imageVector = CPSIcons.Attention,
                color = colorFor(collisionType),
                size = 14.sp,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}

@Composable
private fun AttentionBoxed(
    text: String,
    collisionType: DangerType,
    modifier: Modifier = Modifier
) {
    if (collisionType == DangerType.SAFE) {
        Text(
            text = text,
            modifier = modifier,
        )
    } else {
        Text(
            text = text,
            color = cpsColors.background,
            modifier = modifier
                .background(color = colorFor(collisionType))
                .border(color = cpsColors.background, width = 0.dp),
        )
    }
}
