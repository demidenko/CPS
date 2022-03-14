package com.demich.cps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import kotlin.math.max

@Composable
fun LazyColumnWithScrollBar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollBarWidth: Dp = 5.dp,
    content: LazyListScope.() -> Unit
) {
    Box(
        modifier = modifier
    ) {
        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
        VerticalScrollBar(
            listState = state,
            modifier = Modifier.align(Alignment.CenterEnd).width(scrollBarWidth)
        )
    }
}

@Composable
fun VerticalScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    scrollBarColor: Color = cpsColors.textColor.copy(alpha = 0.5f)
) {
    //TODO: window min height
    Column(
        modifier = modifier
            .background(color = Color.Transparent)
    ) {
        val count = listState.layoutInfo.visibleItemsInfo.size
        if (count > 0) {
            val windowWidth: Float = (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset).toFloat()
            val itemWidth: Float = listState.layoutInfo.visibleItemsInfo.sumOf { it.size } / count.toFloat()
            val totalWidth: Float = listState.layoutInfo.totalItemsCount * itemWidth
            val before: Float = listState.firstVisibleItemIndex * itemWidth - listState.layoutInfo.visibleItemsInfo.first().offset
            val after: Float = max(0f, totalWidth - windowWidth - before)
            if (before > 0) Box(modifier = Modifier.fillMaxWidth().weight(before))
            if (before > 0 || after > 0) {
                Box(modifier = Modifier
                    .background(
                        color = scrollBarColor,
                        shape = MaterialTheme.shapes.small
                    )
                    .fillMaxWidth()
                    .weight(windowWidth)
                )
            }
            if (after > 0) Box(modifier = Modifier.fillMaxWidth().weight(after))
        }
    }
}
