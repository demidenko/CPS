package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test3.CodeforcesTitle
import com.example.test3.R
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.utils.LoadingState
import com.google.android.material.tabs.TabLayout

class CodeforcesNewsMainFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.MAIN
    private val itemsAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            lifecycleScope,
            newsFragment.newsViewModel.getBlogEntriesMain(),
            newsFragment.viewedDataStore.blogsViewedFlow(title),
            isManagesNewEntries = true,
            clearNewEntriesOnDataChange = true
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        newsFragment.newsViewModel.getPageLoadingStateLiveData(title).observe(viewLifecycleOwner){ loadingState ->
            swipeRefreshLayout.isRefreshing = loadingState == LoadingState.LOADING
        }

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

}