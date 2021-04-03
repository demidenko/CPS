package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test3.CodeforcesTitle
import com.example.test3.NewsFragment
import com.example.test3.R
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.news.codeforces.adapters.CodeforcesCommentsAdapter
import com.example.test3.utils.off
import com.example.test3.utils.on
import kotlinx.coroutines.launch

class CodeforcesNewsTopFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.TOP
    private val blogEntriesAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            this,
            newsFragment.newsViewModel.flowOfTopBlogEntries(),
            null
        )
    }
    private val commentsAdapter by lazy {
        CodeforcesCommentsAdapter(
            this,
            newsFragment.newsViewModel.flowOfTopComments()
        )
    }
    private var commentsAdapterCreated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.apply {
            adapter = blogEntriesAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        swipeRefreshLayout.apply {
            setOnRefreshListener { callReload() }
            setProgressBackgroundColorSchemeResource(R.color.backgroundAdditional)
            setColorSchemeResources(R.color.colorAccent)
        }

        newsFragment.topCommentsButton.apply {
            off()
            setOnClickListener {
                if(recyclerView.adapter == blogEntriesAdapter){
                    on()
                    recyclerView.adapter = commentsAdapter
                    if(!commentsAdapterCreated){
                        commentsAdapterCreated = true
                        lifecycleScope.launch {
                            newsFragment.newsViewModel.reloadTopComments(NewsFragment.getCodeforcesContentLanguage(requireContext()))
                        }
                    }
                } else {
                    off()
                    recyclerView.adapter = blogEntriesAdapter
                }
            }
        }

        subscribeLoadingState(newsFragment.newsViewModel.getPageLoadingStateFlow(title), swipeRefreshLayout)
        subscribeRefreshOnRealColor { blogEntriesAdapter.refresh() }

        if(savedInstanceState == null) callReload()
    }

}