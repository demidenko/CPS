package com.example.test3.news.codeforces

import androidx.lifecycle.lifecycleScope
import com.example.test3.CodeforcesTitle

class CodeforcesNewsTopFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.TOP
    override val isManagesNewEntries = false
    override val isAutoUpdatable = false
    override val viewAdapter by lazy {
        CodeforcesBlogEntriesAdapter(
            lifecycleScope,
            newsFragment.newsViewModel.getBlogEntriesTop(),
            newsFragment.viewedDataStore.blogsViewedFlow(title),
            isManagesNewEntries = isManagesNewEntries,
            clearNewEntriesOnDataChange = true
        )
    }


}