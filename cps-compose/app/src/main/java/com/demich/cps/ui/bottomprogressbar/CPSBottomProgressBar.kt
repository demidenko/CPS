package com.demich.cps.ui.bottomprogressbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
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
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectAsState
import kotlinx.coroutines.flow.map


@Composable
fun CPSBottomProgressBarsColumn(
    modifier: Modifier = Modifier
) {
    val progressBarsViewModel = progressBarsViewModel()
    val progresses by collectAsState {
        progressBarsViewModel.flowOfProgresses().map { it.entries.toList() }
    }

    //TODO: still shit animation of top item
    LazyColumn(
        modifier = modifier,
        reverseLayout = true
    ) {
        items(
            items = progresses,
            key = { it.key }
        ) {
            CPSBottomProgressBar(
                progressBarInfo = it.value,
                modifier = Modifier
                    .padding(all = 3.dp)
                    .animateItem()
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = cpsColors.backgroundNavigation,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = progressBarInfo.title,
                style = CPSDefaults.MonospaceTextStyle.copy(
                    fontSize = 13.sp,
                    color = cpsColors.contentAdditional,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 10.dp).weight(3f)
            )
            CPSProgressIndicator(
                progressBarInfo = progressBarInfo,
                modifier = Modifier.padding(end = 10.dp).weight(5f)
            )
        }
    }
}

@Composable
fun CPSProgressIndicator(
    progressBarInfo: ProgressBarInfo,
    modifier: Modifier = Modifier
) {
    if (progressBarInfo.total > 0) {
        val progress by animateFloatAsState(targetValue = progressBarInfo.fraction)
        LinearProgressIndicatorRounded(
            progress = progress,
            modifier = modifier
        )
    }
}

@Composable
private fun LinearProgressIndicatorRounded(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = cpsColors.accent,
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