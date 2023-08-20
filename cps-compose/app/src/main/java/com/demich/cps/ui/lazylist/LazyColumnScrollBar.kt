package com.demich.cps.ui.lazylist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSDefaults
import kotlin.math.max

fun Modifier.lazyColumnScrollBar(
    state: LazyListState,
    scrollBarColor: Color,
    scrollBarWidth: Dp = CPSDefaults.scrollBarWidth,
    minimumScrollBarHeight: Dp = 10.dp
): Modifier = this.drawWithContent {
    drawContent()
    val info = state.layoutInfo
    val countOfVisible = info.visibleItemsInfo.size
    if (countOfVisible > 0) {
        val windowSize = info.viewportEndOffset - info.viewportStartOffset
        val visibleItemsSize = info.visibleItemsInfo.sumOf { it.size }
        if (windowSize < visibleItemsSize) {
            val itemSize: Float = visibleItemsSize.toFloat() / countOfVisible
            val totalSize: Float = info.totalItemsCount * itemSize
            val beforeSize: Float = state.firstVisibleItemIndex * itemSize - info.visibleItemsInfo.first().offset
            val w = scrollBarWidth.toPx()
            val barHeight = windowSize / totalSize * windowSize
            val h = max(barHeight, minimumScrollBarHeight.toPx())
            val y = if (barHeight == h) beforeSize / totalSize * windowSize
                else beforeSize / (totalSize - windowSize) * (windowSize - h)
            drawRoundRect(
                color = scrollBarColor,
                topLeft = Offset(x = size.width - w, y = y),
                size = Size(width = w, height = h),
                cornerRadius = CornerRadius(w / 2)
            )
        }
    }
}