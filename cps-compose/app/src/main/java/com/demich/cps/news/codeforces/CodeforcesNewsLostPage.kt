package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.utils.context

@Composable
fun CodeforcesNewsLostPage(controller: CodeforcesNewsController) {
    val context = context

    val blogEntriesFlow = remember {
        controller.flowOfLostBlogEntries(context)
    }

    val newEntriesItem = remember {
        CodeforcesNewEntriesDataStore(context).lostNewEntries
    }

    CodeforcesBlogEntries(
        blogEntriesController = rememberCodeforcesBlogEntriesController(
            blogEntriesFlow = blogEntriesFlow,
            newEntriesItem = newEntriesItem
        ),
        modifier = Modifier.fillMaxWidth()
    )
}