package com.example.test3

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch


class NewsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    private lateinit var codeforcesNewsAdapter: CodeforcesNewsAdapter
    private lateinit var tabLayout: TabLayout
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

        codeforcesNewsAdapter = CodeforcesNewsAdapter(this)

        val codeforcesNewsViewPager: ViewPager2 = view.findViewById(R.id.cf_news_pager)
        codeforcesNewsViewPager.adapter = codeforcesNewsAdapter
        codeforcesNewsViewPager.offscreenPageLimit = codeforcesNewsAdapter.fragments.size - 1


        tabLayout = view.findViewById(R.id.cf_news_tab_layout)
        TabLayoutMediator(tabLayout, codeforcesNewsViewPager) { tab, position ->
            val fragment = codeforcesNewsAdapter.fragments[position]
            val title = fragment.title
            tab.text = title
            if(title == "CF MAIN"){
                tab.orCreateBadge.apply {
                    backgroundColor = resources.getColor(android.R.color.holo_green_light, null)
                    isVisible = false
                }
            }
        }.attach()



        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.run{
                    val fragment = codeforcesNewsAdapter.fragments[position]
                    if(fragment is CodeforcesNewsMainFragment) badge?.run{
                        if(hasNumber()){
                            fragment.save()
                            isVisible = false
                            clearNumber()
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
                    val rx = codeforcesNewsAdapter.fragments.map { it.address }.toSet().map { it to async { readURLData(it) } }.toMap()
                    codeforcesNewsAdapter.fragments.mapIndexed { index, fragment ->
                        val tab = tabLayout.getTabAt(index)!!
                        launch {
                            tab.text = "..."
                            fragment.reload(rx)
                            tab.text = fragment.title
                            if(fragment is CodeforcesNewsMainFragment && fragment.newBlogs>0){
                                if(tab.isSelected) fragment.save()
                                else{
                                    tab.badge?.apply{
                                        number = fragment.newBlogs
                                        isVisible = true
                                    }
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
        buttonReload.callOnClick()
    }

}



class CodeforcesNewsAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    val fragments = arrayOf(
        CodeforcesNewsRecentFragment("https://codeforces.com/recent-actions?locale=ru", "CF RECENT"),
        CodeforcesNewsFragment("https://codeforces.com/top?locale=ru", "CF TOP"),
        CodeforcesNewsMainFragment("https://codeforces.com/?locale=ru", "CF MAIN")
    )

    override fun createFragment(position: Int): Fragment = fragments[position]
    override fun getItemCount(): Int = fragments.size
}



open class CodeforcesNewsFragment(val address: String, val title: String) : Fragment() {

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

    open suspend fun reload(data: Map<String,Deferred<String?>>) {
        val s = data[address]?.await() ?: return
        viewAdapter.parseData(s)
    }

    fun refresh(){
        viewAdapter.notifyDataSetChanged()
    }

}

class CodeforcesNewsMainFragment(address: String, title: String): CodeforcesNewsFragment(address, title){

    companion object {
        const val CODEFORCES_NEWS_MAIN = "codeforces_news_main"
        const val blogs = "blogs"
    }

    private lateinit var prefs: SharedPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs = requireActivity().getSharedPreferences(CODEFORCES_NEWS_MAIN, Context.MODE_PRIVATE)
    }

    var newBlogs: Int = 0

    override suspend fun reload(data: Map<String, Deferred<String?>>) {
        super.reload(data)

        val savedBlogs = prefs.getStringSet(blogs, null) ?: emptySet()
        newBlogs = viewAdapter.getBlogIDs().count { !savedBlogs.contains(it) }
    }

    fun save() = with(prefs.edit()){
        val toSave = viewAdapter.getBlogIDs().toSet()
        putStringSet(blogs, toSave)
        commit()
    }
}



class CodeforcesNewsRecentFragment(address: String, title: String): CodeforcesNewsFragment(address, title){

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewAdapter = CodeforcesNewsItemsRecentAdapter(requireActivity() as MainActivity)

        view.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

}


///---------------data adapters--------------------

abstract class CodeforcesNewsItemsAdapter(protected val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    abstract fun parseData(s: String)
    abstract fun getBlogIDs(): List<String>
}

class CodeforcesNewsItemsClassicAdapter(activity: MainActivity): CodeforcesNewsItemsAdapter(activity){

    override fun parseData(s: String) {
        val res = arrayListOf<Array<String>>()
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

            res.add(arrayOf(id,title,author,authorColor,time,comments,rating))
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
    }

    private var rows: Array<Array<String>> = emptyArray()

    override fun getItemCount() = rows.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as RelativeLayout
        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as CodeforcesNewsItemViewHolder
        val info = rows[position]
        holder.view.setOnClickListener {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/${info[0]}")))
        }

        holder.title.text = info[1]

        holder.author.apply {
            text = info[2]
            val tag = info[3]
            setTextColor(activity.accountsFragment.codeforcesAccountManager.getHandleColorByTag(tag) ?: activity.defaultTextColor) // kill me
            typeface = if(tag=="user-black") Typeface.DEFAULT else Typeface.DEFAULT_BOLD
        }


        holder.time.text = info[4]
        holder.comments.text = info[5]

        holder.rating.apply{
            text = info[6]
            setTextColor(activity.resources.getColor(
                if(info[6].startsWith('+')) R.color.blog_rating_positive else R.color.blog_rating_negative, null)
            )
        }
    }

    override fun getBlogIDs(): List<String> {
        return rows.map { it[0] }
    }
}


class CodeforcesNewsItemsRecentAdapter(activity: MainActivity): CodeforcesNewsItemsAdapter(activity){

    override fun parseData(s: String) {
        val commentators = mutableMapOf<String,MutableList<String>>()

        var i = 0
        while(true){
            i = s.indexOf("<table class=\"comment-table\">", i+1)
            if(i==-1) break

            i = s.indexOf("class=\"rated-user", i)
            val handle = s.substring(s.indexOf('>',i)+1, s.indexOf('<',i))

            i = s.indexOf("#comment-", i)
            val commentID = s.substring(s.indexOf('-',i)+1, s.indexOf('"',i))

            val blogID = s.substring(s.lastIndexOf('/',i)+1, i)

            commentators.getOrPut(blogID) { mutableListOf(commentID) }.add(handle)
        }

        val res = arrayListOf<Array<String>>()
        i = s.indexOf("<div class=\"recent-actions\">")
        if(i==-1) return
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

            val (comments, lastCommentId) = commentators.getOrDefault(id, mutableListOf()).let{
                var cid = ""
                if(it.isNotEmpty()){
                    cid = it[0]
                    it.removeAt(0)
                }
                Pair(it.distinct().joinToString(), cid)
            }

            res.add(arrayOf(id,title,author,authorColor,comments,lastCommentId))
        }

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

    private var rows: Array<Array<String>> = emptyArray()

    override fun getItemCount() = rows.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_recent_item, parent, false) as RelativeLayout
        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as CodeforcesNewsItemViewHolder
        val info = rows[position]
        holder.view.setOnClickListener {
            var suf = info[0]
            if(info[5].isNotBlank()) suf+="#comment-${info[5]}"
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/$suf")))
        }

        holder.title.text = info[1]

        holder.author.apply {
            text = info[2]
            val tag = info[3]
            setTextColor(activity.accountsFragment.codeforcesAccountManager.getHandleColorByTag(tag) ?: activity.defaultTextColor) // kill me
            typeface = if(tag=="user-black") Typeface.DEFAULT else Typeface.DEFAULT_BOLD
        }

        holder.comments.text = info[4]
    }

    override fun getBlogIDs(): List<String> {
        return rows.map { it[0] }
    }
}