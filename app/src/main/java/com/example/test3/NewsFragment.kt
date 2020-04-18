package com.example.test3

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.URI


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






        view.findViewById<Button>(R.id.button_reload_cf).apply {
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
    private lateinit var viewAdapter: MyAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewManager = LinearLayoutManager(context)

        viewAdapter = MyAdapter(requireActivity(), arrayListOf())

        recyclerView = view.findViewById<RecyclerView>(R.id.cf_news_page_recyclerview).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    suspend fun reload(){
        readURLData(address)?.let { s ->
            val res = arrayListOf<Pair<String,Int>>()
            var i = 0
            while (true) {
                i = s.indexOf("<div class=\"topic\"", i + 1)
                if (i == -1) break
                val title = s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i))
                i = s.indexOf("entry/", i)
                val id = s.substring(i+6, s.indexOf('"',i)).toInt()
                res.add(Pair(title,id))
            }
            viewAdapter.data = res
            viewAdapter.notifyDataSetChanged()
        }
    }
}


class MyAdapter(val c: FragmentActivity, var data: ArrayList<Pair<String,Int>>): RecyclerView.Adapter<MyAdapter.MyViewHolder>(){
    class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val textView = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as TextView

        return MyViewHolder(textView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = data[position].first
        holder.textView.setOnClickListener {
            println(data[position].second)
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeforces.com/blog/entry/${data[position].second}")))
        }
    }

    override fun getItemCount() = data.size

}
