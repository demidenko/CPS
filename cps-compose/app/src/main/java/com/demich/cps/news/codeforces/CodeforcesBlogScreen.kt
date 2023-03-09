package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.demich.cps.data.api.CodeforcesBlogEntry
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.utils.LoadingStatus

@Composable
fun CodeforcesBlogScreen(
    blogEntries: () -> List<CodeforcesBlogEntry>,
    loadingStatus: () -> LoadingStatus
) {
    LoadingContentBox(
        loadingStatus = loadingStatus(),
        failedText = "Blog load error",
        modifier = Modifier.fillMaxSize()
    ) {
        CodeforcesBlogEntries(
            blogEntriesController = rememberCodeforcesBlogEntriesController(blogEntries),
            enableScrollBar = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}