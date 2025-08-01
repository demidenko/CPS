package com.demich.cps.ui.lazylist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.demich.cps.ui.EmptyMessageBox

fun LazyListLayoutInfo.visibleItemsInfo(requiredVisiblePart: Float): List<LazyListItemInfo> {
    require(requiredVisiblePart in 0f..1f)

    val visibleItems = visibleItemsInfo
    if (visibleItems.isEmpty()) return emptyList()

    val firstVisible = visibleItems.first().let { item ->
        val topHidden = (-item.offset).coerceAtLeast(0)
        val visiblePart = (item.size - topHidden).toFloat() / item.size
        if (visiblePart < requiredVisiblePart) 1 else 0
    }

    val lastVisible = visibleItems.last().let { item ->
        val bottomHidden = (item.offset + item.size - viewportEndOffset)
            .coerceAtLeast(0)
        val visiblePart = (item.size - bottomHidden).toFloat() / item.size
        if (visiblePart < requiredVisiblePart) visibleItems.lastIndex - 1 else visibleItems.lastIndex
    }

    if (firstVisible > lastVisible) return emptyList()
    return visibleItems.subList(fromIndex = firstVisible, toIndex = lastVisible + 1)
}

fun LazyListState.visibleRange(requiredVisiblePart: Float): IntRange =
    with(layoutInfo.visibleItemsInfo(requiredVisiblePart)) {
        if (isEmpty()) return IntRange.EMPTY

        val firstVisibleIndex = first().index
        val lastVisibleIndex = last().index

        firstVisibleIndex .. lastVisibleIndex
    }

inline fun <T> LazyListScope.itemsNotEmpty(
    items: List<T>,
    crossinline onEmptyMessage: @Composable () -> Unit,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    if (items.isEmpty()) {
        item(contentType = EmptyMessageContentType) {
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

data object EmptyMessageContentType

@Composable
inline fun LazyItemScope.ItemWithDivider(
    modifier: Modifier = Modifier,
    content: @Composable LazyItemScope.() -> Unit
) {
    Column(modifier = modifier) {
        content()
        Divider()
    }
}
