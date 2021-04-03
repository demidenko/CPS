package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test3.CodeforcesTitle
import com.example.test3.R
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter

class CodeforcesNewsTopFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.TOP
    private val itemsAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            this,
            newsFragment.newsViewModel.flowOfTopBlogEntries(),
            null
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

        subscribeLoadingState(newsFragment.newsViewModel.getPageLoadingStateFlow(title), swipeRefreshLayout)
        subscribeRefreshOnRealColor { itemsAdapter.refresh() }

        if(savedInstanceState == null) callReload()
    }

}