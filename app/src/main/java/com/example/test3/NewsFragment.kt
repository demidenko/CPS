package com.example.test3

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.job_services.CodeforcesNewsLostRecentJobService
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


class NewsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    private val codeforcesNewsAdapter: CodeforcesNewsAdapter by lazy { CodeforcesNewsAdapter(this) }
    private lateinit var tabLayout: TabLayout
    private lateinit var codeforcesNewsViewPager: ViewPager2
    private lateinit var buttonReload: Button

    fun refresh(){
        try {
            codeforcesNewsAdapter.fragments.forEach { it.refresh() }
        }catch (e: UninitializedPropertyAccessException){

        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        codeforcesNewsViewPager = view.findViewById<ViewPager2>(R.id.cf_news_pager).apply {
            adapter = codeforcesNewsAdapter
            offscreenPageLimit = codeforcesNewsAdapter.fragments.size - 1
        }


        tabLayout = view.findViewById(R.id.cf_news_tab_layout)
        TabLayoutMediator(tabLayout, codeforcesNewsViewPager) { tab, position ->
            val fragment = codeforcesNewsAdapter.fragments[position]
            val title = fragment.title
            tab.text = title
            //tab.setIcon(R.drawable.ic_cf_logo)
            //tab.setCustomView(R.layout.cf_news_tab_layout)
            //tab.customView!!.findViewById<TextView>(R.id.cf_news_tab_title).text = title
            if(fragment is CodeforcesNewsMainFragment){
                tab.orCreateBadge.apply {
                    backgroundColor = resources.getColor(android.R.color.holo_green_light, null)
                    isVisible = false
                }
            }
            /*tab.view.setOnLongClickListener {
                Snackbar.make(view, title, Snackbar.LENGTH_SHORT).show()
                true
            }*/
        }.attach()



        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.run{
                    val fragment = codeforcesNewsAdapter.fragments[position]
                    if(fragment is CodeforcesNewsMainFragment) badge?.run{
                        if(hasNumber()){
                            isVisible = false
                            clearNumber()
                        }
                    }
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.run{
                    val fragment = codeforcesNewsAdapter.fragments[position]
                    if(fragment is CodeforcesNewsMainFragment) badge?.run{
                        if(hasNumber()){
                            fragment.save()
                        }
                    }
                }
            }

        })


        buttonReload = view.findViewById<Button>(R.id.button_reload_cf_news).apply {
            setOnClickListener { button -> button as Button
                button.text = "..."
                button.isEnabled = false
                activity.scope.launch {
                    val rx = codeforcesNewsAdapter.fragments.map { it.address }.toSet().map { it to async { if(it.isNotEmpty()) readURLData(it) else "" } }.toMap()
                    codeforcesNewsAdapter.fragments.mapIndexed { index, fragment ->
                        val tab = tabLayout.getTabAt(index)!!
                        launch {
                            if(tab.isSelected || System.currentTimeMillis()-fragment.lastReloadTime>1000*60) {
                                tab.text = "..."
                                fragment.reload(rx)
                                tab.text = fragment.title
                                if (fragment is CodeforcesNewsMainFragment && fragment.newBlogs.isNotEmpty()) {
                                    tab.badge?.apply {
                                        number = fragment.newBlogs.size
                                        isVisible = true
                                    }
                                    if (tab.isSelected) fragment.save()
                                }
                            }
                        }
                    }.joinAll()
                    button.isEnabled = true
                    button.text = "RELOAD"
                }
            }
        }

    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val defaultTabIndex = 1
        tabLayout.selectTab(tabLayout.getTabAt(defaultTabIndex))
        codeforcesNewsViewPager.setCurrentItem(defaultTabIndex, false)
        buttonReload.callOnClick()
    }


}



class CodeforcesNewsAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    val fragments = arrayOf(
        CodeforcesNewsMainFragment("CF MAIN", "https://codeforces.com/?locale=ru"),
        CodeforcesNewsFragment("CF TOP", "https://codeforces.com/top?locale=ru"),
        CodeforcesNewsRecentFragment("CF RECENT", "https://codeforces.com/recent-actions?locale=ru"),
        CodeforcesNewsLostRecentFragment("CF LOST")
    )

    override fun createFragment(position: Int): Fragment = fragments[position]
    override fun getItemCount(): Int = fragments.size
}



open class CodeforcesNewsFragment(val title: String, val address: String) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_page, container, false)
    }

    protected lateinit var viewAdapter: CodeforcesNewsItemsAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewAdapter = CodeforcesNewsItemsClassicAdapter(requireActivity() as MainActivity)

        view.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    var lastReloadTime = 0L

    open suspend fun reload(data: Map<String,Deferred<String?>>) {
        val s = data[address]?.await() ?: return
        viewAdapter.parseData(s)
        lastReloadTime = System.currentTimeMillis()
    }

    fun refresh(){
        viewAdapter.notifyDataSetChanged()
    }

}

open class CodeforcesNewsMainFragment(title: String, address: String) : CodeforcesNewsFragment(title, address) {

    companion object {
        const val CODEFORCES_NEWS_VIEWED = "codeforces_news_viewed"
    }

    private val prefs: SharedPreferences by lazy { requireActivity().getSharedPreferences(CODEFORCES_NEWS_VIEWED, Context.MODE_PRIVATE) }
    private val prefs_key: String = this::class.java.simpleName

    var newBlogs = hashSetOf<String>()

    override suspend fun reload(data: Map<String, Deferred<String?>>) {
        super.reload(data)

        val savedBlogs = prefs.getStringSet(prefs_key, null) ?: emptySet()
        newBlogs = viewAdapter.getBlogIDs().filter { !savedBlogs.contains(it) }.toHashSet()
    }

    fun save() {
        with(prefs.edit()) {
            val toSave = viewAdapter.getBlogIDs().toSet()
            putStringSet(prefs_key, toSave)
            apply()
        }
        (viewAdapter as? CodeforcesNewsItemsClassicAdapter)?.showNew(newBlogs)
    }
}



class CodeforcesNewsRecentFragment(title: String, address: String) : CodeforcesNewsFragment(title, address) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewAdapter = CodeforcesNewsItemsRecentAdapter(requireActivity() as MainActivity)

        view.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

}

class CodeforcesNewsLostRecentFragment(title: String) : CodeforcesNewsMainFragment(title, "") {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewAdapter = CodeforcesNewsItemsLostRecentAdapter(requireActivity() as MainActivity)

        view.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }
}


///---------------data adapters--------------------

abstract class CodeforcesNewsItemsAdapter(val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    abstract fun parseData(s: String)
    abstract fun getBlogIDs(): List<String>
}

open class CodeforcesNewsItemsClassicAdapter(activity: MainActivity): CodeforcesNewsItemsAdapter(activity){

    data class Info(
        val blogID: String,
        val title: String,
        val author: String,
        val authorColorTag: String,
        val time: String,
        val comments: String,
        val rating: String,
        var isNew: Boolean = false
    )

    override fun parseData(s: String) {
        val res = arrayListOf<Info>()
        var i = 0
        while (true) {
            i = s.indexOf("<div class=\"topic\"", i + 1)
            if (i == -1) break

            val title = fromHTML(s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i)))

            i = s.indexOf("entry/", i)
            val id = s.substring(i+6, s.indexOf('"',i))

            i = s.indexOf("<div class=\"info\"", i)
            i = s.indexOf("/profile/", i)
            val author = s.substring(i+9,s.indexOf('"',i))

