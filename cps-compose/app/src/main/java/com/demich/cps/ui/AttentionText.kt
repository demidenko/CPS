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
import com.demich.cps.utils.SafetyLevel
import com.demich.cps.utils.colorFor

@Composable
internal fun AttentionText(
    text: String,
    safetyLevel: SafetyLevel,
    modifier: Modifier = Modifier
) = AttentionWithMark(text, safetyLevel, modifier)

@Composable
private fun AttentionHighlighted(
    text: String,
    safetyLevel: SafetyLevel,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = cpsColors.colorFor(safetyLevel)
    )
}

@Composable
private fun AttentionWithMark(
    text: String,
    safetyLevel: SafetyLevel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = text)
        if (safetyLevel != SAFE) {
            AttentionIcon(safetyLevel = safetyLevel)
        }
    }
}

@Composable
private fun AttentionBoxed(
    text: String,
    safetyLevel: SafetyLevel,
    modifier: Modifier = Modifier
) {
    if (safetyLevel == SAFE) {
        Text(
            text = text,
            modifier = modifier,
        )
    } else {
        Text(
            text = text,
            color = cpsColors.background,
            modifier = modifier
                .background(color = cpsColors.colorFor(safetyLevel))
                .border(color = cpsColors.background, width = 0.dp),
        )
    }
}