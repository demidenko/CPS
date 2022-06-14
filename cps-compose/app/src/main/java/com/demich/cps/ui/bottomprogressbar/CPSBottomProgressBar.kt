package com.demich.cps.ui.bottomprogressbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
        items(
            items = progressBarsViewModel.progressBars,
            key = { it }
        ) {
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
    if (progressBarInfo.total > 0) {
        val progress by animateFloatAsState(targetValue = progressBarInfo.fraction)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = cpsColors.backgroundNavigation.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = 4.dp)
        ) {
            MonospacedText(
                text = progressBarInfo.title,
                fontSize = 13.sp,
                color = cpsColors.contentAdditional,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp).weight(3f)
            )
            LinearProgressIndicatorRounded(
                progress = progress,
                modifier = Modifier.padding(end = 10.dp).weight(5f)
            )
        }
    }
}

@Composable
fun LinearProgressIndicatorRounded(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    backgroundColor: Color = color.copy(alpha = ProgressIndicatorDefaults.IndicatorBackgroundOpacity)
) {
    Canvas(
        modifier = modifier
            .height(4.dp)
    ) {
        drawRoundRect(
            color = backgroundColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(size.height)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(height = size.height, width = progress.coerceIn(0f, 1f) * size.width),
            cornerRadius = CornerRadius(size.height)
        )
    }
}