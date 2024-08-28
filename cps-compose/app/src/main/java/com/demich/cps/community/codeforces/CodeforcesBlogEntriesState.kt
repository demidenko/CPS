package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.ui.lazylist.visibleRange
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


abstract class NewEntriesState {
    abstract val types: Map<Int, NewEntryInfo>
    abstract suspend fun markSeen(ids: List<Int>)
    abstract fun markOpened(id: Int)
    fun getType(id: Int) = types.getType(id)
}

@Composable
fun rememberNewEntriesState(): NewEntriesState {
    val context = context
    val scope = rememberCoroutineScope()
    val item = remember { CodeforcesNewEntriesDataStore(context).commonNewEntries }
    val typesState = rememberCollect { item.flow }
    return remember(scope, item, typesState) {
        object : NewEntriesState() {
            override val types by typesState

            override suspend fun markSeen(ids: List<Int>) {
                item.markAtLeast(ids, NewEntryType.SEEN)
            }

            override fun markOpened(id: Int) {
                scope.launch {
                    item.markAtLeast(id, NewEntryType.OPENED)
                }
            }
        }
    }
}

@Stable
abstract class CodeforcesBlogEntriesState {
    abstract val blogEntries: List<CodeforcesBlogEntry>

    protected open fun onOpenBlogEntry(blogEntry: CodeforcesBlogEntry) = Unit
    fun openBlogEntry(blogEntry: CodeforcesBlogEntry, context: Context) {
        onOpenBlogEntry(blogEntry)
        context.openUrlInBrowser(url = CodeforcesApi.urls.blogEntry(blogEntryId = blogEntry.id))
    }

    open fun isNew(id: Int): Boolean = false
}

@Composable
fun rememberCodeforcesBlogEntriesState(
    blogEntries: () -> List<CodeforcesBlogEntry>
): CodeforcesBlogEntriesState {
    return remember(blogEntries) {
        object : CodeforcesBlogEntriesState() {
            override val blogEntries get() = blogEntries()
        }
    }
}

@Composable
fun rememberCodeforcesBlogEntriesState(
    blogEntriesFlow: Flow<List<CodeforcesBlogEntry>>,
    isTabVisible: () -> Boolean,
    listState: LazyListState,
    newEntriesState: NewEntriesState,
    showNewEntries: Boolean
): CodeforcesBlogEntriesState {

    LaunchedEffect(blogEntriesFlow, newEntriesState, listState, isTabVisible) {
        combine(
            flow = blogEntriesFlow
                .map { it.map { it.id } }
                .distinctUntilChanged(),
            flow2 = snapshotFlow {
                if (!isTabVisible()) IntRange.EMPTY
                else listState.visibleRange(0.75f)
            }
        ) { ids, visibleRange ->
            if (ids.isEmpty()) {
                //empty ids can create Empty message item!!
                emptyList()
            } else {
                ids.subList(visibleRange)
            }
        }
            .debounce(250.milliseconds) //to sync ids with range / prevent user do fast scroll
            .distinctUntilChanged() //prevent repeats after debounce
            .collect { visibleIds ->
                newEntriesState.markSeen(ids = visibleIds)
            }
    }

    val blogEntriesState = rememberCollectWithLifecycle { blogEntriesFlow }
    return remember(blogEntriesState, newEntriesState, showNewEntries) {
        object : CodeforcesBlogEntriesState() {
            override val blogEntries by blogEntriesState

            override fun onOpenBlogEntry(blogEntry: CodeforcesBlogEntry) {
                newEntriesState.markOpened(id = blogEntry.id)
            }

            override fun isNew(id: Int): Boolean {
                if (!showNewEntries) return false
                val type = newEntriesState.getType(id)
                return type == NewEntryType.UNSEEN || type == NewEntryType.SEEN
            }
        }
    }
}