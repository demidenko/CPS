package com.example.test3.news.codeforces

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.getColorFromResource
import com.example.test3.makeIntentOpenUrl
import com.example.test3.timeRUtoEN
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.utils.MutableSetLiveSize
import com.example.test3.utils.fromHTML
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


open class CodeforcesNewsItemsClassicAdapter: CodeforcesNewsItemsAdapter(){

    data class Info(
        val blogId: Int,
        val title: String,
        val author: String,
        val authorColorTag: CodeforcesUtils.ColorTag,
        val time: String,
        val comments: String,
        val rating: String
    )

    override suspend fun parseData(s: String): Boolean {
        val res = arrayListOf<Info>()
        var i = 0
        while (true) {
            i = s.indexOf("<div class=\"topic\"", i + 1)
            if (i == -1) break

            val title = fromHTML(s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i))).toString()

            i = s.indexOf("entry/", i)
            val id = s.substring(i+6, s.indexOf('"',i)).toInt()

            i = s.indexOf("<div class=\"info\"", i)
            i = s.indexOf("/profile/", i)
            val author = s.substring(i+9,s.indexOf('"',i))

            i = s.indexOf("rated-user user-",i)
            val authorColorTag = CodeforcesUtils.ColorTag.fromString(
                s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))
            )

            i = s.indexOf("<span class=\"format-humantime\"", i)
            val time = s.substring(s.indexOf('>',i)+1, s.indexOf("</span>",i))

            i = s.indexOf("<div class=\"roundbox meta\"", i)
            i = s.indexOf("</span>", i)
            val rating = s.substring(s.lastIndexOf('>',i-1)+1,i)

            i = s.indexOf("<div class=\"right-meta\">", i)
            i = s.indexOf("</ul>", i)
            i = s.lastIndexOf("</a>", i)
            val comments = s.substring(s.lastIndexOf('>',i-1)+1,i).trim()

            res.add(Info(id,title,author,authorColorTag,time,comments,rating))
        }

        if(res.isNotEmpty()){
            rows = res.toTypedArray()
            return true
        }

        return false
    }


    class CodeforcesNewsItemViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
        val commentsIcon: ImageView = view.findViewById(R.id.news_item_comment_icon)
        val newEntryIndicator: View = view.findViewById(R.id.news_item_dot_new)
    }

    protected var rows: Array<Info> = emptyArray()

    override fun getItemCount() = rows.size


    private val newEntries = MutableSetLiveSize<Int>()
    fun getNewEntriesCountLiveData() = newEntries.size
    fun addNewEntries(entries: Collection<Int>) = newEntries.addAll(entries)
    fun clearNewEntries() = newEntries.clear()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as ConstraintLayout
        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as CodeforcesNewsItemViewHolder){
            val info = rows[position]

            val blogId = info.blogId
            view.setOnClickListener {
                activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(blogId)))
                if(newEntries.contains(blogId)){
                    newEntries.remove(blogId)
                    notifyItemChanged(position)
                }
            }

            view.isLongClickable = true
            view.setOnLongClickListener {
                runBlocking { CodeforcesNewsFollowWorker.isEnabled(activity) }.apply {
                    if(this) addToFollowListWithSnackBar(this@with)
                }
            }

            title.text = info.title

            author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

            time.text = timeRUtoEN(info.time)

            newEntryIndicator.visibility = if(newEntries.contains(blogId)) View.VISIBLE else View.GONE

            comments.text = info.comments
            commentsIcon.visibility = if(info.comments.isEmpty()) View.INVISIBLE else View.VISIBLE

            rating.apply{
                text = info.rating
                setTextColor(getColorFromResource(activity,
                    if(info.rating.startsWith('+')) R.color.blog_rating_positive
                    else R.color.blog_rating_negative
                ))
            }
        }
    }

    override fun getBlogIDs(): List<String> = rows.map { it.blogId.toString() }


    private fun addToFollowListWithSnackBar(holder: CodeforcesNewsItemViewHolder){
        activity.newsFragment.lifecycleScope.launch {
            val connector = CodeforcesNewsFollowWorker.FollowDataConnector(activity)
            val handle = holder.author.text
            when(connector.add(handle.toString())){
                true -> {
                    Snackbar.make(holder.view, SpannableStringBuilder("You now followed ").append(handle), Snackbar.LENGTH_LONG).apply {
                        setAction("Manage"){
                            activity.newsFragment.showCodeforcesFollowListManager()
                        }
                    }
                }
                false -> {
                    Snackbar.make(holder.view, SpannableStringBuilder("You already followed ").append(handle), Snackbar.LENGTH_LONG)
                }
            }.setAnchorView(activity.navigation).show()
        }
    }
}
