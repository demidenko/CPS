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
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier
            .lazyColumnScrollBar(
                listState = state,
                scrollBarColor = cpsColors.content.copy(alpha = 0.5f),
                scrollBarWidth = 5.dp
            ),
        state = state,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

fun Modifier.lazyColumnScrollBar(
    listState: LazyListState,
    scrollBarColor: Color,
    scrollBarWidth: Dp,
    minimumScrollBarHeight: Dp = 10.dp
): Modifier = this.drawWithContent {
    drawContent()
    val info = listState.layoutInfo
    val count = info.visibleItemsInfo.size
    if (count > 0) {
        val windowSize = info.viewportEndOffset - info.viewportStartOffset
        val visibleItemsSize = info.visibleItemsInfo.sumOf { it.size }
        if (windowSize < visibleItemsSize) {
            val itemSize: Float = visibleItemsSize.toFloat() / count
            val totalSize: Float = info.totalItemsCount * itemSize
            val beforeSize: Float = listState.firstVisibleItemIndex * itemSize - info.visibleItemsInfo.first().offset
            val h = windowSize / totalSize * windowSize //real height of bar
            val scrollBarHeight = max(h, minimumScrollBarHeight.toPx())
            val offsetY = beforeSize / totalSize * windowSize - beforeSize / (totalSize - windowSize) * (scrollBarHeight - h)
            drawRoundRect(
                color = scrollBarColor,
                topLeft = Offset(x = size.width - scrollBarWidth.toPx(), y = offsetY),
                size = Size(width = scrollBarWidth.toPx(), height = scrollBarHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}