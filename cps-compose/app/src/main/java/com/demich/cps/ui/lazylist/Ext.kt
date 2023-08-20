package com.demich.cps.ui.lazylist

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.demich.cps.ui.EmptyMessageBox

fun LazyListState.visibleRange(requiredVisiblePart: Float = 0.5f): IntRange {
    require(requiredVisiblePart in 0f..1f)

    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return IntRange.EMPTY

    val firstVisible = visibleItems.first().let { item ->
        val topHidden = (-item.offset).coerceAtLeast(0)
        val visiblePart = (item.size - topHidden).toFloat() / item.size
        if (visiblePart < requiredVisiblePart) item.index + 1 else item.index
    }

    val lastVisible = visibleItems.last().let { item ->
        val bottomHidden = (item.offset + item.size - layoutInfo.viewportEndOffset)
            .coerceAtLeast(0)
        val visiblePart = (item.size - bottomHidden).toFloat() / item.size
        if (visiblePart < requiredVisiblePart) item.index - 1 else item.index
    }

    return firstVisible .. lastVisible
}


inline fun <T> LazyListScope.itemsNotEmpty(
    items: List<T>,
    noinline onEmptyMessage: @Composable () -> Unit = { Text(text = "List is empty") },
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    if (items.isEmpty()) {
        item {
            EmptyMessageBox(
                modifier = Modifier.fillParentMaxSize(),
                content = onEmptyMessage
            )
        }
    } else {
        items(
            items = items,
            key = key,
            contentType = contentType,
            itemContent = itemContent
        )
    }
}