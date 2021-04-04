package com.example.test3.news.codeforces.adapters

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.*
import com.example.test3.news.codeforces.CodeforcesNewsFragment
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.utils.MutableSetLiveSize
import com.example.test3.utils.signedToString
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class CodeforcesBlogEntriesAdapter(
    fragment: CodeforcesNewsFragment,
    dataFlow: Flow<List<BlogEntryInfo>>,
    private val viewedBlogEntriesIdsFlow: Flow<Set<Int>>?,
    val clearNewEntriesOnDataChange: Boolean = true
): CodeforcesNewsItemsAdapter<CodeforcesBlogEntriesAdapter.CodeforcesBlogEntryViewHolder, List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>>(
    fragment, dataFlow
) {

    data class BlogEntryInfo(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: CodeforcesUtils.ColorTag,
        val time: String,
        val comments: Int,
        val rating: Int
    )

    private var items: Array<BlogEntryInfo> = emptyArray()
    override fun getItemCount() = items.size

    private val newEntries = MutableSetLiveSize<Int>()
    fun getNewEntriesSize() = newEntries.sizeLiveData

    override suspend fun applyData(data: List<BlogEntryInfo>) {
        items = data.toTypedArray()
        manageNewEntries()
    }

    private suspend fun manageNewEntries() {
        val savedBlogEntries = viewedBlogEntriesIdsFlow?.first() ?: return
        val currentBlogEntries = getBlogIDs()
        if(clearNewEntriesOnDataChange) newEntries.clear()
        else {
            for(id in newEntries.values()) if(id !in currentBlogEntries) newEntries.remove(id)
        }
        val newBlogEntries = currentBlogEntries.filter { it !in savedBlogEntries }
        newEntries.addAll(newBlogEntries)
    }



    class CodeforcesBlogEntryViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val commentsCount: TextView = view.findViewById(R.id.news_item_comments_count)
        val comments: Group = view.findViewById(R.id.news_item_comments)
        val newEntryIndicator: View = view.findViewById(R.id.news_item_dot_new)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesBlogEntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as ConstraintLayout
        return CodeforcesBlogEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        with(holder){
            val info = items[position]

            val blogId = info.blogId
            view.setOnClickListener {
                it.context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(blogId)))
                if(newEntries.contains(blogId)){
                    newEntries.remove(blogId)
                    notifyItemChanged(position)
                }
            }

            view.isLongClickable = true
            view.setOnLongClickListener {
                val mainActivity = it.context!! as MainActivity
                runBlocking { CodeforcesNewsFollowWorker.isEnabled(mainActivity) }.apply {
                    if(this) addToFollowListWithSnackBar(this@with, mainActivity)
                }
            }

            title.text = info.title

            author.text = codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

            time.text = timeRUtoEN(info.time)

            newEntryIndicator.isVisible = newEntries.contains(blogId)

            commentsCount.text = info.comments.toString()
            comments.isGone = info.comments == 0

            rating.apply{
                if(info.rating==0) isVisible = false
                else {
                    isVisible = true
                    text = signedToString(info.rating)
                    setTextColor(getColorFromResource(context,
                        if(info.rating>0) R.color.blog_rating_positive
                        else R.color.blog_rating_negative
                    ))
                }
            }
        }
    }

    fun getBlogIDs() = items.map { it.blogId }


    private fun addToFollowListWithSnackBar(holder: CodeforcesBlogEntryViewHolder, mainActivity: MainActivity){
        mainActivity.newsFragment.lifecycleScope.launch {
            val connector = CodeforcesNewsFollowWorker.FollowDataConnector(mainActivity)
            val handle = holder.author.text
            when(connector.add(handle.toString())){
                true -> {
                    Snackbar.make(holder.view, SpannableStringBuilder("You now followed ").append(handle), Snackbar.LENGTH_LONG).apply {
                        setAction("Manage"){
                            mainActivity.newsFragment.showCodeforcesFollowListManager()
                        }
                    }
                }
                false -> {
                    Snackbar.make(holder.view, SpannableStringBuilder("You already followed ").append(handle), Snackbar.LENGTH_LONG)
                }
            }.setAnchorView(mainActivity.navigation).show()
        }
    }
}
