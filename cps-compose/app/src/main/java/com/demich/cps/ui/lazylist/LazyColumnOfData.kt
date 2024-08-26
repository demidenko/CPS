package com.demich.cps.ui.lazylist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun<T> LazyColumnOfData(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollBarEnabled: Boolean = true,
    scrollToStartButtonEnabled: Boolean = false,
    items: () -> List<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
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
                itemContent = itemContent
            )
        }

        if (scrollToStartButtonEnabled) {
            ScrollButton(listState = state, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun ScrollButton(
    modifier: Modifier = Modifier,
    listState: LazyListState
) {
    //TODO
}