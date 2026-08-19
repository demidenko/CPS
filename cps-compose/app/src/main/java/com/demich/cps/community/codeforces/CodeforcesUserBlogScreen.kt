package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.demich.cps.community.follow.followRepository
import com.demich.cps.features.codeforces.follow.database.blog
import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.fetchstate.FetchState
import com.demich.cps.fetchstate.fetchResultOf
import com.demich.cps.fetchstate.map
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.clients.niceMessage
import com.demich.cps.platforms.codeforces.follow.storage.handle
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag.BLACK
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.platforms.utils.codeforces.getRealColorTagOrNull
import com.demich.cps.platforms.utils.codeforces.toWebBlogEntry
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.filter.FilterIconButton
import com.demich.cps.ui.filter.FilterState
import com.demich.cps.ui.filter.FilterTextField
import com.demich.cps.ui.filter.filterByTokensAsSubsequence
import com.demich.cps.ui.filter.rememberFilterState
import com.demich.cps.utils.ProvideSystemTimeEachMinute
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberUUIDState
import com.sebaslogen.resaca.viewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

@Composable
fun CPSNavigator.ScreenScope<Screen.CommunityCodeforcesBlog>.NavContentCodeforcesBlog() {
    screenTitle = ScreenStaticTitleState("community", "codeforces", "blog")

    val filterState = rememberFilterState()
    CodeforcesUserBlogScreen(
        blogId = screen.blogId,
        filterState = filterState
    )

    bottomBar = {
        FilterIconButton(filterState = filterState)
    }
}

@Composable
private fun CodeforcesUserBlogScreen(
    blogId: Long,
    filterState: FilterState
) {
    val context = context
    val viewModel = viewModelScoped { BlogLoadingViewModel() }

    val uuidState = rememberUUIDState()

    val flow = viewModel.flowOfFetchBlogEntries(blogId, context, key = uuidState.value)
    val blogEntriesState = flow.collectAsState()

    CodeforcesUserBlogScreen(
        blogEntries = blogEntriesState::value,
        onRetry = uuidState::reset,
        filterState = filterState
    )

    LaunchedEffect(flow, filterState) {
        flow.collect {
            filterState.available = it is FetchResult.Success && it.value.isNotEmpty()
        }
    }
}

@Composable
private fun CodeforcesUserBlogScreen(
    blogEntries: () -> FetchState<List<CodeforcesWebBlogEntry>>,
    onRetry: () -> Unit,
    filterState: FilterState
) {
    ProvideSystemTimeEachMinute {
        Column {
            // TODO: add user blog info
            CodeforcesUserBlogContent(
                blogEntries = { blogEntries().map { it.filterBy(filterState) } },
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            )
            FilterTextField(
                filterState = filterState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CodeforcesUserBlogContent(
    blogEntries: () -> FetchState<List<CodeforcesWebBlogEntry>>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LoadingContentBox(
        fetchState = blogEntries,
        failedText = { it.niceMessage ?: "Failed to get blog" },
        onRetry = onRetry,
        modifier = modifier.fillMaxSize()
    ) { blogEntries ->
        CodeforcesBlogEntries(
            blogEntries = { blogEntries },
            newEntriesState = remember { object : CodeforcesNewEntriesState() {} },
            scrollBarEnabled = true,
            scrollUpButtonEnabled = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun List<CodeforcesWebBlogEntry>.filterBy(state: FilterState) =
    filterByTokensAsSubsequence(state) {
        sequenceOf(title)
    }

private class BlogLoadingViewModel: ViewModel() {

    private val blogEntriesLoader = backgroundDataLoader<List<CodeforcesWebBlogEntry>>()

    fun flowOfFetchBlogEntries(blogId: Long, context: Context, key: Any) = blogEntriesLoader.run {
        val repository = context.followRepository
        executeFlow(key = Pair(blogId, key)) {
            val handle = repository.blog(blogId = blogId).handle
            combine(
                flow = flow { emit(repository.getAndReloadBlogEntries(handle).getOrThrow()) },
                flow2 = flowOfColorTag(handle = handle)
            ) { blogEntries, colorTag ->
                blogEntries.map {
                    it.toWebBlogEntry(colorTag = colorTag ?: BLACK)
                }
            }.let { emitAll(it) }
        }
    }

    private fun flowOfColorTag(handle: String): Flow<CodeforcesColorTag?> =
        flow {
            val result = fetchResultOf {
                CodeforcesClient().getRealColorTagOrNull(handle)
            }
            if (result is FetchResult.Success) emit(result.value)
        }.onStart {
            emit(null)
        }
}