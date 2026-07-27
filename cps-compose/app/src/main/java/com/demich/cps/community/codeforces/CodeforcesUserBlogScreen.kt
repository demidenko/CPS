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
import com.demich.cps.utils.FetchResult
import com.demich.cps.utils.FetchState
import com.demich.cps.utils.ProvideSystemTimeEachMinute
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.context
import com.demich.cps.utils.filterByTokensAsSubsequence
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState
import com.sebaslogen.resaca.viewModelScoped
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

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
    val flow = viewModel.flowOfFetchBlogEntries(handle, context, key = dataKey)
    val blogEntries by flow.collectAsState()

    CodeforcesUserBlogScreen(
        blogEntries = { blogEntries },
        onRetry = { dataKey = randomUuid() },
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
    Column {
        CodeforcesUserBlogContent(
            blogEntries = blogEntries,
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
    blogEntries: () -> FetchState<List<CodeforcesWebBlogEntry>>,
    onRetry: () -> Unit,
    filterState: FilterState,
    modifier: Modifier = Modifier
) {
    LoadingContentBox(
        fetchState = blogEntries,
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
    fun flowOfFetchBlogEntries(handle: String, context: Context, key: Any) =
        blogEntriesLoader.executeFlow(key = Pair(handle, key)) {
            emitAll(
                combine(
                    flow = flow { emit(context.followRepository.getAndReloadBlogEntries(handle).getOrThrow()) },
                    flow2 = flow {
                        emit(null)
                        emit(CodeforcesClient().getRealColorTagOrNull(handle))
                    }
                ) { blogEntries, colorTag ->
                    blogEntries.map {
                        it.toWebBlogEntry(colorTag = colorTag ?: BLACK)
                    }
                }
            )
        }
}