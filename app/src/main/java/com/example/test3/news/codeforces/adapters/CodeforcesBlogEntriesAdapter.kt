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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.ui.HideShowLifecycleFragment
import com.example.test3.ui.TimeDepends
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant


class CodeforcesBlogEntriesAdapter(
    fragment: HideShowLifecycleFragment,
    dataFlow: Flow<List<CodeforcesBlogEntry>>,
    private val viewedBlogEntriesIdsFlow: Flow<Set<Int>>?,
    val clearNewEntriesOnDataChange: Boolean = true
): CodeforcesNewsItemsTimedAdapter<CodeforcesBlogEntriesAdapter.CodeforcesBlogEntryViewHolder, List<CodeforcesBlogEntry>>(
    fragment, dataFlow
) {

    private var items: Array<CodeforcesBlogEntry> = emptyArray()
    override fun getItemCount() = items.size

    fun getBlogIDs() = items.map { it.id }

    private val newEntries = MutableSetLiveSize<Int>()
    fun getNewEntriesSizeFlow() = newEntries.sizeStateFlow

    override suspend fun applyData(data: List<CodeforcesBlogEntry>): DiffUtil.DiffResult {
        val oldItems = items
        val oldNewEntries = newEntries.values()
        items = data.toTypedArray()
        manageNewEntries()
        return DiffUtil.calculateDiff(diffCallback(oldItems, items, oldNewEntries, newEntries.values()))
    }

    private suspend fun manageNewEntries() {
        val savedBlogEntries = viewedBlogEntriesIdsFlow?.first() ?: return
        val currentBlogEntries = getBlogIDs()
        if(clearNewEntriesOnDataChange) newEntries.clear()
        else {
            newEntries.removeAll(newEntries.values().filter { it !in currentBlogEntries })
        }
        val newBlogEntries = currentBlogEntries.filter { it !in savedBlogEntries }
        newEntries.addAll(newBlogEntries)
    }



    class CodeforcesBlogEntryViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view), TimeDepends {
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        private val time: TextView = view.findViewById(R.id.news_item_time)
        private val rating: TextView = view.findViewById(R.id.news_item_rating)
        private val commentsCount: TextView = view.findViewById(R.id.news_item_comments_count)
        private val comments: Group = view.findViewById(R.id.news_item_comments)
        private val newEntryIndicator: View = view.findViewById(R.id.news_item_dot_new)

        fun setAuthor(handle: String, colorTag: CodeforcesUtils.ColorTag, manager: CodeforcesAccountManager) {
            author.text = manager.makeSpan(handle, colorTag)
        }

        fun setNewEntryIndicator(isNew: Boolean) {
            newEntryIndicator.isVisible = isNew
        }

        fun setRating(rating: Int) {
            CodeforcesUtils.setVotedView(
                rating = rating,
                ratingTextView = this.rating,
                ratingGroupView = this.rating
            )
        }

        fun setComments(commentsCount: Int) {
            this.commentsCount.text = commentsCount.toString()
            comments.isGone = commentsCount == 0
        }

        override var startTimeSeconds: Long = 0
        override fun refreshTime(currentTime: Instant) {
            time.text = timeAgo(startTimeSeconds, currentTime.epochSeconds)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesBlogEntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as ConstraintLayout
        return CodeforcesBlogEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int, payloads: MutableList<Any>) {
        payloads.forEach {
            if(it is List<*>) {
                val blogEntry = items[position]
                it.forEach { obj ->
                    when(obj) {
                        is NEW_ENTRY -> holder.setNewEntryIndicator(blogEntry.id in newEntries)
                        is UPDATE_RATING -> holder.setRating(blogEntry.rating)
                        is UPDATE_COMMENTS -> holder.setComments(blogEntry.commentsCount)
                    }
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        with(holder){
            val blogEntry = items[position]

            val blogId = blogEntry.id
            view.setOnClickListener {
                if(blogId in newEntries){
                    newEntries.remove(blogId)
                    notifyItemChanged(bindingAdapterPosition, listOf(NEW_ENTRY))
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

            title.text = blogEntry.title

            setAuthor(blogEntry.authorHandle, blogEntry.authorColorTag, codeforcesAccountManager)
            setNewEntryIndicator(blogId in newEntries)
            setComments(blogEntry.commentsCount)
            setRating(blogEntry.rating)

            startTimeSeconds = blogEntry.creationTimeSeconds
            refreshTime(getCurrentTime())
        }
    }

    override fun refreshHandles(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        val blogEntry = items[position]
        holder.setAuthor(blogEntry.authorHandle, blogEntry.authorColorTag, codeforcesAccountManager)
    }

    companion object {
        private object NEW_ENTRY
        private object UPDATE_RATING
        private object UPDATE_COMMENTS

        private fun diffCallback(
            old: Array<CodeforcesBlogEntry>,
            new: Array<CodeforcesBlogEntry>,
            oldNewEntries: Set<Int>,
            newNewEntries: Set<Int>,
        ) =
            object : DiffUtil.Callback() {
                override fun getOldListSize() = old.size
                override fun getNewListSize() = new.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return old[oldItemPosition].id == new[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldBlogEntry = old[oldItemPosition]
                    val newBlogEntry = new[newItemPosition]
                    val id = newBlogEntry.id
                    return (oldBlogEntry == newBlogEntry) && (id in oldNewEntries == id in newNewEntries)
                }

                private val fields = setOf(
                    CodeforcesBlogEntry::rating,
                    CodeforcesBlogEntry::commentsCount,
                )
                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): List<Any>? {
                    val oldBlogEntry = old[oldItemPosition]
                    val newBlogEntry = new[newItemPosition]
                    val difference = classDifference(oldBlogEntry, newBlogEntry)
                    if(!fields.containsAll(difference)) return null
                    val res = mutableListOf<Any>()
                    if(CodeforcesBlogEntry::rating in difference) res.add(UPDATE_RATING)
                    if(CodeforcesBlogEntry::commentsCount in difference) res.add(UPDATE_COMMENTS)
                    val id = newBlogEntry.id
                    if(id in oldNewEntries != id in newNewEntries) res.add(NEW_ENTRY)
                    return res
                }

            }

        private fun addToFollowListWithSnackBar(holder: CodeforcesBlogEntryViewHolder, mainActivity: MainActivity){
            mainActivity.newsFragment.lifecycleScope.launch {
                val handle = holder.author.text
                when(mainActivity.newsFragment.newsViewModel.addToFollowList(handle.toString(), mainActivity)){
                    true -> {
                        Snackbar.make(holder.view, SpannableStringBuilder("You now followed ").append(handle), Snackbar.LENGTH_LONG).apply {
                            setAction("Manage"){
                                mainActivity.newsFragment.showCodeforcesFollowListManager()
                            }
                        }
                    }
                    false -> {
                        Snackbar.make(holder.view, SpannableStringBuilder("You already following ").append(handle), Snackbar.LENGTH_LONG)
                    }
                }.setAnchorView(mainActivity.navigation).show()
            }
        }
    }
}
