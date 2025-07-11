package com.demich.cps.contests.list_items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.AttentionIcon
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.colorFor


@Composable
internal fun AttentionText(
    text: String,
    collisionType: DangerType,
    modifier: Modifier = Modifier
) = AttentionWithMark(text, collisionType, modifier)

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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = text)
        if (collisionType != DangerType.SAFE) {
            AttentionIcon(dangerType = collisionType)
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
