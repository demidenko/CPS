package com.demich.cps.community.codeforces

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.UriHandler
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.utils.NewEntriesMap
import com.demich.cps.utils.backgroundCoroutineScope
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.getType
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


interface NewEntriesState {
    val types: NewEntriesMap
    fun markSeen(ids: List<Int>)
    fun markOpened(id: Int)
}

@Composable
fun rememberNewEntriesState(): NewEntriesState {
    val context = context
    val scope = backgroundCoroutineScope
    val item = remember { CodeforcesNewEntriesDataStore(context).commonNewEntries }
    val typesState = collectItemAsState { item }
    return remember {
        object : NewEntriesState {
            override val types by typesState

            override fun markSeen(ids: List<Int>) {
                scope.launch {
                    item.markAtLeast(ids, SEEN)
                }
            }

            override fun markOpened(id: Int) {
                scope.launch {
                    item.markAtLeast(id, OPENED)
                }
            }
        }
    }
}

@Stable
abstract class CodeforcesNewEntriesState {
    protected open fun onOpenBlogEntry(blogEntry: CodeforcesWebBlogEntry) = Unit
    fun openBlogEntry(blogEntry: CodeforcesWebBlogEntry, handler: UriHandler) {
        onOpenBlogEntry(blogEntry)
        handler.openUri(CodeforcesUrls.blogEntry(blogEntryId = blogEntry.id))
    }

    open fun isNew(id: Int): Boolean = false
}

@Composable
fun rememberCodeforcesNewEntriesState(
    isTabVisible: () -> Boolean,
    listState: LazyListState,
    newEntriesState: NewEntriesState,
    showNewEntries: Boolean
): CodeforcesNewEntriesState {
    LaunchedEffect(newEntriesState, listState, isTabVisible) {
        snapshotFlow {
            if (!isTabVisible()) emptyList()
            else listState.visibleBlogEntriesIds(0.75f)
        }
            .debounce(250.milliseconds) //prevent user do fast scroll / page switch
            .distinctUntilChanged() //prevent repeats after debounce
            .collect { visibleIds ->
                newEntriesState.markSeen(ids = visibleIds)
            }
    }

    return remember(newEntriesState, showNewEntries) {
        object : CodeforcesNewEntriesState() {
            override fun onOpenBlogEntry(blogEntry: CodeforcesWebBlogEntry) {
                newEntriesState.markOpened(id = blogEntry.id)
            }

            override fun isNew(id: Int): Boolean {
                if (!showNewEntries) return false
                val type = newEntriesState.types.getType(id)
                return type == UNSEEN || type == SEEN
            }
        }
    }
}