package com.demich.cps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import kotlin.math.max

@Composable
fun LazyColumnWithScrollBar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    enableScrollBar: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier
            .let {
                if (enableScrollBar) it.lazyColumnScrollBar(
                    state = state,
                    scrollBarColor = cpsColors.content.copy(alpha = 0.5f)
                ) else it
            },
        state = state,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

fun Modifier.lazyColumnScrollBar(
    state: LazyListState,
    scrollBarColor: Color,
    scrollBarWidth: Dp = CPSDefaults.scrollBarWidth,
    minimumScrollBarHeight: Dp = 10.dp
): Modifier = this.drawWithContent {
    drawContent()
    val info = state.layoutInfo
    val countVisible = info.visibleItemsInfo.size
    if (countVisible > 0) {
        val windowSize = info.viewportEndOffset - info.viewportStartOffset
        val visibleItemsSize = info.visibleItemsInfo.sumOf { it.size }
        if (windowSize < visibleItemsSize) {
            val itemSize: Float = visibleItemsSize.toFloat() / countVisible
            val totalSize: Float = info.totalItemsCount * itemSize
            val beforeSize: Float = state.firstVisibleItemIndex * itemSize - info.visibleItemsInfo.first().offset
            val realBarHeight = windowSize / totalSize * windowSize
            val h = max(realBarHeight, minimumScrollBarHeight.toPx())
            val barOffset = beforeSize / totalSize * windowSize - beforeSize / (totalSize - windowSize) * (h - realBarHeight)
            val w = scrollBarWidth.toPx()
            drawRoundRect(
                color = scrollBarColor,
                topLeft = Offset(x = size.width - w, y = barOffset),
                size = Size(width = w, height = h),
                cornerRadius = CornerRadius(w / 2)
            )
        }
    }
}