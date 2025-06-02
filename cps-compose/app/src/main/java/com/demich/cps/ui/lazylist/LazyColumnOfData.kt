package com.demich.cps.ui.lazylist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
inline fun <T> LazyColumnOfData(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollBarEnabled: Boolean = true,
    scrollToStartButtonEnabled: Boolean = false,
    crossinline items: () -> List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    Box(modifier = modifier) {
        LazyColumnWithScrollBar(
            modifier = Modifier.fillMaxSize(),
            state = state,
            scrollBarEnabled = scrollBarEnabled
        ) {
            itemsNotEmpty(
                items = items(),
                key = key,
                itemContent = itemContent,
                contentType = contentType
            )
        }

        if (scrollToStartButtonEnabled) {
            LazyListScrollUpButton(
                listState = state,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp, end = 8.dp)
            )
        }
    }
}
