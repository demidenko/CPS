package com.example.test3.news.codeforces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.CodeforcesTitle
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.news.codeforces.adapters.CodeforcesCommentsAdapter
import com.example.test3.news.codeforces.adapters.CodeforcesRecentBlogEntriesAdapter
import com.example.test3.ui.flowAdapter
import com.example.test3.ui.formatCPS
import com.example.test3.utils.CodeforcesBlogEntry
import kotlinx.coroutines.flow.map

class CodeforcesNewsRecentFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.RECENT

    private fun commentsFilteredAdapter(blogId: Int) =
        CodeforcesCommentsAdapter(
            this,
            newsFragment.newsViewModel.flowOfRecentActions(requireContext()).map { (_, comments) ->
                comments.filter {
                    it.blogEntry?.id == blogId
                }
            },
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

    private val blogEntryHeader: ConstraintLayout get() = requireView().findViewById(R.id.cf_news_page_header)
    private val headerCloseButton: ImageButton get() = blogEntryHeader.findViewById(R.id.recent_header_close)

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
        recyclerViewCommentsFiltered.flowAdapter = commentsFilteredAdapter(blogEntry.id)
        recyclerViewCommentsFiltered.isVisible = true
        headerInfo = blogEntry
        drawBlogEntryHeader()
        switchButton.isVisible = false
    }

    private fun closeCommentsByBlogEntry() {
        recyclerViewCommentsFiltered.flowAdapter = null
        recyclerViewCommentsFiltered.isVisible = false
        recyclerViewBlogEntries.isVisible = true
        headerInfo = null
        drawBlogEntryHeader()
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

        val blogEntriesAdapter = CodeforcesRecentBlogEntriesAdapter(
            this,
            newsFragment.newsViewModel.flowOfRecentActions(requireContext())
        )

        val commentsAdapter = CodeforcesCommentsAdapter(
            this,
            newsFragment.newsViewModel.flowOfRecentActions(requireContext()).map { it.second }
        )

        recyclerViewBlogEntries.formatCPS().apply {
            isVisible = true
            flowAdapter = blogEntriesAdapter.apply {
                setOnBlogSelectListener { openCommentsByBlogEntry(it) }
            }
        }

        recyclerViewComments.formatCPS().apply {
            isVisible = false
            flowAdapter = commentsAdapter
        }

        recyclerViewCommentsFiltered.formatCPS().apply {
            isVisible = false
        }

        swipeRefreshLayout.formatCPS().setOnRefreshListener { callReload() }

        switchButton.apply {
            setOnClickListener {
                if(recyclerViewBlogEntries.isVisible) openCommentsAll()
                else closeCommentsAll()
            }
        }

        headerCloseButton.apply {
            setOnClickListener {
                closeCommentsByBlogEntry()
            }
        }

        subscribeLoadingState(newsFragment.newsViewModel.flowOfPageLoadingState(title), swipeRefreshLayout)
        subscribeRefreshOnRealColor {
            recyclerViewBlogEntries.codeforcesItemsAdapter?.refreshHandles()
            recyclerViewComments.codeforcesItemsAdapter?.refreshHandles()
            recyclerViewCommentsFiltered.codeforcesItemsAdapter?.let {
                drawBlogEntryHeader()
                it.refreshHandles()
            }
        }
    }

}