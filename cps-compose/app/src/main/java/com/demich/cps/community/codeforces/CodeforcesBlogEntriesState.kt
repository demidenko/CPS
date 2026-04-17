package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.utils.NewEntriesMap
import com.demich.cps.utils.collectAsStateWithLifecycle
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.getType
import com.demich.cps.utils.launchData
import com.demich.cps.utils.openUrlInBrowser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds


interface NewEntriesState {
    val types: NewEntriesMap
    fun markSeen(ids: List<Int>)
    fun markOpened(id: Int)
}

@Composable
fun rememberNewEntriesState(): NewEntriesState {
    val context = context
    val viewModel = codeforcesCommunityViewModel()
    val item = remember { CodeforcesNewEntriesDataStore(context).commonNewEntries }
    val typesState = collectItemAsState { item }
    return remember(viewModel, item, typesState) {
        object : NewEntriesState {
            override val types by typesState

            override fun markSeen(ids: List<Int>) {
                viewModel.launchData {
                    item.markAtLeast(ids, SEEN)
                }
            }

            override fun markOpened(id: Int) {
                viewModel.launchData {
                    item.markAtLeast(id, OPENED)
                }
            }
        }
    }
}

@Stable
abstract class CodeforcesBlogEntriesState {
    abstract val blogEntries: List<CodeforcesWebBlogEntry>

    protected open fun onOpenBlogEntry(blogEntry: CodeforcesWebBlogEntry) = Unit
    fun openBlogEntry(blogEntry: CodeforcesWebBlogEntry, context: Context) {
        onOpenBlogEntry(blogEntry)
        context.openUrlInBrowser(url = CodeforcesUrls.blogEntry(blogEntryId = blogEntry.id))
    }

    open fun isNew(id: Int): Boolean = false
}

@Composable
fun rememberCodeforcesBlogEntriesState(
    blogEntries: () -> List<CodeforcesWebBlogEntry>
): CodeforcesBlogEntriesState {
    return remember(blogEntries) {
        object : CodeforcesBlogEntriesState() {
            override val blogEntries get() = blogEntries()
        }
    }
}

@Composable
fun rememberCodeforcesBlogEntriesState(
    blogEntriesFlow: Flow<List<CodeforcesWebBlogEntry>>,
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