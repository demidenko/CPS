package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.context

@Composable
fun CodeforcesBlogScreen(
    blogEntriesState: State<List<CodeforcesBlogEntry>>,
    loadingStatus: LoadingStatus
) {
    val context = context
    val manager = remember { CodeforcesAccountManager(context) }
    CompositionLocalProvider(LocalCodeforcesAccountManager provides manager) {
        LoadingContentBox(
            loadingStatus = loadingStatus,
            failedText = "Blog load error",
            modifier = Modifier.fillMaxSize()
        ) {
            CodeforcesBlogEntries(
                blogEntriesState = blogEntriesState,
            )
        }
    }
}