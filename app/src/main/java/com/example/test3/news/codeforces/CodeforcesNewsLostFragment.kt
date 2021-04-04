package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test3.CodeforcesTitle
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.room.LostBlogEntry
import com.example.test3.room.getLostBlogsDao
import com.example.test3.timeDifference
import com.example.test3.utils.getCurrentTimeSeconds
import com.example.test3.utils.off
import com.example.test3.utils.on
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CodeforcesNewsLostFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.LOST
    private val itemsAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            this,
            makeDataFlow(getLostBlogsDao(requireContext()).getLostFlow()).distinctUntilChanged(),
            newsFragment.viewedDataStore.blogsViewedFlow(title),
            clearNewEntriesOnDataChange = false
        )
    }

    private val itemsSuspectsAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            this,
            makeDataFlow(getLostBlogsDao(requireContext()).getSuspectsFlow()).distinctUntilChanged(),
            null
        )
    }

    private val suspectsButton get() = newsFragment.suspectsLostButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        swipeRefreshLayout.isEnabled = false

        suspectsButton.apply {
            off()
            setOnClickListener {
                with(recyclerView){
                    if(adapter == itemsAdapter){
                        suspectsButton.on()
                        adapter = itemsSuspectsAdapter
                    } else {
                        suspectsButton.off()
                        adapter = itemsAdapter
                    }
                }
            }
        }

        subscribeNewEntries(itemsAdapter)
        subscribeRefreshOnRealColor { itemsAdapter.refresh() }

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

    private fun makeDataFlow(blogEntriesFlow: Flow<List<LostBlogEntry>>) =
        blogEntriesFlow.distinctUntilChanged()
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
                        rating = 0
                    )
                }
        }
}