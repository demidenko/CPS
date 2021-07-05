package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import com.example.test3.CodeforcesTitle
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.room.LostBlogEntry
import com.example.test3.room.getLostBlogsDao
import com.example.test3.ui.formatCPS
import com.example.test3.ui.off
import com.example.test3.ui.on
import com.example.test3.utils.CodeforcesBlogEntry
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CodeforcesNewsLostFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.LOST

    private val suspectsButton get() = newsFragment.suspectsLostButton

    private lateinit var itemsAdapter: CodeforcesBlogEntriesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout.isEnabled = false

        itemsAdapter = CodeforcesBlogEntriesAdapter(
            this,
            makeDataFlow(getLostBlogsDao(requireContext()).flowOfLost()),
            newsFragment.viewedDataStore.blogsViewedFlow(title),
            clearNewEntriesOnDataChange = false
        )

        val itemsSuspectsAdapter by lazy {
            CodeforcesBlogEntriesAdapter(
                this,
                makeDataFlow(getLostBlogsDao(requireContext()).flowOfSuspects()),
                null
            )
        }

        recyclerView.formatCPS().adapter = itemsAdapter

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
        subscribeRefreshOnRealColor { recyclerView.codeforcesItemsAdapter?.refreshHandles() }

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
            blogEntries.sortedByDescending { it.timeStamp }
                .map {
                    CodeforcesBlogEntry(
                        id = it.id,
                        title = it.title,
                        authorHandle = it.authorHandle,
                        authorColorTag = it.authorColorTag,
                        creationTimeSeconds = it.creationTimeSeconds,
                        commentsCount = 0,
                        rating = 0
                    )
                }
        }
}