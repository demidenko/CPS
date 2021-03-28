package com.example.test3.news.codeforces

import androidx.lifecycle.lifecycleScope
import com.example.test3.room.getLostBlogsDao
import com.example.test3.timeDifference
import com.example.test3.utils.getCurrentTimeSeconds
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


class CodeforcesLostRecentAdapter : CodeforcesBlogEntriesAdapter(), CodeforcesNewsItemsAdapterAutoUpdatable {
    override suspend fun parseData(s: String) = true

    override fun subscribeLiveData(
        fragment: CodeforcesNewsFragment,
        dataReadyCallback: () -> Unit
    ) {
        fragment.lifecycleScope.launchWhenStarted {
            getLostBlogsDao(fragment.requireContext()).getLostFlow()
                .distinctUntilChanged()
                .map { blogEntries ->
                    val currentTimeSeconds = getCurrentTimeSeconds()
                    blogEntries.sortedByDescending { it.timeStamp }
                        .map {
                            BlogEntryInfo(
                                blogId = it.id,
                                title = it.title,
                                author = it.authorHandle,
                                authorColorTag = it.authorColorTag,
                                time = timeDifference(it.creationTimeSeconds, currentTimeSeconds),
                                comments = "",
                                rating = ""
                            )
                        }
                }.collect { blogEntries ->
                    rows = blogEntries.toTypedArray()
                    dataReadyCallback()
                }
        }
    }
}