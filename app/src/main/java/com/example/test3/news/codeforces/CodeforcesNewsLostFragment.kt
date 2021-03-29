package com.example.test3.news.codeforces

import androidx.lifecycle.lifecycleScope
import com.example.test3.CodeforcesTitle
import com.example.test3.room.getLostBlogsDao
import com.example.test3.timeDifference
import com.example.test3.utils.getCurrentTimeSeconds
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CodeforcesNewsLostFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.LOST
    override val isManagesNewEntries = true
    override val isAutoUpdatable = true
    override val viewAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            lifecycleScope,
            makeDataFlow(),
            newsFragment.viewedDataStore.blogsViewedFlow(title),
            isManagesNewEntries = isManagesNewEntries,
            clearNewEntriesOnDataChange = false
        )
    }

    override fun onPageSelected(tab: TabLayout.Tab) {
        tab.badge?.run {
            if(hasNumber()){
                isVisible = true
                saveEntries()
            }
        }
    }

    override fun onPageUnselected(tab: TabLayout.Tab) {
        tab.badge?.run {
            if(hasNumber()) isVisible = false
        }
    }

    private fun makeDataFlow() = getLostBlogsDao(requireContext()).getLostFlow()
        .distinctUntilChanged()
        .map { blogEntries ->
            val currentTimeSeconds = getCurrentTimeSeconds()
            blogEntries.sortedByDescending { it.timeStamp }
                .map {
                    CodeforcesBlogEntriesAdapter.BlogEntryInfo(
                        blogId = it.id,
                        title = it.title,
                        author = it.authorHandle,
                        authorColorTag = it.authorColorTag,
                        time = timeDifference(it.creationTimeSeconds, currentTimeSeconds),
                        comments = "",
                        rating = ""
                    )
                }
        }
}