package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import com.example.test3.CodeforcesTitle
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.ui.flowAdapter
import com.example.test3.ui.formatCPS
import com.google.android.material.tabs.TabLayout

class CodeforcesNewsMainFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.MAIN

    private lateinit var itemsAdapter: CodeforcesBlogEntriesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        itemsAdapter = CodeforcesBlogEntriesAdapter(
            this,
            newsFragment.newsViewModel.flowOfMainBlogEntries(requireContext()),
            newsFragment.viewedDataStore.flowOfViewedBlogEntries(title)
        )

        recyclerView.formatCPS().flowAdapter = itemsAdapter

        swipeRefreshLayout.formatCPS().setOnRefreshListener { callReload() }

        subscribeLoadingState(newsFragment.newsViewModel.flowOfPageLoadingState(title), swipeRefreshLayout)
        subscribeNewEntries(itemsAdapter)
        subscribeRefreshOnRealColor { itemsAdapter.refreshHandles() }

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

}