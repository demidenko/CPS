package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test3.CodeforcesTitle
import com.example.test3.R
import com.example.test3.news.codeforces.adapters.CodeforcesRecentActionsAdapter

class CodeforcesNewsRecentFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.RECENT
    private val itemsAdapter by lazy {
        CodeforcesRecentActionsAdapter(
            lifecycleScope,
            newsFragment.newsViewModel.getRecentActionsData()
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

        newsFragment.recentSwitchButton.setOnClickListener { itemsAdapter.switchMode() }
        newsFragment.recentShowBackButton.setOnClickListener { itemsAdapter.closeShowFromBlog() }

        subscribeLoadingState(newsFragment.newsViewModel.getPageLoadingStateFlow(title), swipeRefreshLayout)
        subscribeRefreshOnRealColor { itemsAdapter.refresh() }

        if(savedInstanceState == null) callReload()
    }

}