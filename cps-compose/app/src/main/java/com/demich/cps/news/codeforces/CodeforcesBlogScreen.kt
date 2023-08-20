package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.niceMessage
import com.demich.cps.ui.LoadingContentBox

@Composable
fun CodeforcesBlogScreen(
    blogEntriesResult: () -> Result<List<CodeforcesBlogEntry>>?
) {
    LoadingContentBox(
        dataResult = blogEntriesResult,
        failedText = { it.niceMessage ?: "Blog load error" },
        modifier = Modifier.fillMaxSize()
    ) { blogEntries ->
        CodeforcesBlogEntries(
            blogEntriesController = rememberCodeforcesBlogEntriesController { blogEntries },
            scrollBarEnabled = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}