            i = s.indexOf("rated-user user-",i)
            val authorColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))

            i = s.indexOf("<span class=\"format-humantime\"", i)
            val time = s.substring(s.indexOf('>',i)+1, s.indexOf("</span>",i))

            i = s.indexOf("<div class=\"roundbox meta\"", i)
            i = s.indexOf("</span>", i)
            val rating = s.substring(s.lastIndexOf('>',i-1)+1,i)

            i = s.indexOf("<div class=\"right-meta\">", i)
            i = s.indexOf("</ul>", i)
            i = s.lastIndexOf("</a>", i)
            val comments = s.substring(s.lastIndexOf('>',i-1)+1,i).trim()

            res.add(Info(id,title,author,authorColor,time,comments,rating))
        }

        if(res.isNotEmpty()){
            rows = res.toTypedArray()
            notifyDataSetChanged()
        }
    }


    class CodeforcesNewsItemViewHolder(val view: RelativeLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
        val commentsIcon: ImageView = view.findViewById(R.id.news_item_comment_icon)
        val newDot: View = view.findViewById(R.id.new_item_dot_new)
    }

    protected var rows: Array<Info> = emptyArray()

    override fun getItemCount() = rows.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as RelativeLayout
        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as CodeforcesNewsItemViewHolder){
            val info = rows[position]

            view.setOnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/${info.blogID}")))
            }

            title.text = info.title

            author.text = activity.accountsFragment.codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

            time.text = timeRUtoEN(info.time)

            newDot.visibility = if(info.isNew) View.VISIBLE else View.GONE

            comments.text = info.comments
            commentsIcon.visibility = if(info.comments.isEmpty()) View.INVISIBLE else View.VISIBLE

            rating.apply{
                text = info.rating
                setTextColor(activity.resources.getColor(
                    if(info.rating.startsWith('+')) R.color.blog_rating_positive else R.color.blog_rating_negative, null)
                )
            }
        }
    }

    override fun getBlogIDs(): List<String> = rows.map { it.blogID }

    fun showNew(newBlogs: HashSet<String>){
        rows.forEachIndexed { index, info ->
            val isNew = info.blogID in newBlogs
            if(rows[index].isNew != isNew){
                rows[index].isNew = isNew
                notifyItemChanged(index)
            }
        }
    }
}


class CodeforcesNewsItemsRecentAdapter(activity: MainActivity): CodeforcesNewsItemsAdapter(activity){

    companion object {
        fun parsePage(s: String): ArrayList<Info> {
            val commentators = mutableMapOf<String,MutableList<String>>()
            val commentatorsColors = mutableMapOf<String,String>()

            var i = 0
            while(true){
                i = s.indexOf("<table class=\"comment-table\">", i+1)
                if(i==-1) break

                i = s.indexOf("class=\"rated-user", i)
                val handleColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i+10))

                i = s.lastIndexOf("/profile/",i)
                val handle = s.substring(s.indexOf('/',i+1)+1, s.indexOf('"',i))

                i = s.indexOf("#comment-", i)
                val commentID = s.substring(s.indexOf('-',i)+1, s.indexOf('"',i))

                val blogID = s.substring(s.lastIndexOf('/',i)+1, i)

                commentators.getOrPut(blogID) { mutableListOf(commentID) }.add(handle)
                commentatorsColors[handle] = handleColor
            }

            val res = arrayListOf<Info>()
            i = s.indexOf("<div class=\"recent-actions\">")
            if(i==-1) return res

            while(true){
                i = s.indexOf("<div style=\"font-size:0.9em;padding:0.5em 0;\">", i+1)
                if(i==-1) break

                i = s.indexOf("/profile/", i)
                val author = s.substring(i+9,s.indexOf('"',i))

                i = s.indexOf("rated-user user-",i)
                val authorColor = s.substring(s.indexOf(' ',i)+1, s.indexOf('"',i))

                i = s.indexOf("entry/", i)
                val id = s.substring(i+6, s.indexOf('"',i))

                val title = fromHTML(s.substring(s.indexOf(">",i)+1, s.indexOf("</a",i)))

                val comments = mutableListOf<Pair<String,String>>()
                var lastCommentId = ""

                commentators[id]?.distinct()?.mapIndexed{ index, handle ->
                    if(index==0) lastCommentId = handle
                    else comments.add(Pair(handle,commentatorsColors[handle]!!))
                }

                res.add(Info(id,title,author,authorColor,lastCommentId,comments.toTypedArray()))
            }

