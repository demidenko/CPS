package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.community.CommunityUnitsFactory
import com.demich.cps.community.LocalCommunityUnitsFactory
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesAdminTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRatingColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesUnratedTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesUserTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.platforms.utils.codeforces.toWebBlogEntry
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.onNotNull
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Composable
fun CodeforcesCommunityLostPage(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    pagerState: CodeforcesCommunityPagerState,
    newEntriesState: NewEntriesState
) {
    val unitsFactory = LocalCommunityUnitsFactory.current
    val listState = rememberLazyListState()

    val codeforcesNewEntriesState = rememberCodeforcesNewEntriesState(
        isListVisible = { pagerState.isTabVisible(tab = LOST) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = true
    )

    val topIdsState = remember(backgroundLoader) {
        backgroundLoader.flowOfTopBlogEntries().map { blogEntries ->
            blogEntries?.mapToSet { it.id }
        }
    }.collectAsState(initial = null)

    val blogEntries by remember { unitsFactory.flowOfLostBlogEntries() }
        .collectAsStateWithLifecycle(initialValue = null)

    CodeforcesBlogEntriesFollowAddable(
        blogEntries = blogEntries,
        newEntriesState = codeforcesNewEntriesState,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize(),
        scrollBarEnabled = true,
        label = {
            topIdsState.onNotNull { topIds ->
                if (it.id in topIds) TopLabel()
            }
        }
    )
}

@Composable
private fun TopLabel() {
    Text(
        text = "TOP",
        color = cpsColors.contentAdditional,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
}

fun CommunityUnitsFactory.flowOfLostBlogEntries(): Flow<List<CodeforcesWebBlogEntry>> =
    combine(
        flow = lostStorage().flowOfLostEntries(),
        flow2 = settingsStorage().codeforcesLostMinRatingTag.asFlow()
    ) { list, minRatingTag ->
        list.sortedByDescending { it.timeStamp }.toWebBlogEntries(minRatingTag)
    }

private fun List<CodeforcesLostBlogEntry>.toWebBlogEntries(minRatingTag: CodeforcesRatingColorTag?): List<CodeforcesWebBlogEntry> =
    mapNotNull {
        val authorTag = it.authorUserTag
        if (authorTag != null && (minRatingTag == null || authorTag.isGreaterOrEqualTo(minRatingTag))) {
            it.blogEntry.toWebBlogEntry(tag = authorTag)
        } else null
    }

private fun CodeforcesUserTag.isGreaterOrEqualTo(ratingTag: CodeforcesRatingColorTag) =
    when (this) {
        is CodeforcesRatingColorTag -> this >= ratingTag
        is CodeforcesUnratedTag -> false
        is CodeforcesAdminTag -> true
    }
