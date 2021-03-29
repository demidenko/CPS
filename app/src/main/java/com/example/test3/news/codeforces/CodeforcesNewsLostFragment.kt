package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test3.CodeforcesTitle
import com.example.test3.room.getLostBlogsDao
import com.example.test3.timeDifference
import com.example.test3.utils.getCurrentTimeSeconds
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CodeforcesNewsLostFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.LOST
    override val isAutoUpdatable = true
    private val itemsAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            lifecycleScope,
            makeDataFlow(),
            newsFragment.viewedDataStore.blogsViewedFlow(title),
            isManagesNewEntries = true,
            clearNewEntriesOnDataChange = false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        swipeRefreshLayout.isEnabled = false

        subscribeNewEntries(itemsAdapter)
        subscribeRefreshOnRealColor { itemsAdapter.refresh() }

        if(savedInstanceState == null) callReload()
    }

    override fun onPageSelected(tab: TabLayout.Tab) {
        tab.badge?.run {
            if(hasNumber()){
                isVisible = true
                saveBlogEntriesAsViewed(itemsAdapter)
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