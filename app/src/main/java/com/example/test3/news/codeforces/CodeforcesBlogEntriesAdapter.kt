package com.example.test3.news.codeforces

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.*
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.utils.MutableSetLiveSize
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


open class CodeforcesBlogEntriesAdapter(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    dataFlow: Flow<List<BlogEntryInfo>>,
    private val viewedBlogEntriesIdsFlow: Flow<Set<Int>>,
    val isManagesNewEntries: Boolean,
    val clearNewEntriesOnDataChange: Boolean
    ):
    CodeforcesNewsItemsAdapter<CodeforcesBlogEntriesAdapter.CodeforcesBlogEntryViewHolder>()
{

    data class BlogEntryInfo(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: CodeforcesUtils.ColorTag,
        val time: String,
        val comments: String,
        val rating: String
    )

    init {
        lifecycleCoroutineScope.launchWhenStarted {
            dataFlow.collect { blogEntries ->
                rows = blogEntries.toTypedArray()
                manageNewEntries()
                notifyDataSetChanged()
            }
        }
    }

    private suspend fun manageNewEntries() {
        if(!isManagesNewEntries) return
        val currentBlogs = getBlogIDs()
        if(clearNewEntriesOnDataChange) newEntries.clear()
        else {
            for(id in newEntries.values()) if(id !in currentBlogs) newEntries.remove(id)
        }
        val savedBlogs = viewedBlogEntriesIdsFlow.first()
        val newBlogs = currentBlogs.filter { it !in savedBlogs }
        newEntries.addAll(newBlogs)
    }



    class CodeforcesBlogEntryViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
        val commentsIcon: ImageView = view.findViewById(R.id.news_item_comment_icon)
        val newEntryIndicator: View = view.findViewById(R.id.news_item_dot_new)
    }

    private var rows: Array<BlogEntryInfo> = emptyArray()

    override fun getItemCount() = rows.size

    private val newEntries = MutableSetLiveSize<Int>()
    fun getNewEntriesSize() = newEntries.sizeLiveData

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesBlogEntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as ConstraintLayout
        return CodeforcesBlogEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        with(holder){
            val info = rows[position]

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

            comments.text = info.comments
            commentsIcon.isGone = info.comments.isEmpty()

            rating.apply{
                text = info.rating
                setTextColor(getColorFromResource(context,
                    if(info.rating.startsWith('+')) R.color.blog_rating_positive
                    else R.color.blog_rating_negative
                ))
            }
        }
    }

    fun getBlogIDs() = rows.map { it.blogId }


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
