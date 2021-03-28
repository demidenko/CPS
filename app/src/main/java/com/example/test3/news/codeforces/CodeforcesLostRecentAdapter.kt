package com.example.test3.news.codeforces

import com.example.test3.room.getLostBlogsDao
import com.example.test3.timeDifference
import com.example.test3.utils.getCurrentTimeSeconds


class CodeforcesLostRecentAdapter : CodeforcesBlogEntriesAdapter(), CodeforcesNewsItemsAdapterAutoUpdatable {
    override suspend fun parseData(s: String) = true

    override fun subscribeLiveData(
        fragment: CodeforcesNewsFragment,
        dataReadyCallback: () -> Unit
    ) {
        getLostBlogsDao(fragment.requireContext()).getLostLiveData().observe(fragment.viewLifecycleOwner){ blogs ->
            val currentTimeSeconds = getCurrentTimeSeconds()
            rows = blogs
                .sortedByDescending { it.timeStamp }
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
                }.toTypedArray()

            dataReadyCallback()
        }
    }
}