package com.demich.cps.ui.lazylist

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun<T> LazyColumnOfData(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollBarEnabled: Boolean = true,
    items: () -> List<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    LazyColumnWithScrollBar(
        modifier = modifier,
        state = state,
        scrollBarEnabled = scrollBarEnabled
    ) {
        itemsNotEmpty(
            items = items(),
            key = key,
            itemContent = itemContent
        )
    }
}