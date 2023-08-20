package com.demich.cps.ui.lazylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.ui.theme.cpsColors

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
        modifier = if (enableScrollBar) modifier.lazyColumnScrollBar(
            state = state,
            scrollBarColor = cpsColors.content.copy(alpha = 0.5f)
        ) else modifier,
        state = state,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}
