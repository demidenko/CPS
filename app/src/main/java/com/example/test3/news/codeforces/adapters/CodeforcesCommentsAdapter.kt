package com.example.test3.news.codeforces.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.getColorFromResource
import com.example.test3.makeIntentOpenUrl
import com.example.test3.timeDifference
import com.example.test3.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

class CodeforcesCommentsAdapter(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    dataFlow: Flow<List<CodeforcesRecentAction>>
): CodeforcesNewsItemsAdapter<CodeforcesCommentsAdapter.CodeforcesCommentViewHolder>() {

    init {
        lifecycleCoroutineScope.launchWhenStarted {
            dataFlow.collect { recentActions ->
                rows = recentActions.toTypedArray()
                notifyDataSetChanged()
            }
        }
    }

    class CodeforcesCommentViewHolder(val view: ConstraintLayout): RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val commentContent: TextView = view.findViewById(R.id.news_item_comment_content)
    }

    private var rows: Array<CodeforcesRecentAction> = emptyArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesCommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_comment, parent, false) as ConstraintLayout
        return CodeforcesCommentViewHolder(view)
    }

    var showTitle = true

    override fun onBindViewHolder(holder: CodeforcesCommentViewHolder, position: Int) {
        val recentAction = rows[position]
        val blogEntry = recentAction.blogEntry!!
        val comment = recentAction.comment!!

        with(holder){
            title.text = blogEntry.title

            author.text = codeforcesAccountManager.makeSpan(comment.commentatorHandle, comment.commentatorHandleColorTag)

            rating.apply{
                if(comment.rating == 0) isGone = true
                else {
                    isVisible = true
                    text = signedToString(comment.rating)
                    if (comment.rating > 0) {
                        setTextColor(getColorFromResource(context, R.color.blog_rating_positive))
                    } else {
                        setTextColor(getColorFromResource(context, R.color.blog_rating_negative))
                    }
                }
            }

            commentContent.text = CodeforcesUtils.fromCodeforcesHTML(comment.text)

            time.text = timeDifference(comment.creationTimeSeconds, getCurrentTimeSeconds())

            view.setOnClickListener {
                it.context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.comment(blogEntry.id,comment.id)))
            }

            title.isVisible = showTitle
            view.findViewById<TextView>(R.id.recent_arrow).isVisible = showTitle
        }
    }

    override fun getItemCount() = rows.size


}