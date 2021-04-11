package com.example.test3.news.codeforces

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.NewsFragment
import com.example.test3.R
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.ui.CPSFragment
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.utils.fromHTML
import kotlinx.coroutines.flow.flow

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

        view.findViewById<RecyclerView>(R.id.manage_cf_follow_user_blog_entries).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
            adapter = CodeforcesBlogEntriesAdapter(
                this@CodeforcesBlogEntriesFragment,
                makeUserBlogEntriesSingleFlow(getHandle(), requireContext()),
                null
            )
        }
    }

    private fun makeUserBlogEntriesSingleFlow(handle: String, context: Context) =
        flow {
            val blogEntries =
                CodeforcesAPI.getUserBlogEntries(handle, NewsFragment.getCodeforcesContentLanguage(context))
                    ?.result ?: emptyList()

            val authorColorTag = CodeforcesUtils.getRealColorTag(handle)

            emit(
                blogEntries.map { blogEntry ->
                    blogEntry.copy(
                        title = fromHTML(blogEntry.title.removeSurrounding("<p>", "</p>")).toString(),
                        authorColorTag = authorColorTag
                    )
                }
            )
        }
}