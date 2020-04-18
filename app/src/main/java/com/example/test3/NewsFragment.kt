package com.example.test3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.lang.Exception


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



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val textView: TextView = view.findViewById(R.id.news_cf_textview)
        textView.setLineSpacing(0f, 1.3f)
    }

    suspend fun reload(){
        val t = view!!.findViewById<TextView>(R.id.news_cf_textview)
        readURLData(address)?.let { s ->
            var res: String = ""
            var i = 0
            while (true) {
                i = s.indexOf("<div class=\"topic\"", i + 1)
                if (i == -1) break
                val title = s.substring(s.indexOf("<p>", i) + 3, s.indexOf("</p>", i))
                res += title + "\n"
            }
            t.text = res
        }
    }
}
