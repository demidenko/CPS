package com.example.test3.news.codeforces.adapters

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.makeIntentOpenUrl
import com.example.test3.utils.CodeforcesBlogEntry
import com.example.test3.utils.CodeforcesComment
import com.example.test3.utils.CodeforcesRecentAction
import com.example.test3.utils.CodeforcesURLFactory
import kotlinx.coroutines.flow.Flow

class CodeforcesRecentBlogEntriesAdapter(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    dataFlow: Flow<Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>>
): CodeforcesNewsItemsAdapter<CodeforcesRecentBlogEntriesAdapter.CodeforcesRecentBlogEntryViewHolder,Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>>(
    lifecycleCoroutineScope, dataFlow
){

    private var items: Array<CodeforcesBlogEntry> = emptyArray()
    override fun getItemCount() = items.size

    private var commentsByBlogEntry = mapOf<Int, List<CodeforcesComment>>()
    private fun calculateCommentatorsSpans(blogId: Int): List<Spannable> {
        return commentsByBlogEntry[blogId]
            ?.distinctBy { it.commentatorHandle }
            ?.map { comment ->
                codeforcesAccountManager.makeSpan(
                    comment.commentatorHandle,
                    comment.commentatorHandleColorTag
                )
            } ?: emptyList()
    }

    override suspend fun applyData(data: Pair<List<CodeforcesBlogEntry>, List<CodeforcesRecentAction>>) {
        val (blogEntries, comments) = data
        commentsByBlogEntry = comments.groupBy({ it.blogEntry!!.id }){ it.comment!! }
        items = blogEntries.toTypedArray()
        notifyDataSetChanged()
    }

    class CodeforcesRecentBlogEntryViewHolder(val view: ConstraintLayout):  RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
        val commentsIcon: ImageView = view.findViewById(R.id.news_item_comment_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesRecentBlogEntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_item, parent, false) as ConstraintLayout
        return CodeforcesRecentBlogEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeforcesRecentBlogEntryViewHolder, position: Int) {
        val blogEntry = items[position]

        with(holder){
            view.setOnClickListener(makeItemClickListener(blogEntry.id))

            title.text =  blogEntry.title
            author.text = codeforcesAccountManager.makeSpan(blogEntry.authorHandle, blogEntry.authorColorTag)

            val commentators = calculateCommentatorsSpans(blogEntry.id)
            comments.text = commentators.joinTo(SpannableStringBuilder())
            commentsIcon.isGone = commentators.isEmpty()
        }

    }


    private fun makeItemClickListener(blogId: Int): View.OnClickListener {
        //remember to prevent update data while menu is open
        val lastComment = commentsByBlogEntry[blogId]?.firstOrNull()
        return View.OnClickListener { v ->
            val context = v.context
            if(lastComment == null){
                context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(blogId)))
            } else {
                buildPopupMenu(lastComment, blogId, v, context).show()
            }
        }
    }

    private fun buildPopupMenu(comment: CodeforcesComment, blogId: Int, anchor: View, context: Context): PopupMenu =
        PopupMenu(context, anchor, Gravity.CENTER_HORIZONTAL).apply {
            inflate(R.menu.cf_recent_item_open_variants)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                setForceShowIcon(true)
            }
            menu.findItem(R.id.cf_news_recent_item_menu_open_last_comment).let { item ->
                item.title = SpannableStringBuilder(item.title)
                    .append(" [")
                    .append(codeforcesAccountManager.makeSpan(comment.commentatorHandle,comment.commentatorHandleColorTag))
                    .append("]")
            }

            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.cf_news_recent_item_menu_open_blog -> {
                        context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(blogId)))
                        true
                    }
                    R.id.cf_news_recent_item_menu_open_last_comment -> {
                        context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.comment(blogId,comment.id)))
                        true
                    }
                    R.id.cf_news_recent_item_menu_show_comments -> {
                        //TODO("call open comments from blogEntry")
                        true
                    }
                    else -> false
                }
            }
        }

}
