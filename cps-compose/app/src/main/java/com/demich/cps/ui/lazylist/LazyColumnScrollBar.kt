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
    color: () -> Color,
    scrollBarWidth: Dp,
    minimumScrollBarHeight: Dp
): Modifier = this.drawWithContent {
    drawContent()
    state.layoutInfo.calculateBarSize(minBarSize = minimumScrollBarHeight.toPx()) { y, h ->
        val w = scrollBarWidth.toPx()
        drawRoundRect(
            color = color(),
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
    // TODO: this read recreates modifier
    val scrollInProgress = state.isScrollInProgress

    val fraction by animateFloatAsState(
        targetValue = if (scrollInProgress) 1f else 0f,
        animationSpec = if (scrollInProgress) enterAnimationSpec else exitAnimationSpec,
        label = "scroll_bar_active"
    )

    drawScrollBar(
        state = state,
        color = { lerp(start = inactiveColor, stop = activeColor, fraction) },
        scrollBarWidth = scrollBarWidth,
        minimumScrollBarHeight = minimumScrollBarHeight
    )
}

private inline fun LazyListLayoutInfo.calculateSizes(
    block: (windowSize: Int, totalSize: Float, windowOffset: Float) -> Unit
) {
    val visibleItems = visibleItemsInfo
    if (visibleItems.isNotEmpty()) {
        val windowSize = viewportEndOffset - viewportStartOffset
        val visibleItemsSize = visibleItems.sumOf { it.size }
        if (windowSize < visibleItemsSize) {
            val itemAvgSize: Float = visibleItemsSize.toFloat() / visibleItems.size
            val totalSize: Float = totalItemsCount * itemAvgSize
            val windowOffset: Float = visibleItems.first().run { index * itemAvgSize - offset }
            block(windowSize, totalSize, windowOffset)
        }
    }
}

private inline fun LazyListLayoutInfo.calculateBarSize(
    minBarSize: Float,
    block: (offset: Float, size: Float) -> Unit
) {
    calculateSizes { windowSize, totalSize, windowOffset ->
        val rawSize = windowSize / totalSize * windowSize
        val size = max(rawSize, minBarSize)
        val offset = if (size == rawSize) windowOffset / totalSize * windowSize
            else windowOffset / (totalSize - windowSize) * (windowSize - size)
        block(offset, size)
    }
}