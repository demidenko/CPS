package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.niceMessage
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.filter.FilterIconButton
import com.demich.cps.ui.filter.FilterState
import com.demich.cps.ui.filter.FilterTextField
import com.demich.cps.ui.filter.rememberFilterState
import com.demich.cps.utils.ProvideTimeEachMinute
import com.demich.cps.utils.context
import com.demich.cps.utils.filterByTokensAsSubsequence
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState

@Composable
fun CodeforcesUserBlogScreen(
    blogEntriesResult: () -> Result<List<CodeforcesBlogEntry>>?,
    onRetry: () -> Unit,
    filterState: FilterState
) {
    LaunchedEffect(filterState, blogEntriesResult) {
        //available = res != null && res.isSuccess && res.value.isNotEmpty()
        snapshotFlow { blogEntriesResult()?.map { it.isNotEmpty() } }
            .collect { result ->
                filterState.available = result?.getOrNull() == true
            }
    }

    Column {
        CodeforcesUserBlogContent(
            blogEntriesResult = blogEntriesResult,
            onRetry = onRetry,
            filterState = filterState,
            modifier = Modifier.weight(1f)
        )
        FilterTextField(
            filterState = filterState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CodeforcesUserBlogContent(
    blogEntriesResult: () -> Result<List<CodeforcesBlogEntry>>?,
    onRetry: () -> Unit,
    filterState: FilterState,
    modifier: Modifier = Modifier
) {
    LoadingContentBox(
        dataResult = blogEntriesResult,
        failedText = { it.niceMessage ?: "Blog load error" },
        onRetry = onRetry,
        modifier = modifier.fillMaxSize()
    ) { blogEntries ->
        ProvideTimeEachMinute {
            CodeforcesBlogEntries(
                blogEntriesState = rememberCodeforcesBlogEntriesState {
                    blogEntries.filterBy(filterState)
                },
                scrollBarEnabled = true,
                scrollUpButtonEnabled = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NavContentCodeforcesBlog(
    holder: CPSNavigator.DuringCompositionHolder
) {
    val handle = (holder.screen as Screen.CommunityCodeforcesBlog).handle

    var dataKey by rememberUUIDState
    val blogEntriesResult by codeforcesCommunityViewModel()
        .flowOfBlogEntriesResult(handle, context, key = dataKey)
        .collectAsState()

    val filterState = rememberFilterState()

    CodeforcesUserBlogScreen(
        blogEntriesResult = { blogEntriesResult },
        onRetry = { dataKey = randomUuid() },
        filterState = filterState
    )

    holder.bottomBar = {
        FilterIconButton(filterState = filterState)
    }

    holder.setSubtitle("community", "codeforces", "blog")
}

private fun List<CodeforcesBlogEntry>.filterBy(state: FilterState) =
    filterByTokensAsSubsequence(state.filter) {
        sequenceOf(title)
    }