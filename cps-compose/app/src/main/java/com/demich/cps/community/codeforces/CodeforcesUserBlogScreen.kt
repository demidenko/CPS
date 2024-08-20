package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.niceMessage
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.filter.FilterIconButton
import com.demich.cps.ui.filter.FilterState
import com.demich.cps.ui.filter.FilterTextField
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.filterByTokensAsSubsequence
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CodeforcesUserBlogScreen(
    blogEntriesResult: () -> Result<List<CodeforcesBlogEntry>>?,
    filterState: FilterState
) {
    LaunchedEffect(filterState, blogEntriesResult) {
        snapshotFlow(blogEntriesResult)
            .distinctUntilChanged()
            .collect { result ->
                filterState.available = result != null && result.isSuccess
            }
    }

    Column {
        LoadingContentBox(
            dataResult = blogEntriesResult,
            failedText = { it.niceMessage ?: "Blog load error" },
            modifier = Modifier.fillMaxSize()
        ) { blogEntries ->
            ProvideTimeEachMinute {
                CodeforcesBlogEntries(
                    blogEntriesController = rememberCodeforcesBlogEntriesController {
                        filterState.filterUserBlogEntries(blogEntries)
                    },
                    scrollBarEnabled = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        FilterTextField(
            filterState = filterState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun codeforcesUserBlogBottomBarBuilder(
    filterState: FilterState
): AdditionalBottomBarBuilder = {
    FilterIconButton(filterState = filterState)
}

private fun FilterState.filterUserBlogEntries(blogEntries: List<CodeforcesBlogEntry>) =
    blogEntries.filterByTokensAsSubsequence(filter) {
        sequenceOf(title)
    }