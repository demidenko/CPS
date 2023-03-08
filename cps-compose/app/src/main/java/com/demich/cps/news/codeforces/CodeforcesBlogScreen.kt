package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.data.api.CodeforcesBlogEntry

@Composable
fun CodeforcesBlogScreen(
    blogEntriesState: State<List<CodeforcesBlogEntry>>,
    loadingStatus: LoadingStatus
) {
    LoadingContentBox(
        loadingStatus = loadingStatus,
        failedText = "Blog load error",
        modifier = when (loadingStatus) {
            LoadingStatus.PENDING -> Modifier.fillMaxWidth()
            else -> Modifier.fillMaxSize()
        }
    ) {
        CodeforcesBlogEntries(
            blogEntriesController = rememberCodeforcesBlogEntriesController(blogEntriesState = blogEntriesState),
            enableScrollBar = true
        )
    }
}