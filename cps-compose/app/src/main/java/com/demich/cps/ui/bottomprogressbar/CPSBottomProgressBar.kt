package com.demich.cps.ui.bottomprogressbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors


@Composable
fun CPSBottomProgressBarsColumn(
    progressBarsViewModel: ProgressBarsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        this.items(items = progressBarsViewModel.progressBars, key = { it }) {
            val progress by progressBarsViewModel.collectProgress(id = it)
            CPSBottomProgressBar(
                progressBarInfo = progress,
                modifier = Modifier.padding(all = 3.dp)
            )
        }
    }
}

@Composable
fun CPSBottomProgressBar(
    progressBarInfo: ProgressBarInfo,
    modifier: Modifier = Modifier
) {
    if (progressBarInfo.total > 0) Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cpsColors.backgroundNavigation.copy(alpha = 0.5f),
        elevation = 5.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            MonospacedText(
                text = progressBarInfo.title,
                fontSize = 13.sp,
                color = cpsColors.textColorAdditional,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(3f)
            )
            val progress by animateFloatAsState(targetValue = progressBarInfo.fraction)
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .weight(5f)
                    .padding(start = 5.dp)
            )
        }
    }
}