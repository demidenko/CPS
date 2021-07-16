package com.example.test3.news.codeforces

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.room.getFollowDao
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.flowAdapter
import com.example.test3.ui.formatCPS
import com.example.test3.ui.settingsUI
import com.example.test3.utils.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class CodeforcesBlogEntriesFragment: CPSFragment() {

    companion object {
        private const val keyHandle = "handle"
    }

    fun setHandle(handle: String) {
        requireArguments().putString(keyHandle, handle)
    }

    fun getHandle(): String  = requireArguments().getString(keyHandle,"")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cf_blog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::news.codeforces.blog"
        setBottomPanelId(R.id.support_navigation_empty, R.layout.navigation_empty)

        val blogEntriesAdapter = CodeforcesBlogEntriesAdapter(
            this,
            makeUserBlogEntriesSingleFlow(getHandle(), requireContext()),
            null
        )

        view.findViewById<RecyclerView>(R.id.manage_cf_follow_user_blog_entries).formatCPS().flowAdapter = blogEntriesAdapter

        launchAndRepeatWithViewLifecycle {
            mainActivity.settingsUI.flowOfUseRealColors().ignoreFirst().collect { blogEntriesAdapter.refreshHandles() }
        }
    }

    private fun makeUserBlogEntriesSingleFlow(handle: String, context: Context) =
        flow {
            val (blogEntries, authorColorTag) = asyncPair(
                { getFollowDao(context).loadBlogEntries(handle, context) },
                { CodeforcesUtils.getRealColorTag(handle) }
            )
            emit(
                blogEntries.map { blogEntry ->
                    blogEntry.copy(
                        title = fromHTML(blogEntry.title.removeSurrounding("<p>", "</p>")).toString(),
                        authorColorTag = authorColorTag
                    )
                }
            )
        }.stateIn(lifecycleScope, SharingStarted.Lazily, emptyList())
}