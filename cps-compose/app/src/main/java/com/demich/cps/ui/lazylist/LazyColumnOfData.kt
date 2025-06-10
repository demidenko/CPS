package com.demich.cps.ui.lazylist

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.utils.plusIf

@Composable
inline fun <T> LazyColumnOfData(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollBarEnabled: Boolean = true,
    scrollUpButtonEnabled: Boolean = false,
    crossinline items: () -> List<T>?,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    Box(modifier = modifier) {
        LazyColumnWithScrollBar(
            state = state,
            scrollBarEnabled = scrollBarEnabled,
            modifier = Modifier.fillMaxSize()
        ) {
            items()?.let {
                itemsNotEmpty(
                    items = it,
                    onEmptyMessage = { Text(text = "List is empty") },
                    key = key,
                    itemContent = itemContent,
                    contentType = contentType
                )
            }
        }

        if (scrollUpButtonEnabled) {
            LazyListScrollUpButton(
                listState = state,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }, //TODO: bad exit finish because of padding
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = 8.dp,
                        end = 4.dp.plusIf(scrollBarEnabled) { CPSDefaults.scrollBarWidth }
                    )
            )
        }
    }
}
