package com.example.test3

import android.content.Intent
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
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch


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

        val tabTitles = arrayOf("CF TOP", "CF MAIN")

        val tabLayout: TabLayout = view.findViewById(R.id.cf_news_tab_layout)
        TabLayoutMediator(tabLayout, codeforcesNewsViewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        codeforcesNewsViewPager.offscreenPageLimit = 2

        val tabs = arrayListOf(
            codeforcesNewsAdapter.fragment1,
            codeforcesNewsAdapter.fragment2
        )




        view.findViewById<Button>(R.id.button_reload_cf_news).apply {
            setOnClickListener { button -> button as Button
                button.text = "..."
                button.isEnabled = false
                activity.scope.launch {
                    tabs.mapIndexed { index, fragment ->
                        val tab = tabLayout.getTabAt(index)!!
                        launch {
                            tab.text = "..."
                            fragment.reload()
                            tab.text = tabTitles[index]
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
        return 2
    }


    val fragment1 = CodeforcesNewsFragment("https://codeforces.com/top?locale=ru")
    val fragment2 = CodeforcesNewsFragment("https://codeforces.com/?locale=ru")

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> fragment1
            1 -> fragment2
            else -> throw Exception("cf news tabs bad position")
        }
    }

}

class CodeforcesNewsFragment(val address: String) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cf_news_page, container, false)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: CodeforcesNewsItemsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewManager = LinearLayoutManager(context)

        viewAdapter = CodeforcesNewsItemsAdapter(requireActivity(), arrayListOf())

        recyclerView = view.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    suspend fun reload(){
        readURLData(address)?.let { s ->
            val res = arrayListOf<ArrayList<String>>()
            var i = 0
            while (true) {
                i = s.indexOf("<div class=\"topic\"", i + 1)
                if (i == -1) break
                val title = Html.fromHtml(
                    s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i)),
                    HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

                i = s.indexOf("entry/", i)
                val id = s.substring(i+6, s.indexOf('"',i))

                i = s.indexOf("<div class=\"info\"", i)
                i = s.indexOf("</a>", i)
                val author = s.substring(s.lastIndexOf('>',i-1)+1,i).trim()

                i = s.indexOf("</span>", i)
                val time = s.substring(s.lastIndexOf('>',i-1)+1,i)

                i = s.indexOf("<div class=\"roundbox meta\"", i)
                i = s.indexOf("</span>", i)
                val rating = s.substring(s.lastIndexOf('>',i-1)+1,i)

                i = s.indexOf("<div class=\"right-meta\">", i)
                i = s.indexOf("</ul>", i)
                i = s.lastIndexOf("</a>", i)
                val comments = s.substring(s.lastIndexOf('>',i-1)+1,i).trim()

                res.add(arrayListOf(id,title,author,time,comments,rating))
            }
            viewAdapter.data = res
            viewAdapter.notifyDataSetChanged()
        }
    }
}


class CodeforcesNewsItemsAdapter(private val activity: FragmentActivity, var data: ArrayList<ArrayList<String>>): RecyclerView.Adapter<CodeforcesNewsItemsAdapter.CodeforcesNewsItemViewHolder>(){
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


        holder.author.text = info[2]
        holder.time.text = info[3]
        holder.comments.text = info[4]

        holder.rating.text = info[5]
        holder.rating.setTextColor(activity.resources.getColor(
            if(info[5].startsWith('+')) R.color.blog_rating_positive else R.color.blog_rating_negative,
            null))
    }

    override fun getItemCount() = data.size

}
