package com.example.test3.news.codeforces.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.makeIntentOpenUrl
import com.example.test3.timeDifference
import com.example.test3.utils.*
import kotlinx.coroutines.flow.Flow

class CodeforcesCommentsAdapter(
    fragment: Fragment,
    dataFlow: Flow<List<CodeforcesRecentAction>>,
    private val showTitle: Boolean = true
): CodeforcesNewsItemsTimedAdapter<CodeforcesCommentsAdapter.CodeforcesCommentViewHolder,List<CodeforcesRecentAction>>(
    fragment, dataFlow
) {

    private var items: Array<CodeforcesRecentAction> = emptyArray()
    override fun getItemCount() = items.size

    override suspend fun applyData(data: List<CodeforcesRecentAction>): DiffUtil.DiffResult {
        val newItems = data.toTypedArray()
        return DiffUtil.calculateDiff(diffCallback(items, newItems)).also {
            items = newItems
        }
    }

    class CodeforcesCommentViewHolder(val view: ConstraintLayout): RecyclerView.ViewHolder(view), TimeDepends {
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        private val time: TextView = view.findViewById(R.id.news_item_time)
        private val rating: TextView = view.findViewById(R.id.news_item_rating)
        val commentContent: TextView = view.findViewById(R.id.news_item_comment_content)
        val titleWithArrow: Group = view.findViewById(R.id.news_item_title_with_arrow)

        fun setRating(rating: Int) {
            this.rating.apply {
                if(rating == 0) isGone = true
                else {
                    isVisible = true
                    text = signedToString(rating)
                    setTextColor(getColorFromResource(context,
                        if (rating > 0) R.color.blog_rating_positive
                        else R.color.blog_rating_negative
                    ))
                }
            }
        }

        override var startTimeSeconds: Long = 0
        override fun refreshTime(currentTimeSeconds: Long) {
            time.text = timeDifference(startTimeSeconds, currentTimeSeconds)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesCommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_comment, parent, false) as ConstraintLayout
        return CodeforcesCommentViewHolder(view)
    }


    override fun onBindViewHolder(holder: CodeforcesCommentViewHolder, position: Int) {
        val recentAction = items[position]
        val blogEntry = recentAction.blogEntry!!
        val comment = recentAction.comment!!

        with(holder){
            title.text = blogEntry.title

            author.text = codeforcesAccountManager.makeSpan(comment.commentatorHandle, comment.commentatorHandleColorTag)

            setRating(comment.rating)

            commentContent.text = CodeforcesUtils.fromCodeforcesHTML(comment.text)

            startTimeSeconds = comment.creationTimeSeconds
            refreshTime(getCurrentTimeSeconds())

            view.setOnClickListener {
                it.context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.comment(blogEntry.id,comment.id)))
            }

            titleWithArrow.isVisible = showTitle
        }
    }

    override fun onBindViewHolder(
        holder: CodeforcesCommentViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        payloads.forEach {
            if(it is UPDATE_RATING) {
                val recentAction = items[position]
                val comment = recentAction.comment!!
                holder.setRating(comment.rating)
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun refreshHandles(holder: CodeforcesCommentViewHolder, position: Int) {
        val recentAction = items[position]
        val comment = recentAction.comment!!
        holder.author.text = codeforcesAccountManager.makeSpan(comment.commentatorHandle, comment.commentatorHandleColorTag)
    }

    companion object {
        private object UPDATE_RATING

        private fun diffCallback(old: Array<CodeforcesRecentAction>, new: Array<CodeforcesRecentAction>) =
            object : DiffUtil.Callback() {
                override fun getOldListSize() = old.size
                override fun getNewListSize() = new.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return old[oldItemPosition].comment!!.id == new[newItemPosition].comment!!.id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return old[oldItemPosition] == new[newItemPosition]
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    val oldComment = old[oldItemPosition].comment!!
                    val newComment = new[newItemPosition].comment!!
                    if(oldComment.text != newComment.text) return null
                    if(oldComment.commentatorHandle != newComment.commentatorHandle) return null
                    if(oldComment.commentatorHandleColorTag != newComment.commentatorHandleColorTag) return null
                    if(oldComment.rating != newComment.rating) return UPDATE_RATING
                    return null
                }
            }
    }
}