package com.example.test3.news.codeforces

import androidx.lifecycle.lifecycleScope
import com.example.test3.CodeforcesTitle
import com.google.android.material.tabs.TabLayout

class CodeforcesNewsMainFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.MAIN
    override val isManagesNewEntries = true
    override val isAutoUpdatable = false
    override val viewAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            lifecycleScope,
            newsFragment.newsViewModel.getBlogEntriesMain(),
            newsFragment.viewedDataStore.blogsViewedFlow(title),
            isManagesNewEntries = isManagesNewEntries,
            clearNewEntriesOnDataChange = true
        )
    }

    override fun onPageSelected(tab: TabLayout.Tab) {
        tab.badge?.run {
            if(hasNumber()){
                isVisible = true
                saveEntries()
            }
        }
    }

    override fun onPageUnselected(tab: TabLayout.Tab) {
        tab.badge?.run {
            if(hasNumber()) isVisible = false
        }
    }

}