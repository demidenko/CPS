package com.demich.cps.ui.lazylist

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

suspend fun LazyListState.autoScrollToTop(
    predicate: (LazyListItemInfo, LazyListItemInfo) -> Boolean,
    animationScope: CoroutineScope
) {
    trackFirstVisibleChanges(predicate).collect {
        if (it) animationScope.launch {
            println("launch!")
            animateScrollToItem(index = 0)
        }
    }
}

fun LazyListState.trackFirstVisibleChanges(
    predicate: (LazyListItemInfo, LazyListItemInfo) -> Boolean
) = snapshotFlow {
        if (isScrollInProgress) null
        else layoutInfo.visibleItemsInfo.firstOrNull()
    }.zipWithPrev { prev, cur ->
        when {
            prev == null || cur == null -> false
            prev.key == cur.key && prev.offset == cur.offset && prev.size == cur.size ->
                prev.index != cur.index && predicate(prev, cur)
            else -> false
        }
    }

private fun <T, R> Flow<T>.zipWithPrev(
    transform: (T, T) -> R
): Flow<R> = flow {
    var prev: Any? = UNDEFINED
    collect {
        prev.let { prev ->
            if (prev !== UNDEFINED) emit(transform(prev as T, it))
        }
        prev = it
    }
}

private object UNDEFINED