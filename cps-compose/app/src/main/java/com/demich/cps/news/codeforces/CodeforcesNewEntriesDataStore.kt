package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CodeforcesNewEntriesDataStore(context: Context): ItemizedDataStore(context.cf_new_entries_dataStore) {
    companion object {
        private val Context.cf_new_entries_dataStore by dataStoreWrapper("cf_new_entries")
    }

    val mainNewEntries = itemNewEntriesTypes(name = "main")
    val lostNewEntries = itemNewEntriesTypes(name = "lost")

    private fun itemNewEntriesTypes(name: String) =
        NewEntriesDataStoreItem(jsonCPS.item(name = name, defaultValue = emptyMap()))
}

@Composable
fun rememberCodeforcesBlogEntriesController(
    blogEntriesState: State<List<CodeforcesBlogEntry>>,
): CodeforcesBlogEntriesController {
    return rememberWith(blogEntriesState) {
        CodeforcesBlogEntriesController(blogEntriesState = this)
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

    LaunchedEffect(tab, blogEntriesFlow, newEntriesItem, listState, controller) {
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
        }.launchIn(this)
    }

    val scope = rememberCoroutineScope()
    val types = rememberCollect { newEntriesItem.flow }
    val blogEntriesState = rememberCollect { blogEntriesFlow }
    return remember {
        object : CodeforcesBlogEntriesController(
            blogEntriesState = blogEntriesState,
            types = types
        ) {
            override fun onOpenBlogEntry(blogEntry: CodeforcesBlogEntry) {
                scope.launch {
                    newEntriesItem.mark(id = blogEntry.id, type = NewEntryType.OPENED)
                }
            }
        }
    }
}


@Stable
open class CodeforcesBlogEntriesController(
    blogEntriesState: State<List<CodeforcesBlogEntry>>,
    private val types: State<NewEntriesTypes>? = null
) {
    protected open fun onOpenBlogEntry(blogEntry: CodeforcesBlogEntry) = Unit
    fun openBlogEntry(blogEntry: CodeforcesBlogEntry, context: Context) {
        onOpenBlogEntry(blogEntry)
        context.openUrlInBrowser(url = CodeforcesApi.urls.blogEntry(blogEntryId = blogEntry.id))
    }

    val blogEntries by blogEntriesState

    fun isNew(id: Int): Boolean {
        if (types == null) return false
        val type = types.value[id]
        return type == NewEntryType.UNSEEN || type == NewEntryType.SEEN
    }
}