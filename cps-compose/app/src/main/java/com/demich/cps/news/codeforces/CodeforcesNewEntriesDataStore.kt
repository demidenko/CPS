package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.datastore_itemized.ItemizedDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CodeforcesNewEntriesDataStore(context: Context): ItemizedDataStore(context.cf_new_entries_dataStore) {
    companion object {
        private val Context.cf_new_entries_dataStore by preferencesDataStore("cf_new_entries")
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
    return remember(blogEntriesState) {
        object : CodeforcesBlogEntriesController(blogEntriesState = blogEntriesState) {
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
    val scope = rememberCoroutineScope()
    val types = rememberCollect { newEntriesItem.flow }
    val blogEntriesState = rememberCollect { blogEntriesFlow }

    //TODO: merge this flows
    LaunchedEffect(Unit) {
        snapshotFlow {
            blogEntriesState.value.map { it.id }
        }.collect { ids ->
            newEntriesItem.apply(newEntries = ids.map(Int::toString))
        }
    }

    LaunchedEffect(controller, listState) {
        snapshotFlow<List<Int>> {
            if (!controller.isTabVisible(tab)) return@snapshotFlow emptyList()
            val ids = blogEntriesState.value.map { it.id }
            if (ids.isEmpty()) return@snapshotFlow emptyList()
            listState.visibleRange(0.75f).map { ids[it] }
        }.collect { visibleIds ->
            newEntriesItem.markAtLeast(
                ids = visibleIds.map(Int::toString),
                type = NewEntryType.SEEN
            )
        }
    }

    return remember {
        object : CodeforcesBlogEntriesController(
            blogEntriesState = blogEntriesState,
            types = types
        ) {
            override fun openBlogEntry(blogEntry: CodeforcesBlogEntry) {
                scope.launch {
                    newEntriesItem.mark(id = blogEntry.id.toString(), type = NewEntryType.OPENED)
                }
                context.openUrlInBrowser(url = CodeforcesApi.urls.blogEntry(blogEntryId = blogEntry.id))
            }
        }
    }
}


@Stable
abstract class CodeforcesBlogEntriesController(
    val blogEntriesState: State<List<CodeforcesBlogEntry>>,
    val types: State<Map<String, NewEntryType>> = mutableStateOf(emptyMap()),
) {
    abstract fun openBlogEntry(blogEntry: CodeforcesBlogEntry)

    val blogEntries by blogEntriesState

    fun isNew(id: Int): Boolean {
        val type = types.value[id.toString()]
        return type == NewEntryType.UNSEEN || type == NewEntryType.SEEN
    }
}