            return res
        }
    }

    data class Info(
        val blogID: String,
        val title: String,
        val author: String,
        val authorColorTag: String,
        val lastCommentId: String,
        val comments: Array<Pair<String,String>>
    )

    override fun parseData(s: String) {
        val res = parsePage(s)
        if(res.isNotEmpty()){
            rows = res.toTypedArray()
            notifyDataSetChanged()
        }
    }

    class CodeforcesNewsItemViewHolder(val view: RelativeLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
    }

    private var rows: Array<Info> = emptyArray()

    override fun getItemCount() = rows.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_item, parent, false) as RelativeLayout
        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as CodeforcesNewsItemViewHolder){
            val info = rows[position]

            view.setOnClickListener {
                var suf = info.blogID
                if(info.lastCommentId.isNotBlank()) suf+="#comment-${info.lastCommentId}"
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/$suf")))
            }

            title.text =  info.title

            val codeforcesAccountManager = activity.accountsFragment.codeforcesAccountManager

            author.text = codeforcesAccountManager.makeSpan(info.author, info.authorColorTag)

            comments.text = SpannableStringBuilder().apply {
                var flag = false
                info.comments.forEach {(handle, colorTag) ->
                    if(flag) append(", ")
                    append(codeforcesAccountManager.makeSpan(handle,colorTag))
                    flag = true
                }
            }
        }
    }

    override fun getBlogIDs(): List<String> = rows.map { it.blogID }

}

class CodeforcesNewsItemsLostRecentAdapter(activity: MainActivity) : CodeforcesNewsItemsClassicAdapter(activity) {
    override fun parseData(s: String) {
        val blogs = CodeforcesNewsLostRecentJobService.getBlogs(activity, CodeforcesNewsLostRecentJobService.CF_LOST)

        if(blogs.isNotEmpty()){
            val currentTime = System.currentTimeMillis()
            rows = blogs
                .sortedByDescending { it.creationTime }
                .map {
                Info(
                    blogID = it.id.toString(),
                    title = it.title,
                    author = it.author,
                    authorColorTag = it.authorColorTag,
                    time = timeDifference(it.creationTime, currentTime),
                    comments = "",
                    rating = ""
                )
            }.toTypedArray()

            notifyDataSetChanged()
        }
    }

    fun timeDifference(fromTime: Long, toTime: Long): String {
        val t = toTime - fromTime
        return when {
            t <= TimeUnit.MINUTES.toMillis(2) -> "${TimeUnit.MILLISECONDS.toSeconds(t)} seconds"
            t <= TimeUnit.HOURS.toMillis(2) -> "${TimeUnit.MILLISECONDS.toMinutes(t)} minutes"
            t <= TimeUnit.HOURS.toMillis(24 * 2) -> "${TimeUnit.MILLISECONDS.toHours(t)} hours"
            else -> "${TimeUnit.MILLISECONDS.toDays(t)} days"
        } + " ago"
    }
}

fun timeRUtoEN(time: String): String{
    val s = time.split(' ')
    if(s.size == 3 && s.last()=="назад" && s.first().toIntOrNull()!=null){
        val w = s[1]
        return s.first() + " " + when {
            w.startsWith("сек") -> "seconds"
            w.startsWith("мин") -> "minutes"
            w.startsWith("час") -> "hours"
            w.startsWith("д") -> "days"
            w.startsWith("нед") -> "weeks"
            w.startsWith("мес") -> "months"
            w=="лет" || w=="год" -> "years"
            else -> w
        } + " ago"
    }
    return time
}