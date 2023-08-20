package com.demich.cps.ui.lazylist

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun<T> LazyColumnOfData(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollBarEnabled: Boolean = true,
    items: () -> List<T>,
    key: ((item: T) -> Any)? = null,
    onEmptyMessage: @Composable () -> Unit = { Text(text = "List is empty") },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    LazyColumnWithScrollBar(
        modifier = modifier,
        state = state,
        enableScrollBar = scrollBarEnabled
    ) {
        itemsNotEmpty(
            items = items(),
            key = key,
            onEmptyMessage = onEmptyMessage,
            itemContent = itemContent
        )
    }
}