package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test3.CodeforcesTitle
import com.example.test3.R
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.google.android.material.tabs.TabLayout

class CodeforcesNewsMainFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.MAIN

    private lateinit var itemsAdapter: CodeforcesBlogEntriesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        itemsAdapter = CodeforcesBlogEntriesAdapter(
            this,
            newsFragment.newsViewModel.flowOfMainBlogEntries(requireContext()),
            newsFragment.viewedDataStore.blogsViewedFlow(title)
        )

        recyclerView.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        swipeRefreshLayout.apply {
            setOnRefreshListener { callReload() }
            setProgressBackgroundColorSchemeResource(R.color.backgroundAdditional)
            setColorSchemeResources(R.color.colorAccent)
        }

        subscribeLoadingState(newsFragment.newsViewModel.getPageLoadingStateFlow(title), swipeRefreshLayout)
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