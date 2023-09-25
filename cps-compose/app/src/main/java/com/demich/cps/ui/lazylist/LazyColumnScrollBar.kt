package com.demich.cps.ui.lazylist

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlin.math.max

internal fun Modifier.drawScrollBar(
    state: LazyListState,
    color: Color,
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
            color = color,
            topLeft = Offset(x = size.width - w, y = y),
            size = Size(width = w, height = h),
            cornerRadius = CornerRadius(w / 2)
        )
    }
}

internal fun Modifier.drawScrollBar(
    state: LazyListState,
    activeColor: Color,
    inactiveColor: Color,
    scrollBarWidth: Dp,
    minimumScrollBarHeight: Dp,
    enterAnimationSpec: AnimationSpec<Float>,
    exitAnimationSpec: AnimationSpec<Float>,
): Modifier = composed {
    val scrollInProgress = state.isScrollInProgress

    val fraction by animateFloatAsState(
        targetValue = if (scrollInProgress) 1f else 0f,
        animationSpec = if (scrollInProgress) enterAnimationSpec else exitAnimationSpec,
        label = "scroll_bar_active"
    )

    drawScrollBar(
        state = state,
        color = lerp(start = inactiveColor, stop = activeColor, fraction),
        scrollBarWidth = scrollBarWidth,
        minimumScrollBarHeight = minimumScrollBarHeight
    )
}

private inline fun LazyListLayoutInfo.calculateSizes(
    block: (windowSize: Int, totalSize: Float, windowOffset: Float) -> Unit
) {
    if (visibleItemsInfo.isNotEmpty()) {
        val windowSize = viewportEndOffset - viewportStartOffset
        val visibleItemsSize = visibleItemsInfo.sumOf { it.size }
        if (windowSize < visibleItemsSize) {
            val itemAvgSize: Float = visibleItemsSize.toFloat() / visibleItemsInfo.size
            val totalSize: Float = totalItemsCount * itemAvgSize
            val windowOffset: Float = visibleItemsInfo.first().run { index * itemAvgSize - offset }
            block(windowSize, totalSize, windowOffset)
        }
    }
}