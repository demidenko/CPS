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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.*
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class CodeforcesBlogEntriesAdapter(
    fragment: Fragment,
    dataFlow: Flow<List<CodeforcesBlogEntry>>,
    private val viewedBlogEntriesIdsFlow: Flow<Set<Int>>?,
    val clearNewEntriesOnDataChange: Boolean = true
): CodeforcesNewsItemsAdapter<CodeforcesBlogEntriesAdapter.CodeforcesBlogEntryViewHolder, List<CodeforcesBlogEntry>>(
    fragment, dataFlow
) {

    private var items: Array<CodeforcesBlogEntry> = emptyArray()
    override fun getItemCount() = items.size

    fun getBlogIDs() = items.map { it.id }

    private val newEntries = MutableSetLiveSize<Int>()
    fun getNewEntriesSizeFlow() = newEntries.sizeFlow

    override suspend fun applyData(data: List<CodeforcesBlogEntry>) {
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

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int, payloads: MutableList<Any>) {
        payloads.forEach {
            if(it is NEW_ENTRY) {
                val blogId = items[position].id
                holder.newEntryIndicator.isVisible = newEntries.contains(blogId)
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        with(holder){
            val info = items[position]

            val blogId = info.id
            view.setOnClickListener {
                if(newEntries.contains(blogId)){
                    newEntries.remove(blogId)
                    notifyItemChanged(position, NEW_ENTRY)
                }
                it.context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(blogId)))
            }

            view.isLongClickable = true
            view.setOnLongClickListener {
                val mainActivity = it.context!! as MainActivity
                runBlocking { CodeforcesNewsFollowWorker.isEnabled(mainActivity) }.apply {
                    if(this) addToFollowListWithSnackBar(this@with, mainActivity)
                }
            }

            title.text = info.title

            author.text = codeforcesAccountManager.makeSpan(info.authorHandle, info.authorColorTag)

            time.text = timeDifference(info.creationTimeSeconds, getCurrentTimeSeconds())

            newEntryIndicator.isVisible = newEntries.contains(blogId)

            commentsCount.text = info.commentsCount.toString()
            comments.isGone = info.commentsCount == 0

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

    override fun refreshHandles(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        val info = items[position]
        holder.author.text = codeforcesAccountManager.makeSpan(info.authorHandle, info.authorColorTag)
    }


    companion object {
        private object NEW_ENTRY

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
}
