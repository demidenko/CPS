package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.utils.NewEntryInfo
import com.demich.cps.utils.NewEntryType
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.collectAsStateWithLifecycle
import com.demich.cps.utils.context
import com.demich.cps.utils.getType
import com.demich.cps.utils.openUrlInBrowser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val typesState = collectAsState { item.flow }
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

    val blogEntriesState = collectAsStateWithLifecycle { blogEntriesFlow }
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