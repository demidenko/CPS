package com.demich.cps.ui.lazylist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlin.math.max

internal fun Modifier.drawScrollBar(
    state: LazyListState,
    scrollBarColor: Color,
    scrollBarWidth: Dp,
    minimumScrollBarHeight: Dp
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

internal fun Modifier.drawScrollBar(
    state: LazyListState,
    scrollBarColor: Color,
    scrollBarActiveAlpha: Float,
    scrollBarInactiveAlpha: Float,
    scrollBarWidth: Dp,
    minimumScrollBarHeight: Dp
): Modifier = composed {
    val scrollInProgress by remember(state) {
        derivedStateOf { state.isScrollInProgress }
    }

    val alpha by animateFloatAsState(
        targetValue = if (scrollInProgress) scrollBarActiveAlpha else scrollBarInactiveAlpha,
        animationSpec = if (scrollInProgress) snap() else tween(delayMillis = 1000),
        label = "scroll_bar_alpha"
    )

    drawScrollBar(
        state = state,
        scrollBarColor = scrollBarColor.copy(alpha = alpha),
        scrollBarWidth = scrollBarWidth,
        minimumScrollBarHeight = minimumScrollBarHeight
    )
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