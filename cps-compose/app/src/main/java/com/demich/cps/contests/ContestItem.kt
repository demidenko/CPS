package com.demich.cps.contests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.format

@Composable
fun ContestItem(
    contest: Contest,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = contest.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            MonospacedText(
                text = contest.startTime.format("dd.MM HH:mm"),
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            MonospacedText(
                text = contest.platform.name,
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}