package com.demich.cps.ui.lazylist

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

internal fun Modifier.drawScrollBar(
    state: LazyListState,
    scrollBarColor: Color,
    scrollBarWidth: Dp,
    minimumScrollBarHeight: Dp = 10.dp
): Modifier = this.drawWithContent {
    drawContent()
    state.layoutInfo.calculateSizes { windowSize, totalSize, windowOffset ->
        val w = scrollBarWidth.toPx()
        val barHeight = windowSize / totalSize * windowSize
        val h = max(barHeight, minimumScrollBarHeight.toPx())
        val y = if (barHeight == h) windowOffset / totalSize * windowSize
        else windowOffset / (totalSize - windowSize) * (windowSize - h)
        drawRoundRect(
            color = scrollBarColor,
            topLeft = Offset(x = size.width - w, y = y),
            size = Size(width = w, height = h),
            cornerRadius = CornerRadius(w / 2)
        )
    }
}

private inline fun LazyListLayoutInfo.calculateSizes(
    block: (windowSize: Int, totalSize: Float, windowOffset: Float) -> Unit
) {
    val countOfVisible = visibleItemsInfo.size
    if (countOfVisible > 0) {
        val windowSize = viewportEndOffset - viewportStartOffset
        val visibleItemsSize = visibleItemsInfo.sumOf { it.size }
        if (windowSize < visibleItemsSize) {
            val itemAvgSize: Float = visibleItemsSize.toFloat() / countOfVisible
            val totalSize: Float = totalItemsCount * itemAvgSize
            val windowOffset: Float = visibleItemsInfo.first().run { index * itemAvgSize - offset }
            block(windowSize, totalSize, windowOffset)
        }
    }
}