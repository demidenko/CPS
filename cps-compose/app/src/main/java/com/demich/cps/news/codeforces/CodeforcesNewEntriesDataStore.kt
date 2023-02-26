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
    val context = context
    return rememberWith(blogEntriesState) {
        object : CodeforcesBlogEntriesController(blogEntriesState = this) {
            override fun openBlogEntry(blogEntry: CodeforcesBlogEntry) {
                context.openUrlInBrowser(url = CodeforcesApi.urls.blogEntry(blogEntryId = blogEntry.id))
            }
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
    val context = context

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
            override fun openBlogEntry(blogEntry: CodeforcesBlogEntry) {
                scope.launch {
                    newEntriesItem.mark(id = blogEntry.id, type = NewEntryType.OPENED)
                }
                context.openUrlInBrowser(url = CodeforcesApi.urls.blogEntry(blogEntryId = blogEntry.id))
            }
        }
    }
}


@Stable
abstract class CodeforcesBlogEntriesController(
    val blogEntriesState: State<List<CodeforcesBlogEntry>>,
    val types: State<NewEntriesTypes> = mutableStateOf(emptyMap()),
) {
    abstract fun openBlogEntry(blogEntry: CodeforcesBlogEntry)

    val blogEntries by blogEntriesState

    fun isNew(id: Int): Boolean {
        val type = types.value[id]
        return type == NewEntryType.UNSEEN || type == NewEntryType.SEEN
    }
}