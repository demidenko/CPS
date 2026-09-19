package com.demich.cps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.WarningLevel
import com.demich.cps.utils.colorFor

@Composable
internal fun AttentionText(
    text: String,
    warningLevel: WarningLevel,
    modifier: Modifier = Modifier
) = AttentionWithMark(text, warningLevel, modifier)

@Composable
private fun AttentionHighlighted(
    text: String,
    warningLevel: WarningLevel,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = cpsColors.colorFor(warningLevel)
    )
}

@Composable
private fun AttentionWithMark(
    text: String,
    warningLevel: WarningLevel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = text)
        if (warningLevel != SAFE) {
            AttentionIcon(warningLevel = warningLevel)
        }
    }
}

@Composable
private fun AttentionBoxed(
    text: String,
    warningLevel: WarningLevel,
    modifier: Modifier = Modifier
) {
    if (warningLevel == SAFE) {
        Text(
            text = text,
            modifier = modifier,
        )
    } else {
        Text(
            text = text,
            color = cpsColors.background,
            modifier = modifier
                .background(color = cpsColors.colorFor(warningLevel))
                .border(color = cpsColors.background, width = 0.dp),
        )
    }
}