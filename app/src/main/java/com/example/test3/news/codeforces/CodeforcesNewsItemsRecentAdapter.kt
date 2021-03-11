package com.example.test3.news.codeforces

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.getColorFromResource
import com.example.test3.makeIntentOpenUrl
import com.example.test3.timeDifference
import com.example.test3.utils.*

class CodeforcesNewsItemsRecentAdapter: CodeforcesNewsItemsAdapter(){

    class BlogInfo(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: CodeforcesUtils.ColorTag,
        val lastCommentId: Long,
        var commentators: List<Spannable>
    )

    private var rows: Array<BlogInfo> = emptyArray()
    private var rowsComments: Array<CodeforcesRecentAction> = emptyArray()
    private val blogComments = mutableMapOf<Int, MutableList<CodeforcesComment>>()

    private fun calculateCommentatorsSpans(blogID: Int): List<Spannable> {
        return blogComments[blogID]
            ?.distinctBy { it.commentatorHandle }
            ?.map { comment ->
                activity.accountsFragment.codeforcesAccountManager.makeSpan(
                    comment.commentatorHandle,
                    comment.commentatorHandleColorTag
                )
            } ?: emptyList()
    }

    override fun beforeRefresh() {
        showHeader()
        rows.forEachIndexed { index, blogInfo ->
            rows[index].commentators = calculateCommentatorsSpans(blogInfo.blogId)
        }
    }

    override suspend fun parseData(s: String): Boolean {
        val (blogs, comments) = CodeforcesUtils.parseRecentActionsPage(s)

        blogComments.clear()
        comments.forEach { recentAction ->
            blogComments.getOrPut(recentAction.blogEntry!!.id){ mutableListOf() }.add(recentAction.comment!!)
        }

        val res = blogs.map { blog ->
            BlogInfo(blog.id, blog.title, blog.authorHandle, blog.authorColorTag,
                lastCommentId = blogComments[blog.id]?.first()?.id ?: -1,
                commentators = calculateCommentatorsSpans(blog.id)
            )
        }

        if(res.isNotEmpty()){
            rows = res.toTypedArray()
            rowsComments = comments.toTypedArray()
            return true
        }
        return false
    }

    class CodeforcesNewsBlogItemViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
        val commentsIcon: ImageView = view.findViewById(R.id.news_item_comment_icon)
    }

    class CodeforcesNewsCommentItemViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val comment: TextView = view.findViewById(R.id.news_item_comment_content)
    }

    override fun getItemCount(): Int {
        return headerBlog?.run {
            rowsComments.count { recentAction ->
                recentAction.blogEntry!!.id == blogId
            }
        } ?: if (modeGrouped) rows.size else rowsComments.size
    }

    private var headerBlog: BlogInfo? = null
    private lateinit var header: View
    private val switchButton: ImageButton by lazy { activity.navigation.findViewById(R.id.navigation_news_recent_swap) }
    private val showBackButton: ImageButton by lazy { activity.navigation.findViewById(R.id.navigation_news_recent_show_blog_back) }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        with(recyclerView.parent.parent as ConstraintLayout){
            header = findViewById(R.id.cf_news_page_header)
        }
    }

    private fun showHeader(){
        if(headerBlog!=null) header.apply {
            val info = headerBlog!!
            findViewById<TextView>(R.id.news_item_title).text = info.title
            findViewById<TextView>(R.id.news_item_author).text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)
            visibility = View.VISIBLE
        } else {
            header.visibility = View.GONE
        }
    }

    fun showFromBlog(info: BlogInfo){
        switchButton.visibility = View.GONE
        showBackButton.visibility = View.VISIBLE
        headerBlog = info
        showHeader()
        notifyDataSetChanged()
        recyclerView.scrollToPosition(0)
    }
    fun closeShowFromBlog(){
        showBackButton.visibility = View.GONE
        switchButton.visibility = View.VISIBLE
        headerBlog = null
        showHeader()
        notifyDataSetChanged()
    }

    private var modeGrouped = true
    fun switchMode(){
        modeGrouped = !modeGrouped
        if(modeGrouped) switchButton.setImageResource(R.drawable.ic_recent_mode_comments)
        else switchButton.setImageResource(R.drawable.ic_recent_mode_grouped)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (headerBlog!=null || !modeGrouped) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_comment, parent, false) as ConstraintLayout
            return CodeforcesNewsCommentItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_item, parent, false) as ConstraintLayout
            return CodeforcesNewsBlogItemViewHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (headerBlog!=null || !modeGrouped) 1 else 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is CodeforcesNewsBlogItemViewHolder){
            onBindViewBlogHolder(holder, position)
        }else
            if(holder is CodeforcesNewsCommentItemViewHolder){
                onBindViewCommentHolder(holder, position)
            }
    }

    private fun onBindViewBlogHolder(holder: CodeforcesNewsBlogItemViewHolder, position: Int){
        val info = rows[position]

        holder.view.setOnClickListener {
            if(info.commentators.isEmpty()){
                activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(info.blogId)))
                return@setOnClickListener
            }

            PopupMenu(activity, holder.title, Gravity.CENTER_HORIZONTAL).apply {
                inflate(R.menu.cf_recent_item_open_variants)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    setForceShowIcon(true)
                }
                menu.findItem(R.id.cf_news_recent_item_menu_open_last_comment).let { item ->
                    item.title = SpannableStringBuilder(item.title)
                        .append(" [")
                        .append(info.commentators.first())
                        .append("]")
                }

                setOnMenuItemClickListener {
                    when(it.itemId){
                        R.id.cf_news_recent_item_menu_open_blog -> {
                            activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(info.blogId)))
                            true
                        }
                        R.id.cf_news_recent_item_menu_open_last_comment -> {
                            activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.comment(info.blogId,info.lastCommentId)))
                            true
                        }
                        R.id.cf_news_recent_item_menu_show_comments -> {
                            showFromBlog(info)
                            true
                        }
                        else -> false
                    }
                }

                show()
            }
        }

        holder.title.text =  info.title

        holder.author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

        holder.comments.text = info.commentators.joinTo(SpannableStringBuilder())

        holder.commentsIcon.visibility = if(info.commentators.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun onBindViewCommentHolder(holder: CodeforcesNewsCommentItemViewHolder, position: Int){
        val recentAction =
            headerBlog?.run { rowsComments.filter { it.blogEntry!!.id == blogId }[position] }
                ?: rowsComments[position]

        val blogEntry = recentAction.blogEntry!!
        val comment = recentAction.comment!!

        holder.title.text = blogEntry.title

        holder.author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(comment.commentatorHandle, comment.commentatorHandleColorTag)

        holder.rating.apply{
            if(comment.rating == 0) visibility = View.GONE
            else {
                visibility = View.VISIBLE
                text = signedToString(comment.rating)
                if (comment.rating > 0) {
                    setTextColor(getColorFromResource(activity, R.color.blog_rating_positive))
                } else {
                    setTextColor(getColorFromResource(activity, R.color.blog_rating_negative))
                }
            }
        }

        holder.comment.text = CodeforcesUtils.fromCodeforcesHTML(comment.text)

        holder.time.text = timeDifference(comment.creationTimeSeconds, getCurrentTimeSeconds())

        holder.view.setOnClickListener {
            activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.comment(blogEntry.id,comment.id)))
        }

        if(headerBlog!=null){
            holder.title.visibility = View.GONE
            holder.view.findViewById<TextView>(R.id.recent_arrow).visibility = View.GONE
        }else{
            holder.title.visibility = View.VISIBLE
            holder.view.findViewById<TextView>(R.id.recent_arrow).visibility = View.VISIBLE
        }
    }

}
