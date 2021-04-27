package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_top, container, false)
    }

    private val recyclerViewBlogEntries: RecyclerView get() = requireView().findViewById(R.id.cf_news_page_recyclerview_blog_entries)
    private val recyclerViewComments: RecyclerView get() = requireView().findViewById(R.id.cf_news_page_recyclerview_comments)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerViewBlogEntries.apply {
            isVisible = true
            adapter = blogEntriesAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        recyclerViewComments.apply {
            isVisible = false
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
                if(recyclerViewBlogEntries.isVisible){
                    on()
                    recyclerViewBlogEntries.isVisible = false
                    with(recyclerViewComments){
                        isVisible = true
                        if(adapter == null){
                            adapter = commentsAdapter
                            lifecycleScope.launch {
                                newsFragment.newsViewModel.reloadTopComments(NewsFragment.getCodeforcesContentLanguage(requireContext()))
                            }
                        }
                    }
                } else {
                    off()
                    recyclerViewComments.isVisible = false
                    recyclerViewBlogEntries.isVisible = true
                }
            }
        }

        subscribeLoadingState(newsFragment.newsViewModel.getPageLoadingStateFlow(title), swipeRefreshLayout)
        subscribeRefreshOnRealColor {
            recyclerViewBlogEntries.codeforcesItemsAdapter?.refreshHandles()
            recyclerViewComments.codeforcesItemsAdapter?.refreshHandles()
        }

        if(savedInstanceState == null) callReload()
    }

}