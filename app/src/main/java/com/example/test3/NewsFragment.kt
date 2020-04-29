package com.example.test3

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.test3.account_manager.CodeforcesAccountManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*


class NewsFragment() : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    private lateinit var codeforcesNewsAdapter: CodeforcesNewsAdapter
    private lateinit var codeforcesNewsViewPager: ViewPager2


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        codeforcesNewsAdapter = CodeforcesNewsAdapter(this)
        codeforcesNewsViewPager = view.findViewById(R.id.cf_news_pager)
        codeforcesNewsViewPager.adapter = codeforcesNewsAdapter
        codeforcesNewsViewPager.offscreenPageLimit = codeforcesNewsAdapter.fragments.size - 1


        val tabLayout: TabLayout = view.findViewById(R.id.cf_news_tab_layout)
        TabLayoutMediator(tabLayout, codeforcesNewsViewPager) { tab, position ->
            tab.text = codeforcesNewsAdapter.fragments[position].title
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.run{
                    val fragment = codeforcesNewsAdapter.fragments[position]
                    if(fragment.forSave && text.toString().endsWith(')')){
                        text = fragment.title
                        fragment.save()
                    }
                }
            }

        })


        view.findViewById<Button>(R.id.button_reload_cf_news).apply {
            setOnClickListener { button -> button as Button
                button.text = "..."
                button.isEnabled = false
                activity.scope.launch {
                    codeforcesNewsAdapter.fragments.mapIndexed { index, fragment ->
                        val tab = tabLayout.getTabAt(index)!!
                        launch {
                            tab.text = "..."
                            val newBlogs = fragment.reload()
                            if(newBlogs>0){
                                tab.text = fragment.title + " ($newBlogs)"
                                if(tab.isSelected) fragment.save()
                            }else{
                                tab.text = fragment.title
                            }
                        }
                    }.joinAll()
                    button.isEnabled = true
                    button.text = "RELOAD"
                }
            }
            callOnClick()
        }

    }

}



class CodeforcesNewsAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return fragments.size
    }

    val fragments = arrayOf(
        CodeforcesNewsFragment("https://codeforces.com/top?locale=ru", "CF TOP", false),
        CodeforcesNewsRecentFragment("https://codeforces.com/?locale=ru", "CF RECENT"),
        CodeforcesNewsFragment("https://codeforces.com/?locale=ru", "CF MAIN", true)
    )

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

}



open class CodeforcesNewsFragment(val address: String, val title: String, val forSave: Boolean) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_page, container, false)
    }

    private lateinit var recyclerView: RecyclerView
    protected lateinit var viewAdapter: CodeforcesNewsItemsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewManager = LinearLayoutManager(context)

        viewAdapter = CodeforcesNewsItemsAdapter(requireActivity() as MainActivity, arrayOf())

        recyclerView = view.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    fun fromHTML(s: String): String {
        return Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    open suspend fun reload(): Int {
        readURLData(address)?.let { s ->
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
            if(res.isEmpty()) return 0

            viewAdapter.setNewData(res.toTypedArray())

            if(!forSave) return 0

            requireActivity().getSharedPreferences("CodeforcesNewsFragmentData $title", Context.MODE_PRIVATE)?.run{
                val savedBlogs = getStringSet("blogs", null) ?: emptySet()
                val currentBlogs = res.map{ it[0] }.filter{ !savedBlogs.contains(it) }
                return currentBlogs.size
            }
        }

        return 0
    }

    fun save() = with(requireActivity().getSharedPreferences("CodeforcesNewsFragmentData $title", Context.MODE_PRIVATE).edit()){
        val toSave = viewAdapter.data.map { it[0] }.toSet()
        putStringSet("blogs", toSave)
        commit()
    }
}

class CodeforcesNewsRecentFragment(address: String, title: String): CodeforcesNewsFragment(address, title, false){

    override suspend fun reload(): Int {
        readURLData(address)?.let { s ->
            val res = arrayListOf<Array<String>>()
            var i = s.indexOf("<div class=\"recent-actions\">")
            if(i==-1) return 0
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

                val time = ""
                val comments = ""
                val rating = ""

                res.add(arrayOf(id,title,author,authorColor,time,comments,rating))
            }
            if(res.isEmpty()) return 0

            viewAdapter.setNewData(res.toTypedArray())
        }
        return 0
    }
}


class CodeforcesNewsItemsAdapter(private val activity: MainActivity, var data: Array<Array<String>>): RecyclerView.Adapter<CodeforcesNewsItemsAdapter.CodeforcesNewsItemViewHolder>(){
    class CodeforcesNewsItemViewHolder(val view: RelativeLayout) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        val time: TextView = view.findViewById(R.id.news_item_time)
        val rating: TextView = view.findViewById(R.id.news_item_rating)
        val comments: TextView = view.findViewById(R.id.news_item_comments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesNewsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as RelativeLayout

        return CodeforcesNewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeforcesNewsItemViewHolder, position: Int) {
        val info = data[position]
        holder.view.setOnClickListener { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/${info[0]}"))) }
        holder.title.text = info[1]

        holder.author.apply {
            text = info[2]
            val tag = info[3]
            setTextColor(CodeforcesAccountManager.HandleColor.getColorByTag(tag)?.argb ?: activity.defaultTextColor)
            typeface = if(tag=="user-black") Typeface.DEFAULT else Typeface.DEFAULT_BOLD
        }


        holder.time.text = info[4]
        holder.comments.text = info[5]

        holder.rating.apply{
            text = info[6]
            setTextColor(activity.resources.getColor(
                if(info[6].startsWith('+')) R.color.blog_rating_positive else R.color.blog_rating_negative,
                null)
            )
        }
    }

    override fun getItemCount() = data.size

    fun setNewData(a: Array<Array<String>>){
        data = a
        notifyDataSetChanged()
    }
}
