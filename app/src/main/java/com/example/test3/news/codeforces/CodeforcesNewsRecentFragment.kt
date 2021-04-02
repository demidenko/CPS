package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.CodeforcesTitle
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.news.codeforces.adapters.CodeforcesCommentsAdapter
import com.example.test3.news.codeforces.adapters.CodeforcesRecentBlogEntriesAdapter
import com.example.test3.utils.CodeforcesBlogEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CodeforcesNewsRecentFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.RECENT

    private val blogEntriesAdapter by lazy {
        CodeforcesRecentBlogEntriesAdapter(
            this,
            newsFragment.newsViewModel.getRecentActionsData()
        )
    }

    private val commentsAdapter by lazy {
        CodeforcesCommentsAdapter(
            this,
            newsFragment.newsViewModel.getRecentActionsData().map { it.second }.distinctUntilChanged()
        )
    }

    private fun commentsFilteredAdapter(blogId: Int) =
        CodeforcesCommentsAdapter(
            this,
            newsFragment.newsViewModel.getRecentActionsData().map { (_, comments) ->
                comments.filter {
                    it.blogEntry?.id == blogId
                }
            }.distinctUntilChanged(),
            showTitle = false
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_recent, container, false)
    }

    private val recyclerViewBlogEntries: RecyclerView get() = requireView().findViewById(R.id.cf_news_page_recyclerview_blog_entries)
    private val recyclerViewComments: RecyclerView get() = requireView().findViewById(R.id.cf_news_page_recyclerview_comments)
    private val recyclerViewCommentsFiltered: RecyclerView get() = requireView().findViewById(R.id.cf_news_page_recyclerview_comments_filtered)

    private val switchButton get() = newsFragment.recentSwitchButton
    private val backButton get() = newsFragment.recentShowBackButton

    private val blogEntryHeader: ConstraintLayout get() = requireView().findViewById(R.id.cf_news_page_header)
    private var headerInfo: CodeforcesBlogEntry? = null
    private fun drawBlogEntryHeader() {
        blogEntryHeader.apply {
            when(val info = headerInfo){
                null -> isVisible = false
                else -> {
                    findViewById<TextView>(R.id.news_item_title).text = info.title
                    findViewById<TextView>(R.id.news_item_author).text = CodeforcesAccountManager(requireContext()).makeSpan(info.authorHandle, info.authorColorTag)
                    isVisible = true
                }
            }
        }
    }

    private fun openCommentsByBlogEntry(blogEntry: CodeforcesBlogEntry) {
        recyclerViewBlogEntries.isVisible = false
        recyclerViewCommentsFiltered.adapter = commentsFilteredAdapter(blogEntry.id)
        recyclerViewCommentsFiltered.isVisible = true
        headerInfo = blogEntry
        drawBlogEntryHeader()
        switchButton.isVisible = false
        backButton.isVisible = true
    }

    private fun closeCommentsByBlogEntry() {
        recyclerViewCommentsFiltered.adapter = null
        recyclerViewCommentsFiltered.isVisible = false
        recyclerViewBlogEntries.isVisible = true
        headerInfo = null
        drawBlogEntryHeader()
        backButton.isVisible = false
        switchButton.isVisible = true
    }

    private fun openCommentsAll() {
        recyclerViewBlogEntries.isVisible = false
        recyclerViewComments.isVisible = true
        switchButton.setImageResource(R.drawable.ic_recent_mode_grouped)
    }

    private fun closeCommentsAll() {
        recyclerViewComments.isVisible = false
        recyclerViewBlogEntries.isVisible = true
        switchButton.setImageResource(R.drawable.ic_recent_mode_comments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerViewBlogEntries.apply {
            adapter = blogEntriesAdapter.apply {
                setOnBlogSelectListener { openCommentsByBlogEntry(it) }
            }
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        recyclerViewComments.apply {
            isVisible = false
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        recyclerViewCommentsFiltered.apply {
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

        switchButton.apply {
            setOnClickListener {
                if(recyclerViewBlogEntries.isVisible) openCommentsAll()
                else closeCommentsAll()
            }
        }

        backButton.apply {
            setOnClickListener {
                closeCommentsByBlogEntry()
            }
        }

        subscribeLoadingState(newsFragment.newsViewModel.getPageLoadingStateFlow(title), swipeRefreshLayout)
        subscribeRefreshOnRealColor {
            blogEntriesAdapter.refresh()
            commentsAdapter.refresh()
            recyclerViewCommentsFiltered.adapter?.let {
                drawBlogEntryHeader()
                (it as CodeforcesCommentsAdapter).refresh()
            }
        }

        if(savedInstanceState == null) callReload()
    }

}