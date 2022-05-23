package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@Composable
fun CodeforcesNewsMainPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val scope = rememberCoroutineScope()

    val loadingStatus by controller.rememberLoadingStatusState(CodeforcesTitle.MAIN)
    val blogEntriesState = rememberCollect { controller.flowOfMainBlogEntries(context) }

    val newEntriesDataStore = remember { CodeforcesNewEntriesDataStore(context) }
    val mainNewEntries = rememberCollect { newEntriesDataStore.mainNewEntries.flow }

    val newEntriesController = remember { NewEntriesController(newEntriesDataStore.mainNewEntries) }
    LaunchedEffect(blogEntriesState.value) {
        val ids = blogEntriesState.value.map { it.id.toString() }
        newEntriesController.apply(newEntries = ids)
    }

    val mainBlogEntriesController = remember {
        object : CodeforcesBlogEntriesController(
            blogEntriesState = blogEntriesState,
            types = mainNewEntries
        ) {
            override fun openBlogEntry(blogEntry: CodeforcesBlogEntry) {
                scope.launch {
                    newEntriesController.mark(id = blogEntry.id.toString(), type = NewEntryType.OPENED)
                }
                context.openUrlInBrowser(url = CodeforcesApi.urls.blogEntry(blogEntryId = blogEntry.id))
            }
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { controller.reload(title = CodeforcesTitle.MAIN, context = context) },
    ) {
        CodeforcesBlogEntries(
            blogEntriesController = mainBlogEntriesController,
            modifier = Modifier.fillMaxSize()
        )
    }
}

class CodeforcesNewEntriesDataStore(context: Context): CPSDataStore(context.cf_new_entries_dataStore) {
    companion object {
        private val Context.cf_new_entries_dataStore by preferencesDataStore("cf_new_entries")
    }

    val mainNewEntries = itemJsonable<Map<String,NewEntryType>>(name = "main", defaultValue = emptyMap())
}