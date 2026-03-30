package com.demich.cps.community.codeforces

import android.content.Context
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
import androidx.lifecycle.ViewModel
import com.demich.cps.community.follow.followRepository
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.clients.niceMessage
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag.BLACK
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.platforms.utils.codeforces.getRealColorTagOrNull
import com.demich.cps.platforms.utils.codeforces.toWebBlogEntry
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.filter.FilterIconButton
import com.demich.cps.ui.filter.FilterState
import com.demich.cps.ui.filter.FilterTextField
import com.demich.cps.ui.filter.rememberFilterState
import com.demich.cps.utils.ProvideSystemTimeEachMinute
import com.demich.cps.utils.awaitPair
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.context
import com.demich.cps.utils.filterByTokensAsSubsequence
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState
import com.sebaslogen.resaca.viewModelScoped

@Composable
fun CPSNavigator.ScreenScope<Screen.CommunityCodeforcesBlog>.NavContentCodeforcesBlog() {
    screenTitle = ScreenStaticTitleState("community", "codeforces", "blog")

    val filterState = rememberFilterState()
    CodeforcesUserBlogScreen(
        handle = screen.handle,
        filterState = filterState
    )

    bottomBar = {
        FilterIconButton(filterState = filterState)
    }
}

@Composable
private fun CodeforcesUserBlogScreen(
    handle: String,
    filterState: FilterState
) {
    val viewModel = viewModelScoped { BlogLoadingViewModel() }

    var dataKey by rememberUUIDState()
    val blogEntriesResult by viewModel
        .flowOfBlogEntriesResult(handle, context, key = dataKey)
        .collectAsState()

    CodeforcesUserBlogScreen(
        blogEntriesResult = { blogEntriesResult },
        onRetry = { dataKey = randomUuid() },
        filterState = filterState
    )
}

@Composable
private fun CodeforcesUserBlogScreen(
    blogEntriesResult: () -> Result<List<CodeforcesWebBlogEntry>>?,
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
    blogEntriesResult: () -> Result<List<CodeforcesWebBlogEntry>>?,
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
        ProvideSystemTimeEachMinute {
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

private fun List<CodeforcesWebBlogEntry>.filterBy(state: FilterState) =
    filterByTokensAsSubsequence(state.filter) {
        sequenceOf(title)
    }

private class BlogLoadingViewModel: ViewModel() {

    private val blogEntriesLoader = backgroundDataLoader<List<CodeforcesWebBlogEntry>>()

    // TODO: context leak
    fun flowOfBlogEntriesResult(handle: String, context: Context, key: Any) =
        blogEntriesLoader.execute(key = Pair(handle, key)) {
            val (blogEntries, colorTag) = awaitPair(
                blockFirst = { context.followRepository.getAndReloadBlogEntries(handle).getOrThrow() },
                blockSecond = { CodeforcesClient.getRealColorTagOrNull(handle) ?: BLACK }
            )
            blogEntries.map {
                it.toWebBlogEntry(colorTag = colorTag)
            }
        }
}