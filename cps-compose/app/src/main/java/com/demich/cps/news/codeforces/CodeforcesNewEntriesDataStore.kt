package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.*
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CodeforcesNewEntriesDataStore(context: Context): CPSDataStore(context.cf_new_entries_dataStore) {
    companion object {
        private val Context.cf_new_entries_dataStore by preferencesDataStore("cf_new_entries")
    }

    val mainNewEntries = itemJsonable<NewEntriesTypes>(name = "main", defaultValue = emptyMap())
}


@Composable
fun rememberCodeforcesBlogEntriesController(
    blogEntriesFlow: Flow<List<CodeforcesBlogEntry>>,
    newEntriesItem: CPSDataStoreItem<NewEntriesTypes>
): CodeforcesBlogEntriesController {
    val context = context
    val scope = rememberCoroutineScope()
    val types = rememberCollect { newEntriesItem.flow }
    val blogEntriesState = rememberCollect { blogEntriesFlow }
    LaunchedEffect(blogEntriesState.value) {
        val ids = blogEntriesState.value.map { it.id.toString() }
        NewEntriesController(newEntriesItem).apply(newEntries = ids)
    }
    return remember {
        object : CodeforcesBlogEntriesController(
            blogEntriesState = blogEntriesState,
            types = types
        ) {
            override fun openBlogEntry(blogEntry: CodeforcesBlogEntry) {
                scope.launch {
                    NewEntriesController(newEntriesItem).mark(id = blogEntry.id.toString(), type = NewEntryType.OPENED)
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