package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


@Stable
abstract class CodeforcesBlogEntriesController {
    abstract val blogEntries: List<CodeforcesBlogEntry>

    protected open fun onOpenBlogEntry(blogEntry: CodeforcesBlogEntry) = Unit
    fun openBlogEntry(blogEntry: CodeforcesBlogEntry, context: Context) {
        onOpenBlogEntry(blogEntry)
        context.openUrlInBrowser(url = CodeforcesApi.urls.blogEntry(blogEntryId = blogEntry.id))
    }

    open fun isNew(id: Int): Boolean = false
}

@Composable
fun rememberCodeforcesBlogEntriesController(
    blogEntries: () -> List<CodeforcesBlogEntry>
): CodeforcesBlogEntriesController {
    return remember(blogEntries) {
        object : CodeforcesBlogEntriesController() {
            override val blogEntries get() = blogEntries()
        }
    }
}

@Composable
fun rememberCodeforcesBlogEntriesController(
    tab: CodeforcesTitle,
    blogEntriesFlow: Flow<List<CodeforcesBlogEntry>>,
    newEntriesItem: NewEntriesDataStoreItem,
    listState: LazyListState,
    controller: CodeforcesNewsController
): CodeforcesBlogEntriesController {

    LaunchedEffect(tab, newEntriesItem, listState, controller) {
        val flowOfIds = blogEntriesFlow.map {
            it.map { it.id }
        }.distinctUntilChanged().onEach { ids ->
            newEntriesItem.apply(newEntries = ids)
        }

        snapshotFlow {
            if (!controller.isTabVisible(tab)) return@snapshotFlow IntRange.EMPTY
            listState.visibleRange(0.75f)
        }.combine(flowOfIds) { visibleRange, ids ->
            //empty ids can create Empty message item!!
            if (ids.isNotEmpty()) {
                newEntriesItem.markAtLeast(
                    ids = visibleRange.map { ids[it] },
                    type = NewEntryType.SEEN
                )
            }
        }.debounce(250.milliseconds).launchIn(this)
    }

    val scope = rememberCoroutineScope()
    val types = rememberCollect { newEntriesItem.flow }
    val blogEntriesState = rememberCollectWithLifecycle { blogEntriesFlow }
    return remember(blogEntriesState, types, scope) {
        object : CodeforcesBlogEntriesController() {
            override val blogEntries by blogEntriesState

            override fun onOpenBlogEntry(blogEntry: CodeforcesBlogEntry) {
                scope.launch {
                    newEntriesItem.mark(id = blogEntry.id, type = NewEntryType.OPENED)
                }
            }

            override fun isNew(id: Int): Boolean {
                val type = types.value[id]
                return type == NewEntryType.UNSEEN || type == NewEntryType.SEEN
            }
        }
    }